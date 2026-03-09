package ro.kutaba.taskmanager.ui;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ro.kutaba.taskmanager.service.TaskService;

@Component
@Profile("console")
public class ConsoleRunner implements CommandLineRunner {

    private final TaskService service;

    public ConsoleRunner(TaskService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        ConsoleMenu ui = new ConsoleMenu(service);
        ui.start();
    }
}