package de.nidal.taskboard.task;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByOwnerIdOrderByIdAsc(Long ownerId);

    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);
}