package com.mansi.taskflow.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "checklist_items")
public class ChecklistItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long taskId;
    @Column(nullable = false, length = 180) private String text;
    @Column(nullable = false) private boolean completed = false;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    protected ChecklistItem() {}
    public ChecklistItem(Long taskId, String text) { this.taskId = taskId; this.text = text.trim(); }
    public Long getId(){ return id; } public Long getTaskId(){ return taskId; } public String getText(){ return text; }
    public boolean isCompleted(){ return completed; } public void setCompleted(boolean completed){ this.completed = completed; }
}
