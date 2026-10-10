package com.mansi.taskflow.web;

import com.mansi.taskflow.model.Task;
import com.mansi.taskflow.model.TaskPriority;
import com.mansi.taskflow.model.TaskStatus;
import com.mansi.taskflow.service.TaskNotFoundException;
import com.mansi.taskflow.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@Validated
public class TaskApiController {
    private final TaskService service;
    public TaskApiController(TaskService service) { this.service = service; }

    @GetMapping
    public List<Task> list(@RequestParam(required = false) String query,
                           @RequestParam(required = false) TaskStatus status,
                           @RequestParam(required = false) TaskPriority priority, java.security.Principal principal) {
        return service.findTasksForUser(principal.getName(), query, status, priority);
    }

    @GetMapping("/{id}")
    public Task get(@PathVariable Long id, java.security.Principal principal) { return service.getForUserPublic(id, principal.getName()); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Task create(@Valid @RequestBody TaskRequest request, java.security.Principal principal) {
        if (request.section() == null || request.section().isBlank())
            return service.createForUser(principal.getName(), request.title(), request.description(), request.assignee(), request.priority(), request.status(), request.dueDate(), "General");
        return service.createForUser(principal.getName(), request.title(), request.description(), request.assignee(), request.priority(), request.status(), request.dueDate(), request.section());
    }

    @PutMapping("/{id}")
    public Task update(@PathVariable Long id, @Valid @RequestBody TaskRequest request, java.security.Principal principal) {
        if (request.section() == null || request.section().isBlank())
            return service.updateForUser(principal.getName(), id, request.title(), request.description(), request.assignee(), request.priority(), request.status(), request.dueDate(), "General");
        return service.updateForUser(principal.getName(), id, request.title(), request.description(), request.assignee(), request.priority(), request.status(), request.dueDate(), request.section());
    }

    @PatchMapping("/{id}/status")
    public Task updateStatus(@PathVariable Long id, @RequestBody StatusRequest request, java.security.Principal principal) {
        service.updateStatusForUser(principal.getName(), id, request.status(), false, false); return service.getForUserPublic(id, principal.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, java.security.Principal principal) { service.deleteForUser(principal.getName(), id); }

    public record TaskRequest(
            @NotBlank @Size(max = 120) String title,
            @Size(max = 2000) String description,
            @NotBlank @Size(max = 80) String assignee,
            TaskPriority priority,
            TaskStatus status,
            LocalDate dueDate,
            @Size(max = 80) String section) {}

    public record StatusRequest(TaskStatus status) {}

    @ExceptionHandler(TaskNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound(TaskNotFoundException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, jakarta.validation.ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(Exception ex) {
        return Map.of("error", ex.getMessage() == null ? "Invalid request." : ex.getMessage());
    }
}
