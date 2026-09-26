package de.nidal.taskboard.testsupport;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import de.nidal.taskboard.task.TaskRepository;
import de.nidal.taskboard.user.UserRepository;

@RestController
@Profile("test")
public class TestResetController {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TestResetController(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/test/reset")
    public ResponseEntity<Void> reset() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
        return ResponseEntity.noContent().build();
    }
}