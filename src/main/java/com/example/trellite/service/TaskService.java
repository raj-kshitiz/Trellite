package com.example.trellite.service;

import com.example.trellite.dto.*;
import com.example.trellite.enums.TaskStatus;
import com.example.trellite.exception.ResourceNotFoundException;
import com.example.trellite.model.Task;
import com.example.trellite.model.TaskList;
import com.example.trellite.model.User;
import com.example.trellite.repository.AuthRepo;
import com.example.trellite.repository.BoardRepo;
import com.example.trellite.repository.ListRepo;
import com.example.trellite.repository.TaskRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepo taskRepo;
    private final ListRepo listRepo;
    private final BoardRepo boardRepo;
    private final AuthRepo userRepo;

    public TaskService(TaskRepo taskRepo, ListRepo listRepo, BoardRepo boardRepo, AuthRepo userRepo) {
        this.taskRepo = taskRepo;
        this.listRepo = listRepo;
        this.boardRepo = boardRepo;
        this.userRepo = userRepo;
    }

    private TaskResponseDTO mapToTaskResponseDTO(Task task) {
        UserSummaryDTO assignee = null;
        if (task.getAssignee() != null) {
            assignee = new UserSummaryDTO(
                    task.getAssignee().getId(),
                    task.getAssignee().getUsername(),
                    task.getAssignee().getEmail()
            );
        }

        return new TaskResponseDTO(
                task.getTaskId(),
                task.getTitle(),
                task.getTaskDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getStartDate(),
                task.getDeadline(),
                task.getTaskList().getListId(),
                assignee
        );
    }

    public List<TaskResponseDTO> getTasks(Integer boardId, Integer listId) {

        boardRepo.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
        listRepo.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found"));

        List<Task> tasks = taskRepo.findByTaskList_ListId(listId);
        return tasks.stream()
                .map(this::mapToTaskResponseDTO)
                .collect(Collectors.toList());
    }

    public TaskResponseDTO createTask(Integer boardId, Integer listId, TaskCreateDTO taskCreateDTO)
    {
        boardRepo.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
        TaskList taskList = listRepo.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found"));

        Task task = Task.builder()
                .title(taskCreateDTO.title())
                .taskDescription(taskCreateDTO.taskDescription())
                .status(TaskStatus.TODO)
                .priority(taskCreateDTO.priority())
                .startDate(taskCreateDTO.startDate())
                .deadline(taskCreateDTO.deadline())
                .taskList(taskList)
                .build();

        return mapToTaskResponseDTO(taskRepo.save(task));
    }

    public TaskResponseDTO getTask(Integer boardId, Integer listId, Integer taskId) {

        boardRepo.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
        listRepo.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found"));

        return mapToTaskResponseDTO(taskRepo.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found")));
    }

    public TaskResponseDTO updateTask(Integer boardId, Integer listId, Integer taskId, TaskUpdateDTO taskUpdateDTO) {
        boardRepo.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
        listRepo.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found"));
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (taskUpdateDTO.title() != null)
            task.setTitle(taskUpdateDTO.title());
        if (taskUpdateDTO.taskDescription() != null)
            task.setTaskDescription(taskUpdateDTO.taskDescription());
        if (taskUpdateDTO.status() != null)
            task.setStatus(taskUpdateDTO.status());
        if (taskUpdateDTO.priority() != null)
            task.setPriority(taskUpdateDTO.priority());
        if (taskUpdateDTO.startDate() != null)
            task.setStartDate(taskUpdateDTO.startDate());
        if (taskUpdateDTO.deadline() != null)
            task.setDeadline(taskUpdateDTO.deadline());

        return mapToTaskResponseDTO(taskRepo.save(task));
    }

    public void deleteTask(Integer boardId, Integer listId, Integer taskId) {
        boardRepo.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
        listRepo.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found"));
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        taskRepo.delete(task);
    }

    public TaskResponseDTO moveTask(Integer boardId, Integer listId, Integer taskId, MoveTaskDTO taskMoveDTO) {
        boardRepo.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
        listRepo.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found"));
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        TaskList targetList = listRepo.findById(taskMoveDTO.listId())
                .orElseThrow(() -> new ResourceNotFoundException("Target list not found"));

        task.setTaskList(targetList);

        return mapToTaskResponseDTO(taskRepo.save(task));
    }

    public TaskResponseDTO assignTask(Integer taskId, AssignTaskDTO assignTaskDTO) {
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        User assignee = userRepo.findById(assignTaskDTO.assigneeId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        task.setAssignee(assignee);
        return mapToTaskResponseDTO(taskRepo.save(task));
    }
}
