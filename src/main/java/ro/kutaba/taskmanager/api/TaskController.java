package ro.kutaba.taskmanager.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ro.kutaba.taskmanager.api.dto.CreateTaskRequest;
import ro.kutaba.taskmanager.api.error.TaskNotFoundException;
import ro.kutaba.taskmanager.api.dto.QueryRequest;
import ro.kutaba.taskmanager.api.dto.UpdateTaskRequest;
import ro.kutaba.taskmanager.api.dto.TaskResponse;
import ro.kutaba.taskmanager.model.Task;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.service.TaskQuery;
import ro.kutaba.taskmanager.service.TaskService;
import ro.kutaba.taskmanager.mapper.TaskMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "CRUD + query pentru task-uri")
public class TaskController{

    private final TaskService service;

    public TaskController(TaskService service){
        this.service = service;
    }

    @Operation(summary = "Listă toate task-urile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")
    })
    @GetMapping
    public List<TaskResponse> all() {
        return service.getAllTasks()
                    .stream()
                    .map(TaskMapper::toResponse)
                    .toList(); 
    }

     @Operation(summary = "Ia task după id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ok"),
            @ApiResponse(responseCode = "404", description = "Task inexistent")
    })
    @GetMapping("/{id}")
    public TaskResponse byId(@PathVariable int id){
        Task t = service.findById(id);
        if (t == null) throw new TaskNotFoundException(id);
        return TaskMapper.toResponse(t);
    }

    @Operation(summary = "Creează un task nou")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Creat"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody CreateTaskRequest req){
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

    @Operation(summary = "Update complet task (PUT)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Modificat"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PutMapping("/{id}")
    public TaskResponse updateFull(@PathVariable int id, @Valid @RequestBody UpdateTaskRequest req){
        boolean ok = service.updateTaskFull(
            id,
            req.title(),
            req.description(),
            req.priority(),
            req.deadline(),
            req.status(),
            req.estimatedMinutes()
        );

        if (!ok) throw new TaskNotFoundException(id);
        return TaskMapper.toResponse(service.findById(id));
    }

    @Operation(summary = "Update complet task (PUT)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Modificat"),
            @ApiResponse(responseCode = "400", description = "Validare eșuată / task inexistent")
    })
    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(@PathVariable int id, @RequestParam TaskStatus status) {
        boolean ok = service.updateStatus(id, status);
        if (!ok) throw new TaskNotFoundException(id);
        return TaskMapper.toResponse(service.findById(id));
    }

    @Operation(summary = "Update status task (PATCH)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status schimbat"),
            @ApiResponse(responseCode = "400", description = "Status invalid / task inexistent / tranziție nepermisă")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) {
        boolean ok = service.deleteById(id);
        if (!ok) throw new TaskNotFoundException(id);
    }

    @Operation(summary = "Query: filtru + sort + paginare")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Request invalid")
    })
    @PostMapping("/query")
    public List<TaskResponse> query(@RequestBody QueryRequest req) {
        TaskQuery q = new TaskQuery(
                req.statusFilter(),
                req.textQuery(),
                req.sortBy(),
                req.desc(),
                req.page(),
                req.pageSize()
        );
        return service.queryTasks(q)
                    .stream()
                    .map(TaskMapper::toResponse)
                    .toList();
    }
    
}