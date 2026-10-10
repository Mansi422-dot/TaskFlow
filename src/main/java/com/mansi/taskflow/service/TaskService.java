package com.mansi.taskflow.service;

import com.mansi.taskflow.model.Task;
import com.mansi.taskflow.model.TaskPriority;
import com.mansi.taskflow.model.TaskStatus;
import com.mansi.taskflow.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.time.LocalDate;
import com.mansi.taskflow.model.ChecklistItem;
import com.mansi.taskflow.model.TaskAttachment;
import com.mansi.taskflow.repository.ChecklistItemRepository;
import com.mansi.taskflow.repository.TaskAttachmentRepository;
import java.util.Comparator;

@Service
@Transactional
public class TaskService {
    private final TaskRepository repository;
    private final ChecklistItemRepository checklistRepository;
    private final TaskAttachmentRepository attachmentRepository;

    public TaskService(TaskRepository repository, ChecklistItemRepository checklistRepository, TaskAttachmentRepository attachmentRepository) { this.repository = repository; this.checklistRepository = checklistRepository; this.attachmentRepository = attachmentRepository; }

    @Transactional(readOnly = true)
    public List<Task> findTasks(String query, TaskStatus status, TaskPriority priority) {
        String normalized = query == null || query.isBlank() ? null : query.trim().toLowerCase(java.util.Locale.ROOT);
        List<Task> results = repository.search(null, status, priority);
        if (normalized == null) return results;
        // Keep free-text search flexible for user-defined sections and enum labels too.
        return results.stream().filter(task -> contains(task.getTitle(), normalized)
                || contains(task.getDescription(), normalized)
                || contains(task.getAssignee(), normalized)
                || contains(task.getSection(), normalized)
                || task.getPriority().name().toLowerCase(java.util.Locale.ROOT).contains(normalized)
                || task.getStatus().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ').contains(normalized))
                .toList();
    }


    // Owner-scoped entry points used by every web and REST controller.
    @Transactional(readOnly = true)
    public List<Task> findTasksForUser(String owner, String query, TaskStatus status, TaskPriority priority) {
        String normalized = query == null || query.isBlank() ? null : query.trim().toLowerCase(java.util.Locale.ROOT);
        List<Task> results = repository.findByOwnerUsernameOrderByStatusAscSortOrderAscUpdatedAtDesc(owner).stream()
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> priority == null || t.getPriority() == priority).toList();
        if (normalized == null) return results;
        return results.stream().filter(task -> contains(task.getTitle(), normalized) || contains(task.getDescription(), normalized)
                || contains(task.getAssignee(), normalized) || contains(task.getSection(), normalized)
                || task.getPriority().name().toLowerCase(java.util.Locale.ROOT).contains(normalized)
                || task.getStatus().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ').contains(normalized)).toList();
    }

    private Task getForUser(Long id, String owner) {
        return repository.findByIdAndOwnerUsername(id, owner).orElseThrow(() -> new TaskNotFoundException(id));
    }
    public Task getForUserPublic(Long id, String owner) { return getForUser(id, owner); }
    public long countByStatusForUser(String owner, TaskStatus status) { return repository.countByOwnerUsernameAndStatus(owner, status); }

    public Task createForUser(String owner, String title, String description, String assignee, TaskPriority priority, TaskStatus status, LocalDate dueDate, String section) {
        Task task = new Task(title, description, assignee, priority, status, dueDate); task.setSection(section); task.setOwnerUsername(owner);
        task.setSortOrder((int) repository.countByOwnerUsernameAndStatus(owner, status)); return repository.save(task);
    }
    public Task updateForUser(String owner, Long id, String title, String description, String assignee, TaskPriority priority, TaskStatus status, LocalDate dueDate, String section) {
        Task task = getForUser(id, owner);
        if (status == TaskStatus.DONE && task.getStatus() != TaskStatus.DONE) ensureChecklistMayBeCompleted(id, false);
        task.update(title, description, assignee, priority, status, dueDate, section); return repository.save(task);
    }
    public void deleteForUser(String owner, Long id) { repository.delete(getForUser(id, owner)); }
    public void updateStatusForUser(String owner, Long id, TaskStatus status, boolean allowIncomplete, boolean markChecklistComplete) {
        getForUser(id, owner); updateStatus(id, status, allowIncomplete, markChecklistComplete);
    }
    public void reorderForUser(String owner, Long id, TaskStatus status, int position, boolean allowIncomplete, boolean markChecklistComplete) {
        getForUser(id, owner);
        if (status == TaskStatus.DONE && markChecklistComplete) markAllChecklistItemsComplete(id);
        if (status == TaskStatus.DONE) ensureChecklistMayBeCompleted(id, allowIncomplete);
        Task task = getForUser(id, owner); task.update(task.getTitle(), task.getDescription(), task.getAssignee(), task.getPriority(), status, task.getDueDate()); repository.save(task);
        List<Task> ordered = repository.findByOwnerUsernameAndStatusOrderBySortOrderAscUpdatedAtDesc(owner, status);
        ordered.removeIf(item -> item.getId().equals(id)); int safe = Math.max(0, Math.min(position, ordered.size())); ordered.add(safe, task);
        for (int i=0; i<ordered.size(); i++) ordered.get(i).setSortOrder(i); repository.saveAll(ordered);
    }
    public List<ChecklistItem> checklistForUser(String owner, Long id) { getForUser(id, owner); return checklistRepository.findByTaskIdOrderByIdAsc(id); }
    public ChecklistItem addChecklistItemForUser(String owner, Long id, String text) { getForUser(id, owner); return addChecklistItem(id, text); }
    public void toggleChecklistItemForUser(String owner, Long id, Long itemId, boolean completed) { getForUser(id, owner); toggleChecklistItem(id, itemId, completed); }
    public void deleteChecklistItemForUser(String owner, Long id, Long itemId) { getForUser(id, owner); deleteChecklistItem(id, itemId); }
    public List<TaskAttachment> attachmentsForUser(String owner, Long id) { getForUser(id, owner); return attachments(id); }
    public TaskAttachment saveAttachmentForUser(String owner, Long id, String originalName, String storedName, String contentType, long size) { getForUser(id, owner); return saveAttachment(id, originalName, storedName, contentType, size); }
    public TaskAttachment getAttachmentForUser(String owner, Long id, Long attachmentId) { getForUser(id, owner); return getAttachment(id, attachmentId); }
    public void deleteAttachmentForUser(String owner, Long id, Long attachmentId) { getForUser(id, owner); deleteAttachment(id, attachmentId); }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(java.util.Locale.ROOT).contains(query);
    }

    @Transactional(readOnly = true)
    public Task get(Long id) {
        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    public Task create(String title, String description, String assignee, TaskPriority priority, TaskStatus status) {
        return create(title, description, assignee, priority, status, null);
    }

    public Task create(String title, String description, String assignee, TaskPriority priority, TaskStatus status, LocalDate dueDate) {
        return create(title, description, assignee, priority, status, dueDate, "General");
    }

    public Task create(String title, String description, String assignee, TaskPriority priority, TaskStatus status, LocalDate dueDate, String section) {
        Task task = new Task(title, description, assignee, priority, status, dueDate);
        task.setSection(section);
        task.setSortOrder((int) repository.countByStatus(status));
        return repository.save(task);
    }

    public List<ChecklistItem> checklist(Long taskId) { get(taskId); return checklistRepository.findByTaskIdOrderByIdAsc(taskId); }
    public ChecklistItem addChecklistItem(Long taskId, String text) { get(taskId); if (text == null || text.isBlank() || text.trim().length() > 180) throw new IllegalArgumentException("Checklist item must be between 1 and 180 characters."); return checklistRepository.save(new ChecklistItem(taskId, text)); }
    public void toggleChecklistItem(Long taskId, Long itemId, boolean completed) { get(taskId); ChecklistItem item = checklistRepository.findById(itemId).orElseThrow(() -> new IllegalArgumentException("Checklist item not found.")); if (!item.getTaskId().equals(taskId)) throw new IllegalArgumentException("Checklist item does not belong to this task."); item.setCompleted(completed); checklistRepository.save(item); }
    public void deleteChecklistItem(Long taskId, Long itemId) { get(taskId); ChecklistItem item = checklistRepository.findById(itemId).orElseThrow(() -> new IllegalArgumentException("Checklist item not found.")); if (!item.getTaskId().equals(taskId)) throw new IllegalArgumentException("Checklist item does not belong to this task."); checklistRepository.delete(item); }
    public List<TaskAttachment> attachments(Long taskId) { get(taskId); return attachmentRepository.findByTaskIdOrderByUploadedAtDesc(taskId); }
    public TaskAttachment saveAttachment(Long taskId, String originalName, String storedName, String contentType, long size) { get(taskId); return attachmentRepository.save(new TaskAttachment(taskId, originalName, storedName, contentType, size)); }
    public TaskAttachment getAttachment(Long taskId, Long attachmentId) { get(taskId); TaskAttachment a = attachmentRepository.findById(attachmentId).orElseThrow(() -> new IllegalArgumentException("Attachment not found.")); if (!a.getTaskId().equals(taskId)) throw new IllegalArgumentException("Attachment does not belong to this task."); return a; }
    public void deleteAttachment(Long taskId, Long attachmentId) { attachmentRepository.delete(getAttachment(taskId, attachmentId)); }
    public void reorder(Long taskId, TaskStatus status, int position) {
        reorder(taskId, status, position, false);
    }

    public void reorder(Long taskId, TaskStatus status, int position, boolean allowIncompleteChecklist) {
        reorder(taskId, status, position, allowIncompleteChecklist, false);
    }

    public void reorder(Long taskId, TaskStatus status, int position, boolean allowIncompleteChecklist, boolean markChecklistComplete) {
        Task task = get(taskId);
        if (status == TaskStatus.DONE && markChecklistComplete) markAllChecklistItemsComplete(taskId);
        if (status == TaskStatus.DONE) ensureChecklistMayBeCompleted(taskId, allowIncompleteChecklist);
        task.update(task.getTitle(), task.getDescription(), task.getAssignee(), task.getPriority(), status, task.getDueDate());
        repository.save(task);
        List<Task> ordered = repository.findByStatusOrderBySortOrderAscUpdatedAtDesc(status);
        ordered.removeIf(item -> item.getId().equals(taskId));
        int safePosition = Math.max(0, Math.min(position, ordered.size()));
        ordered.add(safePosition, task);
        for (int i = 0; i < ordered.size(); i++) ordered.get(i).setSortOrder(i);
        repository.saveAll(ordered);
    }

    public Task update(Long id, String title, String description, String assignee, TaskPriority priority, TaskStatus status) {
        return update(id, title, description, assignee, priority, status, get(id).getDueDate());
    }

    public Task update(Long id, String title, String description, String assignee, TaskPriority priority, TaskStatus status, LocalDate dueDate) {
        return update(id, title, description, assignee, priority, status, dueDate, get(id).getSection());
    }

    public Task update(Long id, String title, String description, String assignee, TaskPriority priority, TaskStatus status, LocalDate dueDate, String section) {
        Task task = get(id);
        if (status == TaskStatus.DONE && task.getStatus() != TaskStatus.DONE) ensureChecklistMayBeCompleted(id, false);
        task.update(title, description, assignee, priority, status, dueDate, section);
        return repository.save(task);
    }

    public Task updateStatus(Long id, TaskStatus status) {
        return updateStatus(id, status, false);
    }

    public Task updateStatus(Long id, TaskStatus status, boolean allowIncompleteChecklist) {
        return updateStatus(id, status, allowIncompleteChecklist, false);
    }

    public Task updateStatus(Long id, TaskStatus status, boolean allowIncompleteChecklist, boolean markChecklistComplete) {
        Task task = get(id);
        if (status == TaskStatus.DONE && markChecklistComplete) markAllChecklistItemsComplete(id);
        if (status == TaskStatus.DONE) ensureChecklistMayBeCompleted(id, allowIncompleteChecklist);
        task.update(task.getTitle(), task.getDescription(), task.getAssignee(), task.getPriority(), status, task.getDueDate());
        return repository.save(task);
    }

    private void markAllChecklistItemsComplete(Long taskId) {
        List<ChecklistItem> items = checklistRepository.findByTaskIdOrderByIdAsc(taskId);
        for (ChecklistItem item : items) {
            if (!item.isCompleted()) item.setCompleted(true);
        }
        checklistRepository.saveAll(items);
    }

    private void ensureChecklistMayBeCompleted(Long taskId, boolean allowIncompleteChecklist) {
        List<ChecklistItem> items = checklistRepository.findByTaskIdOrderByIdAsc(taskId);
        boolean incomplete = !items.isEmpty() && items.stream().anyMatch(item -> !item.isCompleted());
        if (incomplete && !allowIncompleteChecklist) {
            throw new IllegalStateException("The checklist is not fully complete. Finish all items or explicitly confirm that you want to mark the task complete anyway.");
        }
    }

    public void delete(Long id) {
        Task task = get(id);
        repository.delete(task);
    }

    public long countByStatus(TaskStatus status) {
        return repository.countByStatus(status);
    }
}
