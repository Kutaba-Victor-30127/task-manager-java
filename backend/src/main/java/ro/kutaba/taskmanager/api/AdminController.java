package ro.kutaba.taskmanager.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.kutaba.taskmanager.api.dto.PageResponse;
import ro.kutaba.taskmanager.api.dto.TaskResponse;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.service.TaskService;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Administrative task management endpoints")
public class AdminController {

    private final TaskService service;

    public AdminController(TaskService service) {
        this.service = service;
    }

    @Operation(summary = "List tasks across all users")
    @GetMapping("/all-tasks")
    public PageResponse<TaskResponse> getAllTasks(
        @RequestParam(defaultValue = "") String text,
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "5") int size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return service.getAllTasks(page, size, resolveSortField(sortBy, sort), direction, text, status);
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
