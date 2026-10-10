package com.mansi.taskflow.repository;
import com.mansi.taskflow.model.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> { List<ChecklistItem> findByTaskIdOrderByIdAsc(Long taskId); }
