package com.example.trellite.service;

import com.example.trellite.dto.AssignTaskDTO;
import com.example.trellite.dto.MoveTaskDTO;
import com.example.trellite.dto.TaskCreateDTO;
import com.example.trellite.dto.TaskResponseDTO;
import com.example.trellite.enums.Priority;
import com.example.trellite.enums.TaskStatus;
import com.example.trellite.exception.ForbiddenException;
import com.example.trellite.exception.ResourceNotFoundException;
import com.example.trellite.model.Board;
import com.example.trellite.model.Task;
import com.example.trellite.model.TaskList;
import com.example.trellite.model.User;
import com.example.trellite.repository.AuthRepo;
import com.example.trellite.repository.ListRepo;
import com.example.trellite.repository.TaskRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepo taskRepo;
    @Mock ListRepo listRepo;
    @Mock AuthRepo userRepo;
    @Mock BoardAccessGuard access;

    TaskService taskService;

    User owner = User.builder().id(1L).username("owner").email("owner@x.com").build();
    User member = User.builder().id(2L).username("member").email("member@x.com").build();
    User outsider = User.builder().id(3L).username("outsider").email("outsider@x.com").build();

    Board board = Board.builder().boardId(10).owner(owner).members(Set.of(member)).build();
    Board otherBoard = Board.builder().boardId(20).owner(outsider).build();

    TaskList list = TaskList.builder().listId(100).listName("To do").position(0).board(board).build();
    TaskList listOnOtherBoard = TaskList.builder().listId(200).listName("Theirs").position(0).board(otherBoard).build();

    Task task;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepo, listRepo, userRepo, access);
        task = Task.builder().taskId(1000).title("Card").status(TaskStatus.TODO)
                .priority(Priority.MEDIUM).taskList(list).build();

        lenient().when(access.requireVisibleBoard(10)).thenReturn(board);
        lenient().when(listRepo.findById(100)).thenReturn(Optional.of(list));
        lenient().when(listRepo.findById(200)).thenReturn(Optional.of(listOnOtherBoard));
        lenient().when(taskRepo.findById(1000)).thenReturn(Optional.of(task));
        lenient().when(taskRepo.save(any(Task.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void everyTaskReadGoesThroughTheBoardAccessCheck() {
        taskService.getTasks(10, 100);
        verify(access).requireVisibleBoard(10);
    }

    @Test
    void aListFromAnotherBoardIsNotReachableThroughThisBoardsUrl() {
        assertThatThrownBy(() -> taskService.getTasks(10, 200))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("does not belong to this board");
    }

    @Test
    void createTaskFallsBackToMediumPriority() {
        TaskResponseDTO created = taskService.createTask(10, 100,
                new TaskCreateDTO("Write it up", null, null, null, null));

        assertThat(created.priority()).isEqualTo(Priority.MEDIUM);
        assertThat(created.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void taskCannotBeMovedToAListOnAnotherBoard() {
        assertThatThrownBy(() -> taskService.moveTask(10, 100, 1000, new MoveTaskDTO(200)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("same board");
        verify(taskRepo, never()).save(any(Task.class));
    }

    @Test
    void taskCanBeMovedBetweenListsOnItsOwnBoard() {
        TaskList second = TaskList.builder().listId(101).listName("Doing").position(1).board(board).build();
        when(listRepo.findById(101)).thenReturn(Optional.of(second));

        TaskResponseDTO moved = taskService.moveTask(10, 100, 1000, new MoveTaskDTO(101));

        assertThat(moved.taskListId()).isEqualTo(101);
    }

    @Test
    void cardCannotBeAssignedToSomeoneWhoIsNotOnTheBoard() {
        when(userRepo.findById(3L)).thenReturn(Optional.of(outsider));
        when(access.findBoard(10)).thenReturn(board);
        when(access.canAccess(board, outsider)).thenReturn(false);

        assertThatThrownBy(() -> taskService.assignTask(10, 100, 1000, new AssignTaskDTO(3L)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not on this board");
        verify(taskRepo, never()).save(any(Task.class));
    }

    @Test
    void cardCanBeAssignedToABoardMember() {
        when(userRepo.findById(2L)).thenReturn(Optional.of(member));
        when(access.findBoard(10)).thenReturn(board);
        when(access.canAccess(board, member)).thenReturn(true);

        TaskResponseDTO assigned = taskService.assignTask(10, 100, 1000, new AssignTaskDTO(2L));

        assertThat(assigned.assignee()).isNotNull();
        assertThat(assigned.assignee().userId()).isEqualTo(2L);
    }

    @Test
    void nullAssigneeClearsTheAssignment() {
        task.setAssignee(member);

        TaskResponseDTO cleared = taskService.assignTask(10, 100, 1000, new AssignTaskDTO(null));

        assertThat(cleared.assignee()).isNull();
    }

    @Test
    void aTaskFromAnotherListIsNotReachableThroughThisListsUrl() {
        Task elsewhere = Task.builder().taskId(2000).title("Other")
                .taskList(TaskList.builder().listId(101).board(board).build()).build();
        when(taskRepo.findById(2000)).thenReturn(Optional.of(elsewhere));

        assertThatThrownBy(() -> taskService.getTask(10, 100, 2000))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("does not belong to this list");
    }
}
