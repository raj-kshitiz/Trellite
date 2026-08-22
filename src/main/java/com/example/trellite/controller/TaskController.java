package com.example.trellite.controller;

import com.example.trellite.dto.*;
import com.example.trellite.service.AiTaskService;
import com.example.trellite.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/boards/{boardId}/lists/{listId}/tasks")
public class TaskController {

    private final TaskService taskService;
    private final AiTaskService aiTaskService;

    public TaskController(TaskService taskService, AiTaskService aiTaskService) {
        this.taskService = taskService;
        this.aiTaskService = aiTaskService;
    }

    /**
     * Turns plain English into an unsaved task draft. Returns a TaskCreateDTO, i.e.
     * exactly the body POST .../tasks expects, so the client can review it and send
     * it straight back to create the card. Nothing is persisted by this call.
     */
    @PostMapping("/draft")
    public ResponseEntity<TaskCreateDTO> draftTask(
            @PathVariable Integer boardId,
            @PathVariable Integer listId,
            @Valid @RequestBody TaskDraftRequestDTO taskDraftRequestDTO
    ) {
        TaskCreateDTO draft = aiTaskService.draftTask(boardId, listId, taskDraftRequestDTO.prompt());
        return new ResponseEntity<>(draft, HttpStatus.OK);
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
            @Valid @RequestBody TaskCreateDTO taskCreateDTO
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
            @Valid @RequestBody TaskUpdateDTO taskUpdateDTO
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
            @Valid @RequestBody MoveTaskDTO taskMoveDTO
            ) {
        TaskResponseDTO taskResponseDTO = taskService.moveTask(boardId, listId, taskId, taskMoveDTO);
        return new ResponseEntity<>(taskResponseDTO, HttpStatus.OK);
    }

    /** A null assigneeId clears the assignee. The target must be on the board. */
    @PatchMapping("/{taskId}/assign")
    public ResponseEntity<TaskResponseDTO> assignTask(
            @PathVariable Integer boardId,
            @PathVariable Integer listId,
            @PathVariable Integer taskId,
            @Valid @RequestBody AssignTaskDTO assignTaskDTO) {
        TaskResponseDTO assigned = taskService.assignTask(boardId, listId, taskId, assignTaskDTO);
        return new ResponseEntity<>(assigned, HttpStatus.OK);
    }

}
