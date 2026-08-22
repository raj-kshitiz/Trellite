package com.example.trellite.service;

import com.example.trellite.dto.TaskCreateDTO;
import com.example.trellite.dto.TaskDraftDTO;
import com.example.trellite.enums.Priority;
import com.example.trellite.exception.AiDraftException;
import com.example.trellite.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers the drafting path with a stubbed model, so the mapping, the failure handling and
 * the pre-flight authorization check are all verified without a live provider or an API key.
 */
@ExtendWith(MockitoExtension.class)
class AiTaskServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ChatClient chatClient;
    @Mock
    ChatClient.Builder chatClientBuilder;
    @Mock
    TaskService taskService;

    AiTaskService aiTaskService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        aiTaskService = new AiTaskService(chatClientBuilder, taskService);
    }

    private void modelReturns(TaskDraftDTO draft) {
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(TaskDraftDTO.class)).thenReturn(draft);
    }

    @Test
    void turnsAModelDraftIntoACreatableTask() {
        modelReturns(new TaskDraftDTO(
                "Fix the login redirect bug", "Users land on / after signing in",
                Priority.HIGH, "2026-08-24", "2026-08-28"));

        TaskCreateDTO draft = aiTaskService.draftTask(10, 100, "fix the login redirect bug by Friday, urgent");

        assertThat(draft.title()).isEqualTo("Fix the login redirect bug");
        assertThat(draft.taskDescription()).isEqualTo("Users land on / after signing in");
        assertThat(draft.priority()).isEqualTo(Priority.HIGH);
        // The model speaks ISO; the REST DTO holds real LocalDates.
        assertThat(draft.startDate()).isEqualTo(LocalDate.of(2026, 8, 24));
        assertThat(draft.deadline()).isEqualTo(LocalDate.of(2026, 8, 28));
    }

    @Test
    void emptyDatesAndDescriptionComeBackAsNullNotBlankStrings() {
        modelReturns(new TaskDraftDTO("Buy milk", "  ", Priority.LOW, "", null));

        TaskCreateDTO draft = aiTaskService.draftTask(10, 100, "buy milk");

        assertThat(draft.taskDescription()).isNull();
        assertThat(draft.startDate()).isNull();
        assertThat(draft.deadline()).isNull();
    }

    @Test
    void accessIsCheckedBeforeAnyTokensAreSpent() {
        doThrow(new ForbiddenException("You do not have access to this board"))
                .when(taskService).assertListBelongsToBoard(10, 100);

        assertThatThrownBy(() -> aiTaskService.draftTask(10, 100, "anything"))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(chatClient);
    }

    @Test
    void providerFailureBecomesAFixedMessage() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(TaskDraftDTO.class))
                .thenThrow(new RuntimeException("{\"error\":{\"code\":429,\"message\":\"quota exhausted for project 12345\"}}"));

        assertThatThrownBy(() -> aiTaskService.draftTask(10, 100, "anything"))
                .isInstanceOf(AiDraftException.class)
                .hasMessage("Task drafting is temporarily unavailable")
                // The upstream body carries provider-side identifiers; it must not travel.
                .hasMessageNotContaining("quota")
                .hasMessageNotContaining("12345");
    }

    @Test
    void anEmptyTitleIsRejectedRatherThanDraftingABlankCard() {
        modelReturns(new TaskDraftDTO("   ", "something", Priority.MEDIUM, null, null));

        assertThatThrownBy(() -> aiTaskService.draftTask(10, 100, "???"))
                .isInstanceOf(AiDraftException.class)
                .hasMessageContaining("Could not derive a task");
    }

    @Test
    void anUnparseableDateIsRejected() {
        modelReturns(new TaskDraftDTO("Ship it", null, Priority.MEDIUM, "next tuesday", null));

        assertThatThrownBy(() -> aiTaskService.draftTask(10, 100, "ship it next tuesday"))
                .isInstanceOf(AiDraftException.class)
                .hasMessageContaining("startDate");
    }

    @Test
    void theBoardAndListAreValidatedOnEveryDraft() {
        modelReturns(new TaskDraftDTO("Ship it", null, Priority.MEDIUM, null, null));

        aiTaskService.draftTask(10, 100, "ship it");

        verify(taskService).assertListBelongsToBoard(10, 100);
    }
}
