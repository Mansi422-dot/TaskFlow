package com.mansi.taskflow.web;

import com.mansi.taskflow.model.Task;
import com.mansi.taskflow.model.TaskPriority;
import com.mansi.taskflow.model.TaskStatus;
import com.mansi.taskflow.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.mansi.taskflow.model.TaskAttachment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Controller
public class PageController {
    private final TaskService service;
    private final com.mansi.taskflow.service.RegistrationService registrationService;
    private final com.mansi.taskflow.service.PasswordResetService passwordResetService;
    public PageController(TaskService service, com.mansi.taskflow.service.RegistrationService registrationService,
                          com.mansi.taskflow.service.PasswordResetService passwordResetService) {
        this.service = service;
        this.registrationService = registrationService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String requestPasswordReset(@RequestParam(required = false) String email, RedirectAttributes redirect) {
        try {
            passwordResetService.requestReset(email);
        } catch (RuntimeException ex) {
            // Keep the response generic; do not reveal whether an account exists.
            org.slf4j.LoggerFactory.getLogger(PageController.class).warn("Unable to process password reset request.");
        }
        redirect.addFlashAttribute("message", "If an account exists for that email and email delivery is configured, a reset link will be sent shortly.");
        return "redirect:/forgot-password?submitted";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token == null ? "" : token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token, @RequestParam String password,
                                @RequestParam("confirmPassword") String confirmPassword, RedirectAttributes redirect) {
        if (password == null || !password.equals(confirmPassword)) {
            redirect.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/reset-password?token=" + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);
        }
        if (password.length() < 10 || password.length() > 72) {
            redirect.addFlashAttribute("error", "Password must be between 10 and 72 characters.");
            return "redirect:/reset-password?token=" + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);
        }
        boolean changed = passwordResetService.resetPassword(token, password);
        if (changed) {
            redirect.addFlashAttribute("success", "Your password has been reset. Please sign in with your new password.");
            return "redirect:/login?passwordReset";
        }
        redirect.addFlashAttribute("error", "This reset link is invalid or expired. Request a new one.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/register")
    public String registerPage(Model model) { model.addAttribute("registration", new RegistrationForm()); return "register"; }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registration") RegistrationForm form, BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) { model.addAttribute("error", result.getFieldErrors().stream().map(e -> e.getDefaultMessage()).findFirst().orElse("Please check the form.")); return "register"; }
        try { registrationService.register(form.getUsername(), form.getEmail(), form.getPassword());
            redirect.addFlashAttribute("success", "Account created. Sign in with your new credentials."); return "redirect:/login?registered";
        } catch (IllegalArgumentException ex) { model.addAttribute("error", ex.getMessage()); return "register"; }
    }

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String query,
                            @RequestParam(required = false) TaskStatus status,
                            @RequestParam(required = false) TaskPriority priority,
                            @RequestParam(required = false) String section,
                            Model model, java.security.Principal principal) {
        String owner = principal.getName();
        var allTasks = service.findTasksForUser(owner, null, null, null);
        var safeAllTasks = allTasks == null ? new java.util.ArrayList<Task>() : new java.util.ArrayList<>(allTasks);
        var sections = safeAllTasks.stream().map(Task::getSection).distinct().sorted().toList();
        var taskList = service.findTasksForUser(owner, query, status, priority);
        var safeTasks = taskList == null ? new java.util.ArrayList<Task>() : new java.util.ArrayList<>(taskList);
        for (Task task : safeTasks) task.setChecklistItems(service.checklistForUser(owner, task.getId()));
        model.addAttribute("tasks", safeTasks);
        model.addAttribute("sections", sections);
        model.addAttribute("selectedSection", section == null ? "" : section);
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("priorities", TaskPriority.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPriority", priority);
        model.addAttribute("query", query == null ? "" : query);
        model.addAttribute("todoCount", service.countByStatusForUser(owner, TaskStatus.TODO));
        model.addAttribute("progressCount", service.countByStatusForUser(owner, TaskStatus.IN_PROGRESS));
        model.addAttribute("doneCount", service.countByStatusForUser(owner, TaskStatus.DONE));
        model.addAttribute("form", new TaskForm());
        model.addAttribute("currentUsername", owner);
        return "index";
    }

    @PostMapping("/tasks")
    public String create(@Valid @ModelAttribute("form") TaskForm form, BindingResult result,
                         RedirectAttributes redirect, java.security.Principal principal) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", result.getFieldErrors().stream()
                    .map(e -> e.getDefaultMessage()).findFirst().orElse("Please check the form."));
            return "redirect:/";
        }
        service.createForUser(principal.getName(), form.getTitle(), form.getDescription(), form.getAssignee(), form.getPriority(), form.getStatus(), form.getDueDate(), form.getSection());
        redirect.addFlashAttribute("success", "Task created successfully.");
        return "redirect:/";
    }

    @GetMapping("/tasks/{id}/edit")
    public String edit(@PathVariable Long id, Model model, java.security.Principal principal) {
        String owner = principal.getName();
        Task task = service.getForUserPublic(id, owner);
        TaskForm form = new TaskForm();
        form.setTitle(task.getTitle()); form.setDescription(task.getDescription());
        form.setAssignee(task.getAssignee()); form.setPriority(task.getPriority()); form.setStatus(task.getStatus());
        form.setDueDate(task.getDueDate()); form.setSection(task.getSection());
        model.addAttribute("taskId", id); model.addAttribute("form", form);
        model.addAttribute("sections", service.findTasksForUser(owner, null, null, null).stream().map(Task::getSection).distinct().sorted().toList());
        model.addAttribute("checklist", service.checklistForUser(owner, id));
        model.addAttribute("attachments", service.attachmentsForUser(owner, id));
        model.addAttribute("statuses", TaskStatus.values()); model.addAttribute("priorities", TaskPriority.values());
        return "edit";
    }

    @PostMapping("/tasks/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") TaskForm form,
                         BindingResult result, Model model, RedirectAttributes redirect, java.security.Principal principal) {
        if (result.hasErrors()) {
            model.addAttribute("taskId", id); model.addAttribute("statuses", TaskStatus.values());
            model.addAttribute("priorities", TaskPriority.values());
            model.addAttribute("sections", service.findTasksForUser(principal.getName(), null, null, null).stream().map(Task::getSection).distinct().sorted().toList());
            model.addAttribute("checklist", service.checklistForUser(principal.getName(), id));
            model.addAttribute("attachments", service.attachmentsForUser(principal.getName(), id));
            return "edit";
        }
        try {
            service.updateForUser(principal.getName(), id, form.getTitle(), form.getDescription(), form.getAssignee(), form.getPriority(), form.getStatus(), form.getDueDate(), form.getSection());
            redirect.addFlashAttribute("success", "Task updated successfully.");
            return "redirect:/";
        } catch (IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/tasks/" + id + "/edit";
        }
    }

    @PostMapping("/tasks/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam TaskStatus status,
                               @RequestParam(defaultValue = "false") boolean allowIncompleteChecklist,
                               @RequestParam(defaultValue = "false") boolean markChecklistComplete,
                               RedirectAttributes redirect, java.security.Principal principal) {
        try {
            service.updateStatusForUser(principal.getName(), id, status, allowIncompleteChecklist, markChecklistComplete);
            redirect.addFlashAttribute("success", "Task status updated.");
        } catch (IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/tasks/{id}/checklist")
    public String addChecklist(@PathVariable Long id, @RequestParam String text, RedirectAttributes redirect, java.security.Principal principal) {
        service.addChecklistItemForUser(principal.getName(), id, text); redirect.addFlashAttribute("success", "Checklist item added."); return "redirect:/tasks/" + id + "/edit";
    }
    @PostMapping("/tasks/{id}/checklist/{itemId}/toggle")
    public String toggleChecklist(@PathVariable Long id, @PathVariable Long itemId, @RequestParam(defaultValue="false") boolean completed, java.security.Principal principal) {
        service.toggleChecklistItemForUser(principal.getName(), id, itemId, completed); return "redirect:/tasks/" + id + "/edit";
    }
    @PostMapping("/tasks/{id}/checklist/{itemId}/delete")
    public String deleteChecklist(@PathVariable Long id, @PathVariable Long itemId, java.security.Principal principal) { service.deleteChecklistItemForUser(principal.getName(), id, itemId); return "redirect:/tasks/" + id + "/edit"; }

    @PostMapping("/tasks/{id}/attachments")
    public String uploadAttachment(@PathVariable Long id, @RequestParam("file") MultipartFile file, RedirectAttributes redirect, java.security.Principal principal) throws Exception {
        service.getForUserPublic(id, principal.getName());
        if (file.isEmpty()) { redirect.addFlashAttribute("error", "Choose a file first."); return "redirect:/tasks/" + id + "/edit"; }
        if (file.getSize() > 10L * 1024 * 1024) { redirect.addFlashAttribute("error", "Files must be 10 MB or smaller."); return "redirect:/tasks/" + id + "/edit"; }
        String original = Paths.get(file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename()).getFileName().toString();
        String stored = UUID.randomUUID() + "-" + original.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path dir = Paths.get(System.getProperty("taskflow.upload-dir", "./data/uploads")).toAbsolutePath().normalize(); Files.createDirectories(dir);
        Files.copy(file.getInputStream(), dir.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
        service.saveAttachmentForUser(principal.getName(), id, original, stored, file.getContentType() == null ? "application/octet-stream" : file.getContentType(), file.getSize());
        redirect.addFlashAttribute("success", "Attachment uploaded."); return "redirect:/tasks/" + id + "/edit";
    }
    @GetMapping("/tasks/{id}/attachments/{attachmentId}")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long id, @PathVariable Long attachmentId, java.security.Principal principal) throws Exception {
        TaskAttachment a = service.getAttachmentForUser(principal.getName(), id, attachmentId); Path dir = Paths.get(System.getProperty("taskflow.upload-dir", "./data/uploads")).toAbsolutePath().normalize(); Path file = dir.resolve(a.getStoredName()).normalize();
        if (!file.startsWith(dir) || !Files.exists(file)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(a.getContentType())).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + a.getOriginalName().replace("\"", "") + "\"").body(Files.readAllBytes(file));
    }
    @PostMapping("/tasks/{id}/attachments/{attachmentId}/delete")
    public String deleteAttachment(@PathVariable Long id, @PathVariable Long attachmentId, java.security.Principal principal) throws Exception {
        TaskAttachment a = service.getAttachmentForUser(principal.getName(), id, attachmentId); Path dir = Paths.get(System.getProperty("taskflow.upload-dir", "./data/uploads")).toAbsolutePath().normalize(); Files.deleteIfExists(dir.resolve(a.getStoredName()).normalize()); service.deleteAttachmentForUser(principal.getName(), id, attachmentId); return "redirect:/tasks/" + id + "/edit";
    }
    @PostMapping("/tasks/{id}/reorder")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> reorder(@PathVariable Long id, @RequestParam TaskStatus status,
            @RequestParam(defaultValue="0") int position,
            @RequestParam(defaultValue="false") boolean allowIncompleteChecklist,
            @RequestParam(defaultValue="false") boolean markChecklistComplete, java.security.Principal principal) {
        try {
            service.reorderForUser(principal.getName(), id, status, position, allowIncompleteChecklist, markChecklistComplete);
            return ResponseEntity.ok(java.util.Map.of("ok", true));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("ok", false, "message", ex.getMessage()));
        }
    }

    @PostMapping("/tasks/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect, java.security.Principal principal) {
        service.deleteForUser(principal.getName(), id);
        redirect.addFlashAttribute("success", "Task deleted.");
        return "redirect:/";
    }
}
