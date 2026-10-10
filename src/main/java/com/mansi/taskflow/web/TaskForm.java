package com.mansi.taskflow.web;

import com.mansi.taskflow.model.TaskPriority;
import com.mansi.taskflow.model.TaskStatus;
import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TaskForm {
    @NotBlank(message = "Title is required.")
    @Size(max = 120, message = "Title must be 120 characters or fewer.")
    private String title;

    @Size(max = 2000, message = "Description must be 2000 characters or fewer.")
    private String description = "";

    @NotBlank(message = "Assignee is required.")
    @Size(max = 80, message = "Assignee must be 80 characters or fewer.")
    private String assignee;

    private TaskPriority priority = TaskPriority.MEDIUM;
    private TaskStatus status = TaskStatus.TODO;
    private LocalDate dueDate;
    @Size(max = 80, message = "Section must be 80 characters or fewer.")
    private String section = "General";

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}
