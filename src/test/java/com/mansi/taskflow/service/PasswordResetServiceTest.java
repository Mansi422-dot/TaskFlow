package com.mansi.taskflow.service;

import com.mansi.taskflow.model.UserAccount;
import com.mansi.taskflow.repository.PasswordResetTokenRepository;
import com.mansi.taskflow.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {
    private UserAccountRepository users;
    private PasswordResetTokenRepository tokens;
    private PasswordEncoder encoder;
    private ObjectProvider<JavaMailSender> senders;
    private PasswordResetService service;

    @SuppressWarnings("unchecked")
    @BeforeEach void setUp() {
        users = mock(UserAccountRepository.class);
        tokens = mock(PasswordResetTokenRepository.class);
        encoder = mock(PasswordEncoder.class);
        senders = mock(ObjectProvider.class);
        service = new PasswordResetService(users, tokens, encoder, senders, "http://localhost:8080", "");
    }

    @Test void unknownEmailDoesNotCreateResetToken() {
        when(users.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());
        service.requestReset("nobody@example.com");
        verify(tokens, never()).save(any());
        verify(senders, never()).getIfAvailable();
    }

    @Test void rejectsInvalidOrTooShortResetPasswords() {
        assertFalse(service.resetPassword("not-a-real-token", "short"));
        verify(tokens, never()).findByTokenHash(anyString());
        verify(users, never()).save(any(UserAccount.class));
    }
}
