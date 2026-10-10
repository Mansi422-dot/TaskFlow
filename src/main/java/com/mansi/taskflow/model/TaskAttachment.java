package com.mansi.taskflow.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_attachments")
public class TaskAttachment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long taskId;
    @Column(nullable = false, length = 255) private String originalName;
    @Column(nullable = false, length = 500) private String storedName;
    @Column(nullable = false, length = 120) private String contentType;
    @Column(nullable = false) private long sizeBytes;
    @Column(nullable = false) private LocalDateTime uploadedAt = LocalDateTime.now();
    protected TaskAttachment() {}
    public TaskAttachment(Long taskId, String originalName, String storedName, String contentType, long sizeBytes) { this.taskId=taskId; this.originalName=originalName; this.storedName=storedName; this.contentType=contentType; this.sizeBytes=sizeBytes; }
    public Long getId(){return id;} public Long getTaskId(){return taskId;} public String getOriginalName(){return originalName;} public String getStoredName(){return storedName;} public String getContentType(){return contentType;} public long getSizeBytes(){return sizeBytes;} public LocalDateTime getUploadedAt(){return uploadedAt;}
}
