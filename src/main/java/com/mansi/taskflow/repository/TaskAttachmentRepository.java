package com.mansi.taskflow.repository;
import com.mansi.taskflow.model.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> { List<TaskAttachment> findByTaskIdOrderByUploadedAtDesc(Long taskId); }
