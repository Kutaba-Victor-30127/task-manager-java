package ro.kutaba.taskmanager.service;

import ro.kutaba.taskmanager.model.Task;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.storage.AuditLogger;
import ro.kutaba.taskmanager.storage.TaskRepository;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class TaskService {

    private final TaskRepository repo;
    private final AuditLogger audit;

    private final Deque<UndoAction> undoStack = new ArrayDeque<>();
    private final Deque<UndoAction> redoStack = new ArrayDeque<>();

    public enum SortBy {
        ID, PRIORITY, DEADLINE, TITLE, STATUS, ESTIMATED_MINUTES
    }

    private static final class UndoAction {
        private final String type;
        private final List<Task> before;
        private final List<Task> after;

        private UndoAction(String type, List<Task> before, List<Task> after) {
            this.type = type;
            this.before = before;
            this.after = after;
        }
    }

    public TaskService(TaskRepository repo, AuditLogger audit) {
        if (repo == null) throw new IllegalArgumentException("repo nu poate fi null");
        if (audit == null) throw new IllegalArgumentException("audit nu poate fi null");

        this.repo = repo;
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
        List<Task> before = snapshot();

        validateDeadline(deadline);
        validateEstimatedMinutes(estimatedMinutes);

        Task task = new Task(
                0,
                title,
                description,
                priority,
                deadline,
                status,
                estimatedMinutes
        );

        Task saved = repo.save(task);

        audit.log("CREATE | " + fmt(saved));
        recordNewAction("CREATE", before, snapshot());

        return saved;
    }

    public List<Task> getAllTasks() {
        return repo.findAll();
    }

    public Task findById(Integer id) {
        if (id == null) return null;
        return repo.findById(id).orElse(null);
    }

    public boolean deleteById(Integer id) {
        if (id == null) return false;

        List<Task> before = snapshot();

        Task existing = repo.findById(id).orElse(null);
        if (existing == null) return false;

        boolean deleted = repo.deleteById(id);

        if (deleted) {
            audit.log("DELETE | " + fmt(existing));
            recordNewAction("DELETE", before, snapshot());
        }

        return deleted;
    }

    public boolean updateStatus(Integer id, TaskStatus newStatus) {
        if (id == null) return false;

        List<Task> beforeSnapshot = snapshot();

        Task task = repo.findById(id).orElse(null);
        if (task == null) return false;

        TaskStatus current = task.getStatus();

        if (!canTransition(current, newStatus)) {
            throw new IllegalArgumentException(
                    "Tranzitie status invalida: " + current + " -> " + newStatus
            );
        }

        Task before = copyOf(task);

        task.setStatus(newStatus);
        repo.save(task);

        audit.log("STATUS | BEFORE: " + fmt(before) + " | AFTER: " + fmt(task));
        recordNewAction("STATUS", beforeSnapshot, snapshot());

        return true;
    }

    public boolean updateTaskFull(
            Integer id,
            String title,
            String description,
            int priority,
            LocalDate deadline,
            TaskStatus status,
            int estimatedMinutes
    ) {
        if (id == null) return false;

        List<Task> beforeSnapshot = snapshot();

        Task task = repo.findById(id).orElse(null);
        if (task == null) return false;

        validateDeadline(deadline);
        validateEstimatedMinutes(estimatedMinutes);

        if (task.getStatus() != status && !canTransition(task.getStatus(), status)) {
            throw new IllegalArgumentException(
                    "Tranzitie status invalida: " + task.getStatus() + " -> " + status
            );
        }

        Task before = copyOf(task);

        task.setTitle(title);
        task.setDescription(description);
        task.setPriority(priority);
        task.setDeadline(deadline);
        task.setStatus(status);
        task.setEstimatedMinutes(estimatedMinutes);

        repo.save(task);

        audit.log("EDIT FULL | BEFORE: " + fmt(before) + " | AFTER: " + fmt(task));
        recordNewAction("EDIT FULL", beforeSnapshot, snapshot());

        return true;
    }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;

        UndoAction action = undoStack.pop();

        restoreSnapshot(action.before);
        redoStack.push(action);

        audit.log("UNDO | type=" + action.type);

        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) return false;

        UndoAction action = redoStack.pop();

        restoreSnapshot(action.after);
        undoStack.push(action);

        audit.log("REDO | type=" + action.type);

        return true;
    }

    public List<Task> searchByTitleOrDescription(String query) {
        String q = normalize(query);
        if (q.isEmpty()) return List.of();

        List<Task> result = new ArrayList<>();

        for (Task task : repo.findAll()) {
            String title = normalize(task.getTitle());
            String description = normalize(task.getDescription());

            if (title.contains(q) || description.contains(q)) {
                result.add(task);
            }
        }

        return result;
    }

    public List<Task> filterByStatus(TaskStatus status) {
        if (status == null) return List.of();

        List<Task> result = new ArrayList<>();

        for (Task task : repo.findAll()) {
            if (task.getStatus() == status) {
                result.add(task);
            }
        }

        return result;
    }

    public List<Task> sortByPriorityDescThenDeadlineAsc() {
        List<Task> tasks = repo.findAll();

        tasks.sort(
                Comparator.comparingInt(Task::getPriority).reversed()
                        .thenComparing(Task::getDeadline)
                        .thenComparing(Task::getId)
        );

        return tasks;
    }

    public List<Task> sortByDeadlineAscThenPriorityDesc() {
        List<Task> tasks = repo.findAll();

        tasks.sort(
                Comparator.comparing(Task::getDeadline)
                        .thenComparing(Comparator.comparingInt(Task::getPriority).reversed())
                        .thenComparing(Task::getId)
        );

        return tasks;
    }

    public List<Task> queryTasks(TaskQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("query nu poate fi null");
        }

        if (query.page() < 1) {
            throw new IllegalArgumentException("page trebuie >= 1");
        }

        if (query.pageSize() < 1 || query.pageSize() > 100) {
            throw new IllegalArgumentException("pageSize trebuie intre 1 si 100");
        }

        String text = normalize(query.textQuery());

        List<Task> filtered = new ArrayList<>();

        for (Task task : repo.findAll()) {
            if (query.statusFilter() != null && task.getStatus() != query.statusFilter()) {
                continue;
            }

            if (!text.isEmpty()) {
                String title = normalize(task.getTitle());
                String description = normalize(task.getDescription());

                if (!title.contains(text) && !description.contains(text)) {
                    continue;
                }
            }

            filtered.add(task);
        }

        SortBy sortBy = query.sortBy() == null ? SortBy.ID : query.sortBy();

        Comparator<Task> comparator = switch (sortBy) {
            case ID -> Comparator.comparingInt(Task::getId);
            case PRIORITY -> Comparator.comparingInt(Task::getPriority);
            case DEADLINE -> Comparator.comparing(Task::getDeadline);
            case TITLE -> Comparator.comparing(task -> normalize(task.getTitle()));
            case STATUS -> Comparator.comparing(task -> task.getStatus().name());
            case ESTIMATED_MINUTES -> Comparator.comparingInt(Task::getEstimatedMinutes);
        };

        comparator = comparator.thenComparingInt(Task::getId);

        if (query.desc()) {
            comparator = comparator.reversed();
        }

        filtered.sort(comparator);

        int from = (query.page() - 1) * query.pageSize();

        if (from >= filtered.size()) {
            return List.of();
        }

        int to = Math.min(from + query.pageSize(), filtered.size());

        return new ArrayList<>(filtered.subList(from, to));
    }

    public int countTasks(TaskStatus statusFilter, String textQuery) {
        String q = normalize(textQuery);
        int count = 0;

        for (Task task : repo.findAll()) {
            if (statusFilter != null && task.getStatus() != statusFilter) {
                continue;
            }

            if (!q.isEmpty()) {
                String title = normalize(task.getTitle());
                String description = normalize(task.getDescription());

                if (!title.contains(q) && !description.contains(q)) {
                    continue;
                }
            }

            count++;
        }

        return count;
    }

    public Map<TaskStatus, Integer> countByStatus() {
        EnumMap<TaskStatus, Integer> counts = new EnumMap<>(TaskStatus.class);

        for (TaskStatus status : TaskStatus.values()) {
            counts.put(status, 0);
        }

        for (Task task : repo.findAll()) {
            counts.put(task.getStatus(), counts.get(task.getStatus()) + 1);
        }

        return counts;
    }

    public Task getMostUrgentTask() {
        Task best = null;

        for (Task task : repo.findAll()) {
            if (task.getStatus() == TaskStatus.DONE) {
                continue;
            }

            if (best == null || task.getDeadline().isBefore(best.getDeadline())) {
                best = task;
            }
        }

        return best;
    }

    public Task getLeastUrgentTask() {
        Task worst = null;

        for (Task task : repo.findAll()) {
            if (task.getStatus() == TaskStatus.DONE) {
                continue;
            }

            if (worst == null || task.getDeadline().isAfter(worst.getDeadline())) {
                worst = task;
            }
        }

        return worst;
    }

    public double getAverageEstimatedMinutes() {
        List<Task> tasks = repo.findAll();

        if (tasks.isEmpty()) return 0.0;

        long sum = 0;

        for (Task task : tasks) {
            sum += task.getEstimatedMinutes();
        }

        return (double) sum / tasks.size();
    }

    public List<Task> getTopUrgent(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit trebuie >= 1");
        }

        List<Task> result = new ArrayList<>();

        for (Task task : repo.findAll()) {
            if (task.getStatus() == TaskStatus.TODO ||
                    task.getStatus() == TaskStatus.IN_PROGRESS) {
                result.add(task);
            }
        }

        result.sort(
                Comparator.comparing(Task::getDeadline)
                        .thenComparing(Comparator.comparingInt(Task::getPriority).reversed())
                        .thenComparingInt(Task::getId)
        );

        if (result.size() <= limit) {
            return result;
        }

        return new ArrayList<>(result.subList(0, limit));
    }

    public Map<TaskStatus, Integer> overdueCountByStatus(StatsMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode nu poate fi null");
        }

        EnumMap<TaskStatus, Integer> map = new EnumMap<>(TaskStatus.class);

        for (TaskStatus status : TaskStatus.values()) {
            map.put(status, 0);
        }

        LocalDate today = LocalDate.now();

        for (Task task : repo.findAll()) {
            if (!mode.allowed().contains(task.getStatus())) {
                continue;
            }

            boolean overdue =
                    task.getDeadline().isBefore(today) &&
                            task.getStatus() != TaskStatus.DONE;

            if (overdue) {
                map.put(task.getStatus(), map.get(task.getStatus()) + 1);
            }
        }

        return map;
    }

    public Map<TaskStatus, List<Task>> topUrgentPerStatus(StatsMode mode, int limit) {
        if (mode == null) {
            throw new IllegalArgumentException("mode nu poate fi null");
        }

        if (limit < 1) {
            throw new IllegalArgumentException("limit trebuie >= 1");
        }

        EnumMap<TaskStatus, List<Task>> result = new EnumMap<>(TaskStatus.class);

        for (TaskStatus status : TaskStatus.values()) {
            result.put(status, new ArrayList<>());
        }

        for (Task task : repo.findAll()) {
            if (!mode.allowed().contains(task.getStatus())) {
                continue;
            }

            result.get(task.getStatus()).add(task);
        }

        Comparator<Task> comparator =
                Comparator.comparing(Task::getDeadline)
                        .thenComparing(Comparator.comparingInt(Task::getPriority).reversed())
                        .thenComparingInt(Task::getId);

        for (TaskStatus status : TaskStatus.values()) {
            List<Task> list = result.get(status);
            list.sort(comparator);

            if (list.size() > limit) {
                result.put(status, new ArrayList<>(list.subList(0, limit)));
            }
        }

        return result;
    }

    private void recordNewAction(String type, List<Task> before, List<Task> after) {
        undoStack.push(new UndoAction(type, before, after));
        redoStack.clear();
    }

    private List<Task> snapshot() {
        List<Task> copies = new ArrayList<>();

        for (Task task : repo.findAll()) {
            copies.add(copyOf(task));
        }

        return copies;
    }

    private void restoreSnapshot(List<Task> snapshot) {
        for (Task task : new ArrayList<>(repo.findAll())) {
            repo.deleteById(task.getId());
        }

        for (Task task : snapshot) {
            repo.save(copyOf(task));
        }
    }

    private static Task copyOf(Task task) {
        return new Task(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getDeadline(),
                task.getStatus(),
                task.getEstimatedMinutes()
        );
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase();
    }

    private static boolean canTransition(TaskStatus from, TaskStatus to) {
        if (from == null || to == null) return false;
        if (from == to) return true;

        return switch (from) {
            case TODO -> to == TaskStatus.IN_PROGRESS || to == TaskStatus.BLOCKED;
            case IN_PROGRESS -> to == TaskStatus.TODO || to == TaskStatus.BLOCKED || to == TaskStatus.DONE;
            case BLOCKED -> to == TaskStatus.TODO || to == TaskStatus.IN_PROGRESS;
            case DONE -> false;
        };
    }

    private static void validateDeadline(LocalDate deadline) {
        if (deadline == null) {
            throw new IllegalArgumentException("Deadline nu poate fi null");
        }

        if (deadline.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Deadline nu poate fi in trecut");
        }
    }

    private static void validateEstimatedMinutes(int estimatedMinutes) {
        if (estimatedMinutes < 1) {
            throw new IllegalArgumentException("Timpul estimat trebuie sa fie >= 1 minut");
        }

        if (estimatedMinutes > 10_000) {
            throw new IllegalArgumentException("Timp estimat nerealist");
        }
    }

    private String fmt(Task task) {
        return "id=" + task.getId()
                + " title=\"" + task.getTitle() + "\""
                + " description=\"" + task.getDescription() + "\""
                + " priority=" + task.getPriority()
                + " deadline=" + task.getDeadline()
                + " status=" + task.getStatus()
                + " est=" + task.getEstimatedMinutes();
    }
}