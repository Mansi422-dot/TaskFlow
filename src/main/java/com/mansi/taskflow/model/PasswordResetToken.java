package com.mansi.taskflow.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens", indexes = @Index(name = "idx_password_reset_token_hash", columnList = "tokenHash", unique = true))
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 64, unique = true)
    private String tokenHash;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    @Column
    private LocalDateTime usedAt;
    protected PasswordResetToken() {}
    public PasswordResetToken(String tokenHash, String email, LocalDateTime expiresAt) {
        this.tokenHash = tokenHash;
        this.email = email.toLowerCase(java.util.Locale.ROOT);
        this.expiresAt = expiresAt;
    }
    public boolean isUsableAt(LocalDateTime now) {
        return usedAt == null && expiresAt.isAfter(now);
    }
    public void markUsed(LocalDateTime now) { this.usedAt = now; }
    public String getEmail() { return email; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
}
