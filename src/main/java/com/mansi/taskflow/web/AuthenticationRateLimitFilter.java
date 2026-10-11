package com.mansi.taskflow.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Lightweight per-process throttling for public authentication endpoints.
 * This is intended for a local/portfolio app, not as a distributed production limiter.
 */
@Component
public class AuthenticationRateLimitFilter extends OncePerRequestFilter {
    private record Rule(int maxRequests, long windowMillis) {}
    private static final class Window {
        private long startedAt;
        private int count;
        private Window(long now) { startedAt = now; count = 1; }
    }

    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final int loginLimit;
    private final int registerLimit;
    private final int forgotLimit;
    private final int resetLimit;
    private final long windowMillis;

    public AuthenticationRateLimitFilter(
            @Value("${taskflow.rate-limit.enabled:true}") boolean enabled,
            @Value("${taskflow.rate-limit.window-seconds:900}") long windowSeconds,
            @Value("${taskflow.rate-limit.login:10}") int loginLimit,
            @Value("${taskflow.rate-limit.register:5}") int registerLimit,
            @Value("${taskflow.rate-limit.forgot-password:3}") int forgotLimit,
            @Value("${taskflow.rate-limit.reset-password:10}") int resetLimit) {
        this.enabled = enabled;
        this.windowMillis = Duration.ofSeconds(Math.max(1, windowSeconds)).toMillis();
        this.loginLimit = Math.max(1, loginLimit);
        this.registerLimit = Math.max(1, registerLimit);
        this.forgotLimit = Math.max(1, forgotLimit);
        this.resetLimit = Math.max(1, resetLimit);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !"POST".equalsIgnoreCase(request.getMethod()) || ruleFor(request.getServletPath()) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        Rule rule = ruleFor(path);
        String client = clientKey(request);
        long now = System.currentTimeMillis();

        // Periodic opportunistic cleanup keeps expired entries from accumulating forever.
        if ((now & 0x3f) == 0) {
            windows.entrySet().removeIf(e -> now - e.getValue().startedAt > windowMillis * 2);
        }

        String key = path + "|" + client;
        Window window = windows.compute(key, (k, current) -> {
            if (current == null || now - current.startedAt >= windowMillis) return new Window(now);
            current.count++;
            return current;
        });

        if (window.count > rule.maxRequests()) {
            long retrySeconds = Math.max(1, (window.startedAt + windowMillis - now + 999) / 1000);
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(retrySeconds));
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Too many requests. Please wait before trying again.");
            return;
        }
        chain.doFilter(request, response);
    }

    private Rule ruleFor(String path) {
        return switch (path) {
            case "/login" -> new Rule(loginLimit, windowMillis);
            case "/register" -> new Rule(registerLimit, windowMillis);
            case "/forgot-password" -> new Rule(forgotLimit, windowMillis);
            case "/reset-password" -> new Rule(resetLimit, windowMillis);
            default -> null;
        };
    }

    private String clientKey(HttpServletRequest request) {
        // Do not trust X-Forwarded-For: the app is intended to run locally and clients can spoof it.
        String address = request.getRemoteAddr();
        return address == null || address.isBlank() ? "unknown" : address;
    }
}
