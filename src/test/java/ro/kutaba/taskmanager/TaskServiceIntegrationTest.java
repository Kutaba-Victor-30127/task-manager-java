package ro.kutaba.taskmanager.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ro.kutaba.taskmanager.model.Task;
import ro.kutaba.taskmanager.model.TaskStatus;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TaskServiceIntegrationTest {

    @Autowired
    private TaskService service;

    @Test
    void createTask_shouldWorkWithSpringContext(){

        Task t = service.createTask(
            "Spring Task",
            "desc",
            5,
            LocalDate.now().plusDays(1),
            TaskStatus.TODO,
            10
        );

        assertNotNull(t); 
    }
}