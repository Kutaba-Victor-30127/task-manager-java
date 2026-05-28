package ro.kutaba.taskmanager.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

public class FileAuditLogger implements AuditLogger {

    private final Path logPath;

    public FileAuditLogger(Path logPath) {
        if (logPath == null) throw new IllegalArgumentException("logPath nu poate fi null");
        this.logPath = logPath;
    }

    @Override
    public void log(String message) {
        try {
            Path parent = logPath.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            String line = LocalDateTime.now() + " | " + message + System.lineSeparator();

            Files.writeString(
                    logPath,
                    line,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

        } catch (IOException e) {
            System.err.println("Audit log failed: " + e.getMessage());
        }
    }
}