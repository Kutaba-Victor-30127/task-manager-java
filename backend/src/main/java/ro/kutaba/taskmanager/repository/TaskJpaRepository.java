package ro.kutaba.taskmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.kutaba.taskmanager.model.TaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ro.kutaba.taskmanager.model.User;
import ro.kutaba.taskmanager.model.TaskStatus;

import java.util.List;
import java.util.Optional;

public interface TaskJpaRepository extends JpaRepository<TaskEntity, Long>{
    List<TaskEntity> findByUserUsername(String username);

    Optional<TaskEntity> findByIdAndUserUsername(Long id, String username); 

    Page<TaskEntity> findByUser(User user, Pageable pageable);

    Page<TaskEntity> findByUserAndStatus(User user, TaskStatus status, Pageable pageable);

    Page<TaskEntity> findByUserAndTitleContaining(User user, String text, Pageable pageable);

    Page<TaskEntity> findByUserAndTitleContainingAndStatus(User user, String text, TaskStatus status, Pageable pageable);

    Page<TaskEntity> findByTitleContaining(String text, Pageable pageable);

    Page<TaskEntity> findByStatus(TaskStatus status, Pageable pageable);

    Page<TaskEntity> findByTitleContainingAndStatus(String text, TaskStatus status, Pageable pageable);
}