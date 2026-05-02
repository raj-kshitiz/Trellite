package com.example.trellite.controller;

import com.example.trellite.dto.TaskListCreateDTO;
import com.example.trellite.dto.TaskListResponseDTO;
import com.example.trellite.dto.TaskListUpdateDTO;
import com.example.trellite.service.TaskListService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/boards/{boardId}/lists")
public class TaskListController {

    private final TaskListService taskListService;
    public TaskListController(TaskListService taskListService) {
        this.taskListService = taskListService;
    }

    @GetMapping
    public ResponseEntity<List<TaskListResponseDTO>> getLists(@PathVariable Integer boardId) {
        List<TaskListResponseDTO> lists = taskListService.getLists(boardId);
        if(lists.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(lists, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<TaskListResponseDTO> createList(
            @RequestBody TaskListCreateDTO taskListCreateDTO,
             @PathVariable Integer boardId
    ) {
        TaskListResponseDTO createdList = taskListService.createList(taskListCreateDTO, boardId);
        return new ResponseEntity<>(createdList, HttpStatus.CREATED);
    }

    @PatchMapping("/{listId}")
    public ResponseEntity<TaskListResponseDTO> updateList(
            @RequestBody TaskListUpdateDTO taskListUpdateDTO,
            @PathVariable Integer listId,
            @PathVariable Integer boardId
    ) {
        try {
            TaskListResponseDTO updatedList = taskListService.updateList(taskListUpdateDTO, listId, boardId);
            return new ResponseEntity<>(updatedList, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // Controller
    @DeleteMapping("/{listId}")
    public ResponseEntity<Void> deleteList(
            @PathVariable Integer listId,
            @PathVariable Integer boardId) {
        try {
            taskListService.deleteList(listId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}
