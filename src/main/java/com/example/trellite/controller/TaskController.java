package com.example.trellite.controller;

import com.example.trellite.dto.*;
import com.example.trellite.exception.ResourceNotFoundException;
import com.example.trellite.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/boards/{boardId}/lists/{listId}/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService, AuthenticationManager authenticationManager) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskResponseDTO>> getTasks(
            @PathVariable Integer boardId,
            @PathVariable Integer listId
    ) {
        List<TaskResponseDTO> tasks = taskService.getTasks(boardId, listId);
        if(tasks.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<TaskResponseDTO> createTask(
            @PathVariable Integer boardId, // No use here, only for URL semantics
            @PathVariable Integer listId,
            @RequestBody TaskCreateDTO taskCreateDTO
    ) {
        TaskResponseDTO createdTask = taskService.createTask(boardId, listId, taskCreateDTO);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponseDTO> getTask(
            @PathVariable Integer taskId,
            @PathVariable Integer boardId, // No use
            @PathVariable Integer listId // No use
    ) {
        TaskResponseDTO task = taskService.getTask(boardId, listId, taskId);
        return new ResponseEntity<>(task, HttpStatus.OK);
    }

    @PatchMapping("/{taskId}")
    public ResponseEntity<TaskResponseDTO> updateTask(
            @PathVariable Integer taskId,
            @PathVariable Integer boardId, // No use
            @PathVariable Integer listId, // No use
            @RequestBody TaskUpdateDTO taskUpdateDTO
    ) {
        TaskResponseDTO updatedTask = taskService.updateTask(boardId, listId, taskId, taskUpdateDTO);
        return new ResponseEntity<>(updatedTask, HttpStatus.OK);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Integer boardId,
            @PathVariable Integer listId,
            @PathVariable Integer taskId
    ) {
        taskService.deleteTask(boardId, listId, taskId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/{taskId}/move")
    public ResponseEntity<TaskResponseDTO> moveTask(
            @PathVariable Integer boardId,
            @PathVariable Integer listId,
            @PathVariable Integer taskId,
            @RequestBody MoveTaskDTO taskMoveDTO
            ) {
        TaskResponseDTO taskResponseDTO = taskService.moveTask(boardId, listId, taskId, taskMoveDTO);
        return new ResponseEntity<>(taskResponseDTO, HttpStatus.OK);
    }

    // Controller
    @PatchMapping("/{taskId}/assign")
    public ResponseEntity<TaskResponseDTO> assignTask(
            @PathVariable Integer taskId,
            @RequestBody AssignTaskDTO assignTaskDTO) {
        return new ResponseEntity<>(taskService.assignTask(taskId, assignTaskDTO), HttpStatus.OK);
    }

}
