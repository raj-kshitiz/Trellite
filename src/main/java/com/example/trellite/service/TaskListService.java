package com.example.trellite.service;

import com.example.trellite.dto.TaskListCreateDTO;
import com.example.trellite.dto.TaskListResponseDTO;
import com.example.trellite.dto.TaskListUpdateDTO;
import com.example.trellite.model.Board;
import com.example.trellite.model.TaskList;
import com.example.trellite.repository.BoardRepo;
import com.example.trellite.repository.ListRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskListService {

    private final ListRepo listRepo;
    private final BoardRepo boardRepo;

    public TaskListService(ListRepo listRepo, BoardRepo boardRepo) {
        this.listRepo = listRepo;
        this.boardRepo = boardRepo;
    }

    private TaskListResponseDTO mapToTaskListResponseDTO(TaskList taskList) {
        return new TaskListResponseDTO(
                taskList.getListId(),
                taskList.getListName(),
                taskList.getPosition(),
                taskList.getCreatedAt()
        );
    }

    public List<TaskListResponseDTO> getLists(Integer boardId) {
        return listRepo.findByBoard_BoardId(boardId)
                .stream()
                .map(this::mapToTaskListResponseDTO)
                .collect(Collectors.toList());
    }

    public TaskListResponseDTO createList(
            TaskListCreateDTO taskListCreateDTO,
            Integer boardId
    ) {

        Board board = boardRepo.findById(boardId).orElseThrow(() -> new RuntimeException("Board not found"));

        TaskList taskList = TaskList.builder()
                .listName(taskListCreateDTO.listName())
                .position(taskListCreateDTO.position())
                .board(board)
                .build();

        return mapToTaskListResponseDTO(listRepo.save(taskList));
    }

    public TaskListResponseDTO updateList(
            TaskListUpdateDTO taskListUpdateDTO,
            Integer listId,
            Integer boardId
    ) {
        Board board = boardRepo.findById(boardId).orElseThrow(() -> new RuntimeException("Board not found"));
        TaskList taskList = listRepo.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));

        taskList.setListName(taskListUpdateDTO.listName());
        taskList.setPosition(taskListUpdateDTO.position());
        taskList.setBoard(board);

        return mapToTaskListResponseDTO(listRepo.save(taskList));
    }

    public void deleteList(Integer listId) {
        TaskList taskList = listRepo.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));
        listRepo.delete(taskList);
    }
}
