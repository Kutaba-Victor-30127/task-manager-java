package ro.kutaba.taskmanager;

import ro.kutaba.taskmanager.service.TaskService;
import ro.kutaba.taskmanager.storage.FileAuditLogger;
import ro.kutaba.taskmanager.storage.JsonTaskRepository;
import ro.kutaba.taskmanager.ui.ConsoleMenu;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        TaskService service = new TaskService(
                new JsonTaskRepository(Path.of("data/tasks.json")),
                new FileAuditLogger(Path.of("data/audit.log"))
        );

        ConsoleMenu menu = new ConsoleMenu(service);
        menu.start();
    }
}