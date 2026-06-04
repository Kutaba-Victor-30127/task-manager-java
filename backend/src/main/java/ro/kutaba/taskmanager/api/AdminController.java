package ro.kutaba.taskmanager.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.kutaba.taskmanager.model.Task;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.service.TaskService;
import org.springframework.data.domain.Page;
import ro.kutaba.taskmanager.api.dto.PageResponse;
import ro.kutaba.taskmanager.api.dto.TaskResponse;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import java.util.List;

import javax.print.DocFlavor.STRING;


@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final TaskService service;

    public AdminController(TaskService service) {
        this.service = service;
    }

    @GetMapping("/all-tasks")
    public PageResponse<TaskResponse> getAllTasks(
        @RequestParam(defaultValue = "") String text,
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "5") int size,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ){
        Sort.Direction dir = direction.equalsIgnoreCase("desc")
         ? Sort.Direction.DESC 
         : Sort.Direction.ASC;
         
        Pageable pageable = PageRequest.of(
            page, 
            size,   
            Sort.by(dir, sort)
        );

        return service.getAllTasks(page, size, sort, direction, text, status);

    }
}
