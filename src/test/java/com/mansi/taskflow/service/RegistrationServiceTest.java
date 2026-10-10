package com.mansi.taskflow.service;

import com.mansi.taskflow.model.UserAccount;
import com.mansi.taskflow.repository.TaskRepository;
import com.mansi.taskflow.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {
    @Mock UserAccountRepository users;
    @Mock TaskRepository tasks;
    @Mock PasswordEncoder encoder;
    @InjectMocks RegistrationService service;

    @Test void storesEncodedPasswordAndAssignsLegacyTasksToFirstAccount() {
        when(users.existsByUsernameIgnoreCase("mansi")).thenReturn(false);
        when(users.existsByEmailIgnoreCase("mansi@example.com")).thenReturn(false);
        when(encoder.encode("a-long-password")).thenReturn("bcrypt-hash");
        when(users.count()).thenReturn(1L);
        service.register("mansi", "Mansi@Example.com", "a-long-password");
        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(users).saveAndFlush(captor.capture());
        assertEquals("mansi", captor.getValue().getUsername());
        assertEquals("mansi@example.com", captor.getValue().getEmail());
        assertEquals("bcrypt-hash", captor.getValue().getPasswordHash());
        verify(tasks).assignLegacyTasks("mansi");
    }

    @Test void rejectsShortPasswordsBeforeSaving() {
        assertThrows(IllegalArgumentException.class, () -> service.register("mansi", "mansi@example.com", "short"));
        verify(users, never()).saveAndFlush(any());
    }
}
