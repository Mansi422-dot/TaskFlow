package com.mansi.taskflow.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mansi.taskflow.model.Task;
import com.mansi.taskflow.model.TaskPriority;
import com.mansi.taskflow.model.TaskStatus;
import com.mansi.taskflow.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskApiController.class)
class TaskApiControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean TaskService service;
    @MockBean com.mansi.taskflow.repository.UserAccountRepository userAccountRepository;

    @Test @WithMockUser(username="user") void listsTasks() throws Exception {
        when(service.findTasksForUser("user", null, null, null)).thenReturn(List.of());
        mvc.perform(get("/api/tasks")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test @WithMockUser(username="user") void createsTask() throws Exception {
        Task task = new Task("Demo", "API test", "Mansi", TaskPriority.HIGH, TaskStatus.TODO);
        when(service.createForUser(anyString(), anyString(), anyString(), anyString(), any(), any(), any(), anyString())).thenReturn(task);
        String body = """
            {"title":"Demo","description":"API test","assignee":"Mansi","priority":"HIGH","status":"TODO","dueDate":"2026-10-20"}
            """;
        mvc.perform(post("/api/tasks").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("Demo"));
    }

    @Test @WithMockUser(username="user") void rejectsBlankTitle() throws Exception {
        String body = """
            {"title":" ","description":"","assignee":"Mansi","priority":"MEDIUM","status":"TODO","dueDate":null}
            """;
        mvc.perform(post("/api/tasks").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
