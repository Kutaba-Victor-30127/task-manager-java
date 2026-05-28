package ro.kutaba.taskmanager.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import ro.kutaba.taskmanager.api.dto.*;
import ro.kutaba.taskmanager.api.error.TaskNotFoundException;
import ro.kutaba.taskmanager.mapper.TaskMapper;
import ro.kutaba.taskmanager.model.Task;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.service.TaskService;


@RestController
@RequestMapping("/api/tasks")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tasks", description = "CRUD + query pentru task-uri")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    // PAGINATION + FILTER + SORT
    @Operation(summary = "Lista task-urilor cu paginare")
    @GetMapping("/tasks")
    public PageResponse<TaskResponse> getTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,  
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "") String text,
            @RequestParam(required = false) TaskStatus status
    ) {
        return service.getTasksFiltered(page, size, sortBy, direction, text, status);
    }

    @Operation(summary = "Ia task dupa id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ok"),
            @ApiResponse(responseCode = "404", description = "Task inexistent")
    })
    @GetMapping("/{id}")
    public TaskResponse byId(@PathVariable Integer id) {
        Task t = service.findByIdForCurrentUser(id);
        if (t == null) throw new TaskNotFoundException(id);
        return TaskMapper.toResponse(t);
    }

    @Operation(summary = "Creeaza un task nou")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creat"),
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

    @Operation(summary = "Update complet task")
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

    @Operation(summary = "Update status task")
    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(@PathVariable Integer id,
                                     @RequestParam TaskStatus status) {

        boolean ok = service.updateStatusForUser(id, status);
        if (!ok) throw new TaskNotFoundException(id);

        return TaskMapper.toResponse(service.findByIdForCurrentUser(id));
    }

    @Operation(summary = "Sterge task")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        boolean ok = service.deleteTask(id);
        if (!ok) throw new TaskNotFoundException(id);
    }

}