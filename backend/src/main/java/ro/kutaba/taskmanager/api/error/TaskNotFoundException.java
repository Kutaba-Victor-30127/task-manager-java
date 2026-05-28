package ro.kutaba.taskmanager.api.error;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(int id) {
        super("Task inexistent: id=" + id);
    }
}