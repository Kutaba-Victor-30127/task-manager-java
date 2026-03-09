package ro.kutaba.taskmanager.storage.db;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskJpaRepository extends JpaRepository<TaskEntity, Integer>{
    
}