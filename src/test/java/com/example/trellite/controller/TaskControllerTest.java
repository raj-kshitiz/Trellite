package com.example.trellite.controller;

import com.example.trellite.dto.TaskCreateDTO;
import com.example.trellite.enums.Priority;
import com.example.trellite.exception.AiDraftException;
import com.example.trellite.exception.ForbiddenException;
import com.example.trellite.service.AiTaskService;
import com.example.trellite.service.JwtService;
import com.example.trellite.service.MyUserDetailsService;
import com.example.trellite.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The web layer of the drafting endpoint: validation, the status codes the exception
 * advice produces, and the fact that a draft comes back in exactly the shape the create
 * endpoint accepts. Security filters are off — authorization itself is covered in the
 * service tests.
 */
@WebMvcTest(controllers = TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TaskService taskService;
    @MockitoBean
    AiTaskService aiTaskService;

    // JwtFilter is a Filter bean, so the slice picks it up even with filters disabled;
    // these satisfy its constructor.
    @MockitoBean
    JwtService jwtService;
    @MockitoBean
    MyUserDetailsService userDetailsService;

    @Test
    void draftComesBackInTheShapeTheCreateEndpointAccepts() throws Exception {
        when(aiTaskService.draftTask(eq(10), eq(100), anyString())).thenReturn(new TaskCreateDTO(
                "Fix the login redirect bug", "Users land on / after signing in",
                Priority.HIGH, null, LocalDate.of(2026, 8, 28)));

        mockMvc.perform(post("/boards/10/lists/100/tasks/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"fix the login redirect bug by Friday, urgent\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Fix the login redirect bug"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.startDate").doesNotExist())
                // dd-MM-yyyy on the wire, as the create endpoint expects it back.
                .andExpect(jsonPath("$.deadline").value("28-08-2026"));
    }

    @Test
    void anEmptyPromptIsRejectedBeforeTheModelIsCalled() throws Exception {
        mockMvc.perform(post("/boards/10/lists/100/tasks/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Prompt cannot be empty"));
    }

    @Test
    void anOverlongPromptIsRejected() throws Exception {
        String tooLong = "x".repeat(1001);

        mockMvc.perform(post("/boards/10/lists/100/tasks/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Prompt cannot exceed 1000 characters"));
    }

    @Test
    void anUpstreamModelFailureIsA502WithNoUpstreamDetail() throws Exception {
        when(aiTaskService.draftTask(anyInt(), anyInt(), anyString()))
                .thenThrow(new AiDraftException("Task drafting is temporarily unavailable"));

        mockMvc.perform(post("/boards/10/lists/100/tasks/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"ship it\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(content().string("Task drafting is temporarily unavailable"));
    }

    @Test
    void draftingOnSomeoneElsesBoardIs403() throws Exception {
        when(aiTaskService.draftTask(anyInt(), anyInt(), anyString()))
                .thenThrow(new ForbiddenException("You do not have access to this board"));

        mockMvc.perform(post("/boards/10/lists/100/tasks/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"ship it\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("You do not have access to this board"));
    }

    @Test
    void createRejectsATaskWithNoTitle() throws Exception {
        mockMvc.perform(post("/boards/10/lists/100/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskDescription\":\"no title here\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Title cannot be empty"));
    }

    @Test
    void assignPassesTheBoardAndListFromTheUrlToTheService() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/boards/10/lists/100/tasks/1000/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":2}"))
                .andExpect(status().isOk());

        // The whole path, not just the task id — the old signature took taskId alone.
        verify(taskService).assignTask(eq(10), eq(100), eq(1000), any());
    }
}
