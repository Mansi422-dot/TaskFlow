package com.mansi.taskflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nullable only to allow safe schema upgrades; the first registered account claims legacy tasks.
    @Column(name = "owner_username", length = 32)
    private String ownerUsername;

    @NotBlank @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String title;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description = "";

    @NotBlank @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String assignee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    // Nullable DB column allows Hibernate to upgrade an existing local database safely.
    @Column(name = "section_name", length = 80)
    private String section = "General";

    @Transient
    private List<ChecklistItem> checklistItems = List.of();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Task() {}

    public Task(String title, String description, String assignee, TaskPriority priority, TaskStatus status) {
        this(title, description, assignee, priority, status, null);
    }

    public Task(String title, String description, String assignee, TaskPriority priority, TaskStatus status, LocalDate dueDate) {
        this.title = clean(title);
        this.description = description == null ? "" : description.trim();
        this.assignee = clean(assignee);
        this.priority = priority == null ? TaskPriority.MEDIUM : priority;
        this.status = status == null ? TaskStatus.TODO : status;
        this.dueDate = dueDate;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void update(String title, String description, String assignee, TaskPriority priority, TaskStatus status) {
        update(title, description, assignee, priority, status, this.dueDate);
    }

    public void update(String title, String description, String assignee, TaskPriority priority, TaskStatus status, LocalDate dueDate) {
        update(title, description, assignee, priority, status, dueDate, getSection());
    }

    public void update(String title, String description, String assignee, TaskPriority priority, TaskStatus status, LocalDate dueDate, String section) {
        this.title = clean(title);
        this.description = description == null ? "" : description.trim();
        this.assignee = clean(assignee);
        this.priority = priority == null ? TaskPriority.MEDIUM : priority;
        this.status = status == null ? TaskStatus.TODO : status;
        this.dueDate = dueDate;
        this.section = section == null || section.isBlank() ? "General" : section.trim();
    }

    private static String clean(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Required text cannot be blank.");
        return value.trim();
    }

    public Long getId() { return id; }
    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAssignee() { return assignee; }
    public TaskPriority getPriority() { return priority; }
    public TaskStatus getStatus() { return status; }
    public LocalDate getDueDate() { return dueDate; }
    public String getSection() { return section == null || section.isBlank() ? "General" : section; }
    public void setSection(String section) { this.section = section == null || section.isBlank() ? "General" : section.trim(); }
    public List<ChecklistItem> getChecklistItems() { return checklistItems == null ? List.of() : checklistItems; }
    public void setChecklistItems(List<ChecklistItem> checklistItems) { this.checklistItems = checklistItems == null ? List.of() : checklistItems; }
    public int getChecklistTotal() { return getChecklistItems().size(); }
    public long getChecklistCompleted() { return getChecklistItems().stream().filter(ChecklistItem::isCompleted).count(); }
    public int getChecklistPercent() { return getChecklistTotal() == 0 ? 0 : (int) Math.round(getChecklistCompleted() * 100.0 / getChecklistTotal()); }
    public Integer getSortOrder() { return sortOrder == null ? 0 : sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder == null ? 0 : sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
