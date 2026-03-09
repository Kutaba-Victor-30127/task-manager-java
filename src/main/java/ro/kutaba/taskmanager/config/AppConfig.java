package ro.kutaba.taskmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ro.kutaba.taskmanager.service.TaskService;
import ro.kutaba.taskmanager.storage.AuditLogger;
import ro.kutaba.taskmanager.storage.TaskRepository;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class AppConfig{

    @Bean
    public TaskService taskService(TaskRepository repo, AuditLogger audit){
        return new TaskService(repo, audit);
    }

    @Bean
    public Path auditLogPath() {
        return Paths.get("audit.log");
    }
}