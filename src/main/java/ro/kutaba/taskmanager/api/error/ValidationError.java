package ro.kutaba.taskmanager.api.error;

import java.time.LocalDateTime;
import java.util.Map;

public record ValidationError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fields
) {}