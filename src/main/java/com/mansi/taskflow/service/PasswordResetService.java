package com.mansi.taskflow.service;

import com.mansi.taskflow.model.PasswordResetToken;
import com.mansi.taskflow.model.UserAccount;
import com.mansi.taskflow.repository.PasswordResetTokenRepository;
import com.mansi.taskflow.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class PasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final int TOKEN_MINUTES = 30;
    private static final long REQUEST_COOLDOWN_SECONDS = 60;
    private final ConcurrentMap<String, Long> recentRequests = new ConcurrentHashMap<>();

    private final UserAccountRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final String baseUrl;
    private final String fromAddress;

    public PasswordResetService(UserAccountRepository users, PasswordResetTokenRepository tokens,
            PasswordEncoder encoder, ObjectProvider<JavaMailSender> mailSenders,
            @Value("${taskflow.mail.base-url:http://localhost:8080}") String baseUrl,
            @Value("${taskflow.mail.from:}") String fromAddress) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.mailSenders = mailSenders;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.fromAddress = fromAddress;
    }

    @Transactional
    public void requestReset(String suppliedEmail) {
        if (suppliedEmail == null || suppliedEmail.isBlank() || suppliedEmail.length() > 254) return;
        String email = suppliedEmail.trim().toLowerCase(java.util.Locale.ROOT);
        long nowMillis = System.currentTimeMillis();
        // Lightweight local-app throttle: at most one request per email each minute.
        recentRequests.entrySet().removeIf(entry -> nowMillis - entry.getValue() > 10 * 60 * 1000L);
        Long previous = recentRequests.putIfAbsent(email, nowMillis);
        if (previous != null) {
            if (nowMillis - previous < REQUEST_COOLDOWN_SECONDS * 1000L) return;
            recentRequests.replace(email, previous, nowMillis);
        }
        UserAccount account = users.findByEmailIgnoreCase(email).orElse(null);
        if (account == null) return;

        JavaMailSender mailSender = mailSenders.getIfAvailable();
        if (mailSender == null || fromAddress.isBlank()) {
            // Deliberately do not create a token that cannot be delivered or expose it in logs/UI.
            log.warn("Password reset email is not configured; set TASKFLOW_MAIL_USERNAME, TASKFLOW_MAIL_PASSWORD and TASKFLOW_MAIL_FROM.");
            return;
        }

        byte[] random = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(random);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String tokenHash = sha256(rawToken);
        tokens.deleteAllByEmailIgnoreCase(email);
        tokens.save(new PasswordResetToken(tokenHash, email, LocalDateTime.now().plusMinutes(TOKEN_MINUTES)));

        String link = baseUrl + "/reset-password?token=" + rawToken;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(account.getEmail());
        message.setSubject("Reset your TaskFlow password");
        message.setText("A password reset was requested for your TaskFlow account.\n\n"
                + "Use this one-time link within " + TOKEN_MINUTES + " minutes:\n" + link
                + "\n\nIf you did not request this, you can ignore this email.");
        mailSender.send(message);
    }

    @Transactional
    public boolean resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank() || newPassword == null
                || newPassword.length() < 10 || newPassword.length() > 72) return false;
        PasswordResetToken token = tokens.findByTokenHash(sha256(rawToken)).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (token == null || !token.isUsableAt(now)) return false;
        UserAccount account = users.findByEmailIgnoreCase(token.getEmail()).orElse(null);
        if (account == null) return false;
        account.setPasswordHash(encoder.encode(newPassword));
        users.save(account);
        token.markUsed(now);
        tokens.save(token);
        // Remove any other outstanding reset tokens for this account.
        tokens.deleteAllByEmailIgnoreCase(token.getEmail());
        return true;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
