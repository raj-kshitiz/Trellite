package com.example.trellite.service;

import com.example.trellite.dto.*;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskService {

    private final TaskRepo taskRepo;
    private final ListRepo listRepo;
    private final AuthRepo userRepo;
    private final BoardAccessGuard access;

    public TaskService(TaskRepo taskRepo, ListRepo listRepo, AuthRepo userRepo, BoardAccessGuard access) {
        this.taskRepo = taskRepo;
        this.listRepo = listRepo;
        this.userRepo = userRepo;
        this.access = access;
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

    /**
     * Proves the caller may work on the board and that the list is really on it, then
     * hands back the list. Public so the AI draft endpoint can run the same check before
     * spending a model call.
     */
    public TaskList assertListBelongsToBoard(Integer boardId, Integer listId) {
        access.requireVisibleBoard(boardId);
        TaskList taskList = listRepo.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found"));
        if (!taskList.getBoard().getBoardId().equals(boardId)) {
            throw new ResourceNotFoundException("List does not belong to this board");
        }
        return taskList;
    }

    /** The same check, extended one link down the chain to the task itself. */
    private Task requireTask(Integer boardId, Integer listId, Integer taskId) {
        assertListBelongsToBoard(boardId, listId);
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        if (!task.getTaskList().getListId().equals(listId)) {
            throw new ResourceNotFoundException("Task does not belong to this list");
        }
        return task;
    }

    public List<TaskResponseDTO> getTasks(Integer boardId, Integer listId) {
        assertListBelongsToBoard(boardId, listId);
        return taskRepo.findByTaskList_ListId(listId).stream()
                .map(this::mapToTaskResponseDTO)
                .collect(Collectors.toList());
    }

    public TaskResponseDTO createTask(Integer boardId, Integer listId, TaskCreateDTO taskCreateDTO) {
        TaskList taskList = assertListBelongsToBoard(boardId, listId);

        Task task = Task.builder()
                .title(taskCreateDTO.title())
                .taskDescription(taskCreateDTO.taskDescription())
                .status(TaskStatus.TODO)
                // priority is NOT NULL in the DB; a body that omits it used to reach
                // Postgres and come back as a 500.
                .priority(taskCreateDTO.priority() != null ? taskCreateDTO.priority() : Priority.MEDIUM)
                .startDate(taskCreateDTO.startDate())
                .deadline(taskCreateDTO.deadline())
                .taskList(taskList)
                .build();

        return mapToTaskResponseDTO(taskRepo.save(task));
    }

    public TaskResponseDTO getTask(Integer boardId, Integer listId, Integer taskId) {
        return mapToTaskResponseDTO(requireTask(boardId, listId, taskId));
    }

    public TaskResponseDTO updateTask(Integer boardId, Integer listId, Integer taskId, TaskUpdateDTO taskUpdateDTO) {
        Task task = requireTask(boardId, listId, taskId);

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
        taskRepo.delete(requireTask(boardId, listId, taskId));
    }

    public TaskResponseDTO moveTask(Integer boardId, Integer listId, Integer taskId, MoveTaskDTO taskMoveDTO) {
        Task task = requireTask(boardId, listId, taskId);

        TaskList targetList = listRepo.findById(taskMoveDTO.listId())
                .orElseThrow(() -> new ResourceNotFoundException("Target list not found"));

        // A card cannot leave its board. Without this a task could be moved onto a list
        // on someone else's board, taking it out of everyone's reach.
        if (!targetList.getBoard().getBoardId().equals(boardId)) {
            throw new ForbiddenException("A task can only be moved between lists on the same board");
        }

        task.setTaskList(targetList);

        return mapToTaskResponseDTO(taskRepo.save(task));
    }

    /**
     * Assigns a card to someone on the board, or clears the assignee when assigneeId is
     * null. This used to take a bare task id with no checks at all: any authenticated
     * user could assign any task on any board to anyone.
     */
    public TaskResponseDTO assignTask(Integer boardId, Integer listId, Integer taskId, AssignTaskDTO assignTaskDTO) {
        Task task = requireTask(boardId, listId, taskId);

        if (assignTaskDTO.assigneeId() == null) {
            task.setAssignee(null);
            return mapToTaskResponseDTO(taskRepo.save(task));
        }

        User assignee = userRepo.findById(assignTaskDTO.assigneeId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Board board = access.findBoard(boardId);
        if (!access.canAccess(board, assignee)) {
            throw new ForbiddenException(
                    assignee.getUsername() + " is not on this board — add them as a member first");
        }

        task.setAssignee(assignee);
        return mapToTaskResponseDTO(taskRepo.save(task));
    }
}
