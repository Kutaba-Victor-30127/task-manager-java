package ro.kutaba.taskmanager.service;

import ro.kutaba.taskmanager.api.dto.TaskResponse;
import ro.kutaba.taskmanager.model.Task;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.storage.TaskRepository;
import ro.kutaba.taskmanager.storage.AuditLogger;
import ro.kutaba.taskmanager.repository.UserRepository;
import ro.kutaba.taskmanager.model.User;
import ro.kutaba.taskmanager.security.SecurityUtils;
import ro.kutaba.taskmanager.mapper.TaskMapper;
import ro.kutaba.taskmanager.api.dto.PageResponse;
import ro.kutaba.taskmanager.security.SecurityUtils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;



public class TaskService {

    private final TaskRepository repo;
    private final UserRepository userRepository;
    private final AuditLogger audit;

    public TaskService(TaskRepository repo,UserRepository userRepository, AuditLogger audit){
        if (repo == null) throw new IllegalArgumentException("repo nu poate fi null");
        if (userRepository == null) throw new IllegalArgumentException("UserRepository nu poate fi null");
        if (audit == null) throw new IllegalArgumentException("audit nu poate fi null");
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
            //business rules
            if (deadline == null) throw new IllegalArgumentException("Deadline nu poate fi null"); 
            if (deadline.isBefore(LocalDate.now())){
                throw new IllegalArgumentException("Deadline nu poate fi in trecut");
            }

            if (estimatedMinutes > 10000){
                throw new IllegalArgumentException("Timp estimat nerealist");
            }

            User user = getCurrentUser();

            Task t = new Task(null, title, description, priority, deadline, status, estimatedMinutes);

            audit.log("CREATE | id=" + t.getId() + " | user=" + user.getUsername());
            
            return repo.save(t, user);

            }

    //get all tasks - admin         
    public Page<Task> findAllForAdmin(Pageable pageable){
        return repo.findAll(pageable);
    }
   
    //get tasks per user
    public List<Task> getTasksForCurrentUser(){
        return repo.findByUsername(SecurityUtils.getCurrentUsername());
    }

    //find by id secure
    public Task findByIdForCurrentUser(Integer id){
        return repo.findByIdAndUsername(id, SecurityUtils.getCurrentUsername()).orElseThrow(() -> new RuntimeException("Task cu id " + id + " nu exista pentru utilizatorul curent"));
    }

    //delete secure
    public boolean deleteTaskForCurrentUser(Integer id){
        audit.log("DELETE | id=" + id);
        return repo.deleteByIdAndUsername(id, SecurityUtils.getCurrentUsername());
    }

    //delete admin
    public boolean deleteTask(Integer id){
        String username = SecurityUtils.getCurrentUsername();
        String role = SecurityUtils.getCurrentRole();

        if (role.equals("ROLE_ADMIN")){
            return repo.deleteById(id);//admin poate sterge orice task
        }
        //user normal poate sterge doar taskurile proprii
        return repo.deleteByIdAndUsername(id, username);
        
    }

    //update taskstatus secure
    public boolean updateStatusForUser(Integer id, TaskStatus status) {
        String username = SecurityUtils.getCurrentUsername();
        
        Task t = repo.findByIdAndUsername(id, username).orElse(null);
        if (t == null) return false;

        if (!canTransition(t.getStatus(), status)){
            throw new IllegalArgumentException("Tranzitie status invalida: " + t.getStatus() + " -> " + status);
        }

        t.setStatus(status);
        repo.save(t, getCurrentUser());
        audit.log("UPDATE | id=" + id + " | status=" + status);
        return true;
    }

    //update full secure
    public boolean updateTaskFullForUser(
        Integer id,
        String title,
        String description,
        int priority,
        LocalDate deadline,
        TaskStatus status,
        int estimatedMinutes
    ){
        String username = SecurityUtils.getCurrentUsername();
        
        Task t = repo.findByIdAndUsername(id, username).orElse(null);
        if (t == null) return false;

        if (deadline == null) throw new IllegalArgumentException("Deadline nu poate fi null");
        if (deadline.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Deadline nu poate fi in trecut");
        }

        t.setTitle(title);
        t.setDescription(description);
        t.setPriority(priority);
        t.setDeadline(deadline);
        t.setStatus(status);
        t.setEstimatedMinutes(estimatedMinutes);

        repo.save(t, getCurrentUser());
        audit.log("UPDATE | id=" + id);
        
        return true;
    }

    public PageResponse<TaskResponse> getTasksFiltered(
        int page,
        int size,
        String sortBy,
        String direction,
        String text,
        TaskStatus status
    ){
        if (page < 0) page = 0;
        if (size <= 0) size = 5;

        List<String> allowedSortFields = List.of("id", "title", "priority", "deadline");
        if (!allowedSortFields.contains(sortBy)) {
            sortBy = "id";
        }

        User user = getCurrentUser();     

        Sort.Direction dir = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

        Page<Task> result;

        if (status != null && text != null && !text.isEmpty()){
            result = repo.findByUserAndTitleContainingAndStatus(user, text, status, pageable);
        } else if (status != null){
            result = repo.findByUserAndStatus(user, status, pageable);
        } else if (text != null && !text.isEmpty()){
            result = repo.findByUserAndTitleContaining(user, text, pageable);
        } else {
            result = repo.findByUser(user, pageable);
        }  
        
        return mapToPageResponse(result);

    }

    public PageResponse<TaskResponse> getAllTasks(int page,
        int size,
        String sortBy,
        String direction,
        String text,
        TaskStatus status){
        
        if (page < 0) page = 0;
        if (size <= 0) size = 5;

        List<String> allowedSortFields = List.of("id", "title", "priority", "deadline");
        if (!allowedSortFields.contains(sortBy)) {
            sortBy = "id";
        }
            
        Sort.Direction dir = direction.equalsIgnoreCase("desc") 
            ? Sort.Direction.DESC 
            : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

        Page<Task> result;

        if (status != null && text != null && !text.isEmpty()){
            result = repo.findByTitleContainingAndStatus(text, status, pageable);
        } else if (status != null){
            result = repo.findByStatus(status, pageable);
        } else if (text != null && !text.isEmpty()){
            result = repo.findByTitleContaining(text, pageable);
        } else {
            result = repo.findAll(pageable);
        }
        
        return mapToPageResponse(result);
    }  

    private PageResponse<TaskResponse> mapToPageResponse(Page<Task> page){
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

    private User getCurrentUser(){
        return userRepository.findByUsername(SecurityUtils.getCurrentUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }


    private static boolean canTransition(TaskStatus from, TaskStatus to){
        if(from == null || to == null) return false;
        if(from == to) return true;

        return switch(from) {
            case TODO -> (to == TaskStatus.IN_PROGRESS || to == TaskStatus.BLOCKED);
            case IN_PROGRESS -> (to == TaskStatus.TODO || to == TaskStatus.BLOCKED || to ==TaskStatus.DONE);
            case BLOCKED -> (to == TaskStatus.TODO || to == TaskStatus.IN_PROGRESS);
            case DONE -> false;
        };
    }


}
