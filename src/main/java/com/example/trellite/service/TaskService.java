package com.example.trellite.service;

import com.example.trellite.dto.TaskCreateDTO;
import com.example.trellite.dto.TaskResponseDTO;
import com.example.trellite.dto.UserSummaryDTO;
import com.example.trellite.enums.TaskStatus;
import com.example.trellite.model.Task;
import com.example.trellite.model.TaskList;
import com.example.trellite.repository.ListRepo;
import com.example.trellite.repository.TaskRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepo taskRepo;
    private final ListRepo listRepo;

    public TaskService(TaskRepo taskRepo, ListRepo listRepo) {
        this.taskRepo = taskRepo;
        this.listRepo = listRepo;
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

    public List<TaskResponseDTO> getTasks(Integer listId) {
        List<Task> tasks = taskRepo.findByTaskList_ListId(listId);
        return tasks.stream()
                .map(this::mapToTaskResponseDTO)
                .collect(Collectors.toList());
    }

    public TaskResponseDTO createTask(Integer listId, TaskCreateDTO taskCreateDTO)
    {
        TaskList taskList = listRepo.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));

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

    public TaskResponseDTO getTask(Integer taskId) {
        return mapToTaskResponseDTO(taskRepo.findById(taskId).orElseThrow(NullPointerException::new));
    }
}
