package com.mansi.taskflow.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_accounts", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_accounts_username", columnNames = "username"),
    @UniqueConstraint(name = "uk_user_accounts_email", columnNames = "email")
})
public class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 32) private String username;
    @Column(nullable = false, length = 254) private String email;
    @Column(nullable = false, length = 100) private String passwordHash;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    protected UserAccount() {}
    public UserAccount(String username, String email, String passwordHash) {
        this.username = username.trim(); this.email = email.trim().toLowerCase(java.util.Locale.ROOT); this.passwordHash = passwordHash;
    }
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
