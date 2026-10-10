package com.mansi.taskflow.service;

import com.mansi.taskflow.model.UserAccount;
import com.mansi.taskflow.repository.TaskRepository;
import com.mansi.taskflow.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.regex.Pattern;

@Service
public class RegistrationService {
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]{3,32}$");
    private final UserAccountRepository users;
    private final TaskRepository tasks;
    private final PasswordEncoder encoder;
    public RegistrationService(UserAccountRepository users, TaskRepository tasks, PasswordEncoder encoder) { this.users = users; this.tasks = tasks; this.encoder = encoder; }

    @Transactional
    public void register(String username, String email, String password) {
        String cleanUsername = username == null ? "" : username.trim();
        String cleanEmail = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
        if (!USERNAME.matcher(cleanUsername).matches()) throw new IllegalArgumentException("Username must be 3–32 characters and may contain letters, numbers, dots, underscores, and hyphens.");
        if (cleanEmail.length() > 254 || !cleanEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new IllegalArgumentException("Enter a valid email address.");
        if (password == null || password.length() < 10 || password.length() > 72) throw new IllegalArgumentException("Password must be between 10 and 72 characters.");
        if (users.existsByUsernameIgnoreCase(cleanUsername)) throw new IllegalArgumentException("That username is already taken.");
        if (users.existsByEmailIgnoreCase(cleanEmail)) throw new IllegalArgumentException("An account with that email already exists.");
        users.saveAndFlush(new UserAccount(cleanUsername, cleanEmail, encoder.encode(password)));
        // One-time, non-destructive migration: attach legacy unowned tasks to the first registered account.
        if (users.count() == 1) tasks.assignLegacyTasks(cleanUsername);
    }
}
