package com.example.trellite.service;

import com.example.trellite.dto.TaskListResponseDTO;
import com.example.trellite.dto.TaskListUpdateDTO;
import com.example.trellite.exception.ResourceNotFoundException;
import com.example.trellite.model.Board;
import com.example.trellite.model.TaskList;
import com.example.trellite.model.User;
import com.example.trellite.repository.ListRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskListServiceTest {

    @Mock ListRepo listRepo;
    @Mock BoardAccessGuard access;

    TaskListService taskListService;

    User owner = User.builder().id(1L).username("owner").build();
    Board board = Board.builder().boardId(10).owner(owner).build();
    Board otherBoard = Board.builder().boardId(20).owner(owner).build();
    TaskList list = TaskList.builder().listId(100).listName("To do").position(3).board(board).build();
    TaskList foreignList = TaskList.builder().listId(200).listName("Theirs").position(0).board(otherBoard).build();

    @BeforeEach
    void setUp() {
        taskListService = new TaskListService(listRepo, access);
        lenient().when(access.requireVisibleBoard(10)).thenReturn(board);
        lenient().when(listRepo.findById(100)).thenReturn(Optional.of(list));
        lenient().when(listRepo.findById(200)).thenReturn(Optional.of(foreignList));
        lenient().when(listRepo.save(any(TaskList.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void updatingOnlyTheNameLeavesPositionAlone() {
        TaskListResponseDTO updated = taskListService.updateList(
                new TaskListUpdateDTO("Renamed", null), 100, 10);

        assertThat(updated.listName()).isEqualTo("Renamed");
        // Used to be overwritten with null, and position is NOT NULL — a 500 in practice.
        assertThat(updated.position()).isEqualTo(3);
    }

    @Test
    void updatingOnlyThePositionLeavesTheNameAlone() {
        TaskListResponseDTO updated = taskListService.updateList(
                new TaskListUpdateDTO(null, 7), 100, 10);

        assertThat(updated.listName()).isEqualTo("To do");
        assertThat(updated.position()).isEqualTo(7);
    }

    @Test
    void deletingAListChecksTheBoardItIsOn() {
        taskListService.deleteList(10, 100);

        verify(access).requireVisibleBoard(10);
        verify(listRepo).delete(list);
    }

    @Test
    void aListOnAnotherBoardCannotBeDeletedThroughThisBoardsUrl() {
        assertThatThrownBy(() -> taskListService.deleteList(10, 200))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(listRepo, never()).delete(any(TaskList.class));
    }
}
