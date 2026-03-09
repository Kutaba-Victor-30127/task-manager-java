package ro.kutaba.taskmanager.storage;

import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;

@Component
public class FileAuditLogger implements AuditLogger{
    private final Path logPath;

    public FileAuditLogger(Path logPath){
        this.logPath = logPath;
    }

    public void log(String message){
        try {
            Path parent = logPath.getParent();
            if (parent != null) Files.createDirectories(parent);

            String line = LocalDateTime.now() + " | " + message + System.lineSeparator();

            Files.writeString(
                logPath,
                line,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND
            );
        }catch (IOException e){
            //nu omoram aplicatia pentru logg
            System.err.println("Audit log failed: " + e.getMessage());
        }
    }
}
