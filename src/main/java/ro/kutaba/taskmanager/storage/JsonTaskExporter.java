package ro.kutaba.taskmanager.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ro.kutaba.taskmanager.model.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JsonTaskExporter {

    private final ObjectMapper mapper;

    public JsonTaskExporter() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void exportToFile(List<Task> tasks, Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);

            List<TaskJson> dto = new ArrayList<>();
            for (Task t : tasks) {
                dto.add(new TaskJson(
                        t.getId(),
                        t.getTitle(),
                        t.getDescription(),
                        t.getPriority(),
                        t.getDeadline(),
                        t.getStatus(),
                        t.getEstimatedMinutes()
                ));
            }

            mapper.writeValue(path.toFile(), dto);
        } catch (IOException e) {
            throw new RuntimeException("Export JSON a esuat: " + e.getMessage(), e);
        }
    }
}
