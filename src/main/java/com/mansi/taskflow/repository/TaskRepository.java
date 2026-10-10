package com.mansi.taskflow.repository;

import com.mansi.taskflow.model.Task;
import com.mansi.taskflow.model.TaskPriority;
import com.mansi.taskflow.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    long countByStatus(TaskStatus status);
    long countByOwnerUsernameAndStatus(String ownerUsername, TaskStatus status);
    List<Task> findByOwnerUsernameOrderByStatusAscSortOrderAscUpdatedAtDesc(String ownerUsername);
    List<Task> findByOwnerUsernameAndStatusOrderBySortOrderAscUpdatedAtDesc(String ownerUsername, TaskStatus status);
    java.util.Optional<Task> findByIdAndOwnerUsername(Long id, String ownerUsername);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Task t set t.ownerUsername = :owner where t.ownerUsername is null")
    int assignLegacyTasks(@org.springframework.data.repository.query.Param("owner") String owner);
    List<Task> findByStatusOrderBySortOrderAscUpdatedAtDesc(TaskStatus status);
    @Query("""
        select t from Task t
        where (:status is null or t.status = :status)
          and (:priority is null or t.priority = :priority)
          and (:query is null or lower(t.title) like lower(concat('%', :query, '%'))
               or lower(t.description) like lower(concat('%', :query, '%'))
               or lower(t.assignee) like lower(concat('%', :query, '%')))
        order by t.status asc, t.sortOrder asc, t.updatedAt desc
        """)
    List<Task> search(@Param("query") String query,
                      @Param("status") TaskStatus status,
                      @Param("priority") TaskPriority priority);
}
