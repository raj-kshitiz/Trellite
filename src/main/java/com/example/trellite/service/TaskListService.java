package com.example.trellite.service;

import com.example.trellite.dto.TaskListCreateDTO;
import com.example.trellite.dto.TaskListResponseDTO;
import com.example.trellite.dto.TaskListUpdateDTO;
import com.example.trellite.exception.ResourceNotFoundException;
import com.example.trellite.model.Board;
import com.example.trellite.model.TaskList;
import com.example.trellite.repository.ListRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskListService {

    private final ListRepo listRepo;
    private final BoardAccessGuard access;

    public TaskListService(ListRepo listRepo, BoardAccessGuard access) {
        this.listRepo = listRepo;
        this.access = access;
    }

    private TaskListResponseDTO mapToTaskListResponseDTO(TaskList taskList) {
        return new TaskListResponseDTO(
                taskList.getListId(),
                taskList.getListName(),
                taskList.getPosition(),
                taskList.getCreatedAt()
        );
    }

    /**
     * Loads a list after proving the caller may work on the board, and that the list is
     * actually on the board named in the URL. Every method here goes through it — a list
     * id on its own used to be enough to delete a list, and its tasks with it.
     */
    private TaskList requireList(Integer boardId, Integer listId) {
        access.requireVisibleBoard(boardId);
        TaskList taskList = listRepo.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found"));
        if (!taskList.getBoard().getBoardId().equals(boardId)) {
            throw new ResourceNotFoundException("List does not belong to this board");
        }
        return taskList;
    }

    public List<TaskListResponseDTO> getLists(Integer boardId) {
        access.requireVisibleBoard(boardId);
        return listRepo.findByBoard_BoardId(boardId)
                .stream()
                .map(this::mapToTaskListResponseDTO)
                .collect(Collectors.toList());
    }

    public TaskListResponseDTO createList(TaskListCreateDTO taskListCreateDTO, Integer boardId) {
        Board board = access.requireVisibleBoard(boardId);

        TaskList taskList = TaskList.builder()
                .listName(taskListCreateDTO.listName())
                .position(taskListCreateDTO.position())
                .board(board)
                .build();

        return mapToTaskListResponseDTO(listRepo.save(taskList));
    }

    /**
     * True partial update. This used to set both fields unconditionally, so a body with
     * only listName wrote position = null — and position is NOT NULL, so the request came
     * back as a 500 constraint violation.
     */
    public TaskListResponseDTO updateList(TaskListUpdateDTO taskListUpdateDTO,
                                          Integer listId,
                                          Integer boardId) {
        TaskList taskList = requireList(boardId, listId);

        if (taskListUpdateDTO.listName() != null)
            taskList.setListName(taskListUpdateDTO.listName());
        if (taskListUpdateDTO.position() != null)
            taskList.setPosition(taskListUpdateDTO.position());

        return mapToTaskListResponseDTO(listRepo.save(taskList));
    }

    public void deleteList(Integer boardId, Integer listId) {
        listRepo.delete(requireList(boardId, listId));
    }
}
