package de.nidal.taskboard.task;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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

    public List<TaskResponse> list(Long userId) {
        return taskRepository.findByOwnerIdOrderByIdAsc(userId).stream()
                .map(TaskResponse::from)
                .toList();
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
}