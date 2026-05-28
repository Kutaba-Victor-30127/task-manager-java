package ro.kutaba.taskmanager.api.error;

import ro.kutaba.taskmanager.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    // 404 - Task not found
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(TaskNotFoundException ex) {
        ApiError body = new ApiError(
                LocalDateTime.now(),
                404,
                "Not Found",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // 400 - Username deja existent
    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleDuplicateUser(UsernameAlreadyExistsException ex){
        ApiError body = new ApiError(
            LocalDateTime.now(),
            400,
            "Bad Request",
            ex.getMessage()
        );
        return ResponseEntity.badRequest().body(body);
    }

    // 400 - Validation @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        ValidationError body = new ValidationError(
            LocalDateTime.now(),
            400,
            "Bad Request",
            "Validation failed",
            fieldErrors
        );

        return ResponseEntity.badRequest().body(body);
    }

    // 400 - Business rules (deadline, status etc)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
        ApiError body = new ApiError(
                LocalDateTime.now(),
                400,
                "Bad Request",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 500 - fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {

        log.error("Unexpected error", ex);

        ApiError body = new ApiError(
                LocalDateTime.now(),
                500,
                "Internal Server Error",
                "Unexpected error"
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

}
