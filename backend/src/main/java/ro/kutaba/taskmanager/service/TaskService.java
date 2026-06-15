package ro.kutaba.taskmanager.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ro.kutaba.taskmanager.api.dto.PageResponse;
import ro.kutaba.taskmanager.api.dto.TaskResponse;
import ro.kutaba.taskmanager.api.error.TaskNotFoundException;
import ro.kutaba.taskmanager.mapper.TaskMapper;
import ro.kutaba.taskmanager.model.Task;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.model.User;
import ro.kutaba.taskmanager.repository.UserRepository;
import ro.kutaba.taskmanager.security.SecurityUtils;
import ro.kutaba.taskmanager.storage.AuditLogger;
import ro.kutaba.taskmanager.storage.TaskRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository repo;
    private final UserRepository userRepository;
    private final AuditLogger audit;

    public TaskService(TaskRepository repo, UserRepository userRepository, AuditLogger audit) {
        if (repo == null) throw new IllegalArgumentException("TaskRepository must not be null");
        if (userRepository == null) throw new IllegalArgumentException("UserRepository must not be null");
        if (audit == null) throw new IllegalArgumentException("AuditLogger must not be null");

        this.repo = repo;
        this.userRepository = userRepository;
        this.audit = audit;
    }

    public Task createTask(
            String title,
            String description,
            int priority,
            LocalDate deadline,
            TaskStatus status,
            int estimatedMinutes
    ) {
        if (deadline == null) {
            throw new IllegalArgumentException("Deadline must not be null");
        }
        if (deadline.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Deadline must not be in the past");
        }
        if (estimatedMinutes > 10_000) {
            throw new IllegalArgumentException("Estimated time is too large");
        }

        User user = getCurrentUser();
        Task task = new Task(null, title, description, priority, deadline, status, estimatedMinutes);
        Task savedTask = repo.save(task, user);

        audit.log("TASK_CREATED | id=" + savedTask.getId() + " | user=" + user.getUsername() + " | status=" + savedTask.getStatus());
        return savedTask;
    }

    public Page<Task> findAllForAdmin(Pageable pageable) {
        return repo.findAll(pageable);
    }

    public List<Task> getTasksForCurrentUser() {
        return repo.findByUsername(SecurityUtils.getCurrentUsername());
    }

    public Task findByIdForCurrentUser(Integer id) {
        return repo.findByIdAndUsername(id, SecurityUtils.getCurrentUsername())
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    public boolean deleteTaskForCurrentUser(Integer id) {
        audit.log("TASK_DELETE_REQUEST | id=" + id + " | user=" + SecurityUtils.getCurrentUsername());
        return repo.deleteByIdAndUsername(id, SecurityUtils.getCurrentUsername());
    }

    public boolean deleteTask(Integer id) {
        String username = SecurityUtils.getCurrentUsername();
        String role = SecurityUtils.getCurrentRole();

        if ("ROLE_ADMIN".equals(role)) {
            boolean deleted = repo.deleteById(id);
            if (deleted) {
                audit.log("TASK_DELETED | id=" + id + " | user=" + username + " | scope=ADMIN");
            }
            return deleted;
        }

        boolean deleted = repo.deleteByIdAndUsername(id, username);
        if (deleted) {
            audit.log("TASK_DELETED | id=" + id + " | user=" + username + " | scope=OWNER");
        }
        return deleted;
    }

    public boolean updateStatusForUser(Integer id, TaskStatus status) {
        String username = SecurityUtils.getCurrentUsername();
        Task task = repo.findByIdAndUsername(id, username).orElse(null);

        if (task == null) {
            return false;
        }
        if (!canTransition(task.getStatus(), status)) {
            throw new IllegalArgumentException("Invalid status transition: " + task.getStatus() + " -> " + status);
        }

        task.setStatus(status);
        repo.save(task, getCurrentUser());
        audit.log("TASK_STATUS_UPDATED | id=" + id + " | user=" + username + " | status=" + status);
        return true;
    }

    public boolean updateTaskFullForUser(
            Integer id,
            String title,
            String description,
            int priority,
            LocalDate deadline,
            TaskStatus status,
            int estimatedMinutes
    ) {
        String username = SecurityUtils.getCurrentUsername();
        Task task = repo.findByIdAndUsername(id, username).orElse(null);

        if (task == null) {
            return false;
        }
        if (deadline == null) {
            throw new IllegalArgumentException("Deadline must not be null");
        }
        if (deadline.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Deadline must not be in the past");
        }

        task.setTitle(title);
        task.setDescription(description);
        task.setPriority(priority);
        task.setDeadline(deadline);
        task.setStatus(status);
        task.setEstimatedMinutes(estimatedMinutes);

        repo.save(task, getCurrentUser());
        audit.log("TASK_UPDATED | id=" + id + " | user=" + username);
        return true;
    }

    public PageResponse<TaskResponse> getTasksFiltered(
            int page,
            int size,
            String sortBy,
            String direction,
            String text,
            TaskStatus status
    ) {
        if (page < 0) page = 0;
        if (size <= 0) size = 5;

        List<String> allowedSortFields = List.of("id", "title", "priority", "deadline");
        if (!allowedSortFields.contains(sortBy)) {
            sortBy = "id";
        }

        User user = getCurrentUser();
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

        Page<Task> result;
        if (status != null && text != null && !text.isEmpty()) {
            result = repo.findByUserAndTitleContainingAndStatus(user, text, status, pageable);
        } else if (status != null) {
            result = repo.findByUserAndStatus(user, status, pageable);
        } else if (text != null && !text.isEmpty()) {
            result = repo.findByUserAndTitleContaining(user, text, pageable);
        } else {
            result = repo.findByUser(user, pageable);
        }

        return mapToPageResponse(result);
    }

    public PageResponse<TaskResponse> getAllTasks(
            int page,
            int size,
            String sortBy,
            String direction,
            String text,
            TaskStatus status
    ) {
        if (page < 0) page = 0;
        if (size <= 0) size = 5;

        List<String> allowedSortFields = List.of("id", "title", "priority", "deadline");
        if (!allowedSortFields.contains(sortBy)) {
            sortBy = "id";
        }

        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

        Page<Task> result;
        if (status != null && text != null && !text.isEmpty()) {
            result = repo.findByTitleContainingAndStatus(text, status, pageable);
        } else if (status != null) {
            result = repo.findByStatus(status, pageable);
        } else if (text != null && !text.isEmpty()) {
            result = repo.findByTitleContaining(text, pageable);
        } else {
            result = repo.findAll(pageable);
        }

        return mapToPageResponse(result);
    }

    private PageResponse<TaskResponse> mapToPageResponse(Page<Task> page) {
        List<TaskResponse> mapped = page.getContent().stream()
                .map(TaskMapper::toResponse)
                .toList();

        return new PageResponse<>(
                mapped,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private User getCurrentUser() {
        return userRepository.findByUsername(SecurityUtils.getCurrentUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found"));
    }

    private static boolean canTransition(TaskStatus from, TaskStatus to) {
        if (from == null || to == null) return false;
        if (from == to) return true;

        return switch (from) {
            case TODO -> (to == TaskStatus.IN_PROGRESS || to == TaskStatus.BLOCKED);
            case IN_PROGRESS -> (to == TaskStatus.TODO || to == TaskStatus.BLOCKED || to == TaskStatus.DONE);
            case BLOCKED -> (to == TaskStatus.TODO || to == TaskStatus.IN_PROGRESS);
            case DONE -> false;
        };
    }
}
