package com.example.trellite.controller;

import com.example.trellite.dto.TaskCreateDTO;
import com.example.trellite.dto.TaskResponseDTO;
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
    private final AuthenticationManager authenticationManager;

    public TaskController(TaskService taskService, AuthenticationManager authenticationManager) {
        this.taskService = taskService;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping
    public ResponseEntity<List<TaskResponseDTO>> getTasks(
            @PathVariable Integer boardId,
            @PathVariable Integer listId
    ) {
        List<TaskResponseDTO> tasks = taskService.getTasks( listId);
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
        TaskResponseDTO createdTask = taskService.createTask(listId, taskCreateDTO);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponseDTO> getTask(
            @RequestParam Integer taskId,
            @PathVariable Integer boardId, // No use
            @PathVariable Integer listId // No use
    ) {
        TaskResponseDTO task = taskService.getTask(taskId);
        return new ResponseEntity<>(task, HttpStatus.OK);
    }


}
