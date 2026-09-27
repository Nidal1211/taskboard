package de.nidal.taskboard.task;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByOwnerIdOrderByIdAsc(Long ownerId);

    List<Task> findByOwnerIdAndStatusOrderByIdAsc(Long ownerId, TaskStatus status);

    List<Task> findByOwnerIdAndTitleContainingIgnoreCaseOrderByIdAsc(Long ownerId, String search);

    List<Task> findByOwnerIdAndStatusAndTitleContainingIgnoreCaseOrderByIdAsc(
            Long ownerId, TaskStatus status, String search);

    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);
}