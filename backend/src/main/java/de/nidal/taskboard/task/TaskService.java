package de.nidal.taskboard.task;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.nidal.taskboard.common.ApiException;
import de.nidal.taskboard.user.User;
import de.nidal.taskboard.user.UserRepository;

@Service
public class TaskService {

    private static final int MAX_TITLE_LENGTH = 100;

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    public TaskResponse create(Long userId, CreateTaskRequest request) {
        String title = validateTitle(request.title());
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Bitte melde dich an"));

        Task task = taskRepository.save(new Task(title, owner));
        return TaskResponse.from(task);
    }

    public List<TaskResponse> list(Long userId, String status, String search) {
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasSearch = search != null && !search.isBlank();

        List<Task> tasks;
        if (hasStatus && hasSearch) {
            tasks = taskRepository.findByOwnerIdAndStatusAndTitleContainingIgnoreCaseOrderByIdAsc(
                    userId, parseStatus(status), search);
        } else if (hasStatus) {
            tasks = taskRepository.findByOwnerIdAndStatusOrderByIdAsc(userId, parseStatus(status));
        } else if (hasSearch) {
            tasks = taskRepository.findByOwnerIdAndTitleContainingIgnoreCaseOrderByIdAsc(userId, search);
        } else {
            tasks = taskRepository.findByOwnerIdOrderByIdAsc(userId);
        }

        return tasks.stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional
    public TaskResponse rename(Long userId, Long taskId, UpdateTaskRequest request) {
        Task task = findOwnTask(userId, taskId);
        task.rename(validateTitle(request.title()));
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse changeStatus(Long userId, Long taskId, UpdateStatusRequest request) {
        Task task = findOwnTask(userId, taskId);
        task.changeStatus(parseStatus(request.status()));
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long userId, Long taskId) {
        taskRepository.delete(findOwnTask(userId, taskId));
    }

    private Task findOwnTask(Long userId, Long taskId) {
        return taskRepository.findByIdAndOwnerId(taskId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Aufgabe nicht gefunden"));
    }

    private String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Titel darf nicht leer sein");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Titel darf höchstens 100 Zeichen lang sein");
        }
        return title;
    }

    private TaskStatus parseStatus(String value) {
        if (value == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ungültiger Status");
        }
        try {
            return TaskStatus.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ungültiger Status");
        }
    }
}