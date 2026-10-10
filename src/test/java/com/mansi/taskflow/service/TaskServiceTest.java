package com.mansi.taskflow.service;

import com.mansi.taskflow.model.Task;
import com.mansi.taskflow.model.TaskPriority;
import com.mansi.taskflow.model.TaskStatus;
import com.mansi.taskflow.model.ChecklistItem;
import java.util.List;
import com.mansi.taskflow.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock TaskRepository repository;
    @InjectMocks TaskService service;

    @Test void createsTaskWithProvidedFields() {
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        Task result = service.create("Demo", "Description", "Mansi", TaskPriority.HIGH, TaskStatus.TODO);
        assertEquals("Demo", result.getTitle());
        assertEquals(TaskPriority.HIGH, result.getPriority());
        verify(repository).save(any(Task.class));
    }


    @Test void createsTaskWithDueDate() {
        LocalDate dueDate = LocalDate.of(2026, 10, 20);
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        Task result = service.create("Due date demo", "", "Mansi", TaskPriority.MEDIUM, TaskStatus.TODO, dueDate);
        assertEquals(dueDate, result.getDueDate());
    }


    @Test void calculatesChecklistProgressForCard() {
        Task task = new Task("Checklist demo", "", "Mansi", TaskPriority.MEDIUM, TaskStatus.TODO);
        ChecklistItem one = new ChecklistItem(1L, "First");
        ChecklistItem two = new ChecklistItem(1L, "Second");
        ChecklistItem three = new ChecklistItem(1L, "Third");
        ChecklistItem four = new ChecklistItem(1L, "Fourth");
        ChecklistItem five = new ChecklistItem(1L, "Fifth");
        one.setCompleted(true);
        two.setCompleted(true);
        three.setCompleted(true);
        task.setChecklistItems(List.of(one, two, three, four, five));
        assertEquals(5, task.getChecklistTotal());
        assertEquals(3, task.getChecklistCompleted());
        assertEquals(60, task.getChecklistPercent());
    }


    @Test void createsTaskInCustomSection() {
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        Task result = service.create("Study", "Prepare notes", "Mansi", TaskPriority.MEDIUM,
                TaskStatus.TODO, null, "College");
        assertEquals("College", result.getSection());
    }

    @Test void emptyChecklistHasZeroPercentProgress() {
        Task task = new Task("No checklist", "", "Mansi", TaskPriority.LOW, TaskStatus.TODO);
        task.setChecklistItems(java.util.List.of());
        assertEquals(0, task.getChecklistPercent());
    }

    @Test void throwsWhenTaskDoesNotExist() {
        when(repository.findById(42L)).thenReturn(Optional.empty());
        assertThrows(TaskNotFoundException.class, () -> service.get(42L));
    }

    @Test void rejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(" ", "", "Mansi", TaskPriority.MEDIUM, TaskStatus.TODO));
    }
}
