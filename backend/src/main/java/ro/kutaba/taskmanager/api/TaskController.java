package ro.kutaba.taskmanager.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ro.kutaba.taskmanager.api.dto.CreateTaskRequest;
import ro.kutaba.taskmanager.api.dto.PageResponse;
import ro.kutaba.taskmanager.api.dto.TaskResponse;
import ro.kutaba.taskmanager.api.dto.UpdateTaskRequest;
import ro.kutaba.taskmanager.api.error.TaskNotFoundException;
import ro.kutaba.taskmanager.mapper.TaskMapper;
import ro.kutaba.taskmanager.model.Task;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.service.TaskService;


@RestController
@RequestMapping("/api/tasks")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tasks", description = "Task CRUD operations, filtering, and pagination")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @Operation(summary = "List tasks for the authenticated user")
    @GetMapping
    public PageResponse<TaskResponse> getTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "") String text,
            @RequestParam(required = false) TaskStatus status
    ) {
        return service.getTasksFiltered(page, size, resolveSortField(sortBy, sort), direction, text, status);
    }

    @Operation(summary = "Get a task by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task found"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @GetMapping("/{id}")
    public TaskResponse byId(@PathVariable Integer id) {
        Task task = service.findByIdForCurrentUser(id);
        return TaskMapper.toResponse(task);
    }

    @Operation(summary = "Create a new task")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Task created"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody CreateTaskRequest req) {
        Task t = service.createTask(
                req.title(),
                req.description(),
                req.priority(),
                req.deadline(),
                req.status(),
                req.estimatedMinutes()
        );
        return TaskMapper.toResponse(t);
    }

    @Operation(summary = "Update a task")
    @PutMapping("/{id}")
    public TaskResponse updateFull(@PathVariable Integer id,
                                   @Valid @RequestBody UpdateTaskRequest req) {

        boolean ok = service.updateTaskFullForUser(
                id,
                req.title(),
                req.description(),
                req.priority(),
                req.deadline(),
                req.status(),
                req.estimatedMinutes()
        );

        if (!ok) throw new TaskNotFoundException(id);
        return TaskMapper.toResponse(service.findByIdForCurrentUser(id));
    }

    @Operation(summary = "Update task status")
    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(@PathVariable Integer id,
                                     @RequestParam TaskStatus status) {

        boolean ok = service.updateStatusForUser(id, status);
        if (!ok) throw new TaskNotFoundException(id);

        return TaskMapper.toResponse(service.findByIdForCurrentUser(id));
    }

    @Operation(summary = "Delete a task")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        boolean ok = service.deleteTask(id);
        if (!ok) throw new TaskNotFoundException(id);
    }

    private String resolveSortField(String sortBy, String sort) {
        if (sortBy != null && !sortBy.isBlank()) {
            return sortBy;
        }
        if (sort != null && !sort.isBlank()) {
            return sort;
        }
        return "id";
    }

}
