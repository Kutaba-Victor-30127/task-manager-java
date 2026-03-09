package ro.kutaba.taskmanager.service;

import ro.kutaba.taskmanager.model.Task;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.storage.TaskRepository;
import ro.kutaba.taskmanager.storage.AuditLogger;
import ro.kutaba.taskmanager.service.TaskQuery;
import ro.kutaba.taskmanager.service.StatsMode;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Service
public class TaskService {

    private final List<Task> tasks = new ArrayList<>();
    private final TaskRepository repo;
    private final AuditLogger audit;

    private final Deque<UndoAction> undoStack = new ArrayDeque<>();
    private final Deque<UndoAction> redoStack = new ArrayDeque<>();

    private enum UndoType { ADD, DELETE, EDIT }

    public enum SortBy {
    ID, PRIORITY, DEADLINE, TITLE, STATUS, ESTIMATED_MINUTES
    }

    private static class UndoAction{
        private final UndoType type;
        private final Task before;
        private final Task after;
        private final int index;

        private UndoAction(UndoType type, Task before, Task after, int index){
            this.type = type;
            this.before = before;
            this.after = after;
            this.index = index;
        }

        static UndoAction forAdd(Task added, int index){
            return new UndoAction(UndoType.ADD,null,copyOf(added),index);
        }

        static UndoAction forDelete(Task deleted, int index){
            return new UndoAction(UndoType.DELETE,copyOf(deleted),null,index);
        }

        static UndoAction forEdit(Task before, Task after){
            return new UndoAction(UndoType.EDIT,copyOf(before),copyOf(after),-1);
        }
    }

    private static Task copyOf(Task t){
        return new Task(
            t.getId(),
            t.getTitle(),
            t.getDescription(),
            t.getPriority(),
            t.getDeadline(),
            t.getStatus(),
            t.getEstimatedMinutes()
        );
    }

    public TaskService(TaskRepository repo,AuditLogger audit){
        if (repo == null) throw new IllegalArgumentException("repo nu poate fi null");
        if (audit == null) throw new IllegalArgumentException("audit nu poate fi null");
        this.repo = repo;
        this.audit = audit;

        this.tasks.addAll(repo.findAll()); //load la start 
        initNextIdFromExisting();
    }
    
    private int nextId = 1;

    private int generateId(){
        return nextId++;
    }

    private void initNextIdFromExisting() {
    int max = 0;
    for (Task t : tasks) {
        if (t.getId() > max) {
            max = t.getId();
        }
    }
    nextId = max + 1;
}

    private void recordNewAction(UndoAction action){
        undoStack.push(action);
        redoStack.clear();
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

            int id = generateId();
            Task t = new Task(id, title, description, priority, deadline, status, estimatedMinutes);

            tasks.add(t);
            recordNewAction(UndoAction.forAdd(t, tasks.size() - 1));
            audit.log("CREATE | " + fmt(t));
            repo.save(t);

            return t;
            }

    //

    // CRUD
    void addTask(Task task) {
        if (task == null) throw new IllegalArgumentException("task nu poate fi null");
        if (existsId(task.getId()))
            throw new IllegalArgumentException("ID deja existent: " + task.getId());
        tasks.add(task);
        repo.save(task);
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }


    public Task findById(int id) {
        for (Task t : tasks) {
            if (t.getId() == id) return t;
        }
        return null;
    }

    public boolean deleteById(int id) {
        Task t = findById(id);
        if (t == null) return false;

        int index = tasks.indexOf(t);
        recordNewAction(UndoAction.forDelete(t,index));
        audit.log("DELETE | " + fmt(t));
        boolean removed = tasks.remove(t);
        if (removed) repo.deleteById(id);
        return removed;
    }
    //4
    public boolean updateStatus(int id, TaskStatus newStatus) {
        Task t = findById(id);
        if (t == null) return false;
        TaskStatus curent = t.getStatus();
        if (!canTransition(curent,newStatus)){
            throw new IllegalArgumentException("Tranzitie status invalida: " +curent+ "-> " +newStatus);
        }
        Task before = copyOf(t);
        t.setStatus(newStatus);
        Task after = copyOf(t);
        recordNewAction(UndoAction.forEdit(before,after));
        audit.log("STATUS | id=" + id + " " + curent + " -> " +newStatus);
        repo.save(t);
        return true;
    }
    //7
    public List<Task> searchByTitleOrDescription(String query) {
        String q = normalize(query);
        if (q.isEmpty()) return List.of();

        List<Task> result = new ArrayList<>();
        for (Task t : tasks) {
            String title = normalize(t.getTitle());
            String desc = normalize(t.getDescription());
            if (title.contains(q) || desc.contains(q)) result.add(t);
        }
        return result;
    }
    //8
    public List<Task> filterByStatus(TaskStatus status) {
        if (status == null) return List.of();

        List<Task> result = new ArrayList<>();
        for (Task t : tasks) {
            if (t.getStatus() == status) result.add(t);
        }
        return result;
    }
    //5
    public void sortByPriorityDescThenDeadlineAsc() {
        tasks.sort(
            Comparator.comparingInt(Task::getPriority).reversed()
                      .thenComparing(Task::getDeadline)
                      .thenComparingInt(Task::getId)
        );
        
    }
    //6
    public void sortByDeadlineAscThenPriorityDesc() {
        tasks.sort(
            Comparator.comparing(Task::getDeadline)
                      .thenComparing(Comparator.comparingInt(Task::getPriority).reversed())
                      .thenComparingInt(Task::getId)
        );
        
    }

    public boolean updateTaskFull(int id,String title,String description,int priority,LocalDate deadline,TaskStatus status,int estimatedMinutes){
        Task t = findById(id);
        if (t == null) return false;

         // BUSINESS RULES (aici!)
        if (deadline == null) throw new IllegalArgumentException("Deadline nu poate fi null");
        if (deadline.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Deadline nu poate fi in trecut");
        }

        Task before = copyOf(t);

        t.setTitle(title);
        t.setDescription(description);
        t.setPriority(priority);
        t.setDeadline(deadline);
        //check status transition
       // TaskStatus current = t.getStatus();
       /* if (!canTransition(current, status)) {
            throw new IllegalArgumentException("Tranzitie status invalida: " + current + " -> " + status);
        }*/
        t.setStatus(status);
        t.setEstimatedMinutes(estimatedMinutes);

        Task after = copyOf(t);
        audit.log("EDIT FULL | id=" + id +" | BEFORE: " + fmt(before) +" | AFTER: " +fmt(after));
        recordNewAction(UndoAction.forEdit(before,after));

        repo.save(after);
        return true;
    }

    public boolean undo(){
        if (undoStack.isEmpty()) return false;
        UndoAction a = undoStack.pop();

        switch (a.type){
            case ADD ->{
                //undo add = remove added task
                Task added = a.after;
                Task existing = findById(added.getId());
                if (existing != null) tasks.remove(existing);
            }
            case DELETE ->{
                //undo delete = add back to the same index
                Task deleted = a.before;
                if (!existsId(deleted.getId())){
                    int idx = Math.max(0,Math.min(a.index, tasks.size()));
                    tasks.add(idx,copyOf(deleted));
                }
            }
            case EDIT ->{
                // undo edit = restore "before"
                Task before = a.before;
                Task current = findById(before.getId());
                if (current == null) return false;

                current.setTitle(before.getTitle());
                current.setDescription(before.getDescription());
                current.setPriority(before.getPriority());
                current.setDeadline(before.getDeadline());
                current.setStatus(before.getStatus());
                current.setEstimatedMinutes(before.getEstimatedMinutes());
                }
            }
            redoStack.push(a);
            audit.log("UNDO | type=" +a.type+ " | " + (a.after != null ? fmt(a.after) : fmt(a.before)));
            //save();
            return true;
        }

    public boolean redo(){
        if (redoStack.isEmpty()) return false;
        
        UndoAction a = redoStack.pop();

        switch(a.type){
            case ADD ->{
                Task added = a.after;
                if (!existsId(added.getId())){
                    int idx = Math.max(0,Math.min(a.index,tasks.size()));
                    tasks.add(idx,copyOf(added));
                }
            }
            case DELETE ->{
                Task deleted = a.before;
                Task existing = findById(deleted.getId());
                if (existing != null) tasks.remove(existing);
            }
            case EDIT ->{
                Task after = a.after;
                Task current = findById(after.getId());
                if (current == null) return false;

                current.setTitle(after.getTitle());
                current.setDescription(after.getDescription());
                current.setPriority(after.getPriority());
                current.setDeadline(after.getDeadline());
                current.setStatus(after.getStatus());
                current.setEstimatedMinutes(after.getEstimatedMinutes());
            }
        }
        undoStack.push(a);
        audit.log("REDO | type=" + a.type + " | " + (a.after != null ? fmt(a.after) : fmt(a.before)));
        //save();
        return true;
    }

    public List<Task> queryTasks(TaskQuery q){

        if (q.page() < 1) throw new IllegalArgumentException("page trebuie >= 1");
        if (q.pageSize() < 1 || q.pageSize() > 100) throw new IllegalArgumentException("pageSize intre 1 si 100");

        String text = normalize(q.textQuery());

        // 1) filtrare
        List<Task> filtered = new ArrayList<>();
        for (Task t : tasks){
            if (q.statusFilter() != null && t.getStatus() != q.statusFilter()) continue;

            if (!text.isEmpty()){
                String title = normalize(t.getTitle());
                String descText = normalize(t.getDescription());
                if (!title.contains(text) && !descText.contains(text)) continue;
            }
            filtered.add(t);
        }

            // sortare
        SortBy sortBy = (q.sortBy() == null) ? SortBy.ID : q.sortBy();
        Comparator<Task> cmp = switch(sortBy){
            case ID -> Comparator.comparingInt(Task::getId);
            case PRIORITY -> Comparator.comparingInt(Task::getPriority);
            case DEADLINE -> Comparator.comparing(Task::getDeadline);
            case TITLE -> Comparator.comparing(t -> normalize(t.getTitle()));
            case STATUS -> Comparator.comparing(t -> t.getStatus().name());
            case ESTIMATED_MINUTES -> Comparator.comparingInt(Task::getEstimatedMinutes);
        }; 

        cmp = cmp.thenComparingInt(Task::getId);

        if (q.desc()) cmp = cmp.reversed();

        filtered.sort(cmp);

        //paginare
        int from = (q.page() - 1) * q.pageSize();
        if (from >= filtered.size()) return List.of();

        int to = Math.min(from + q.pageSize(), filtered.size());
        return new ArrayList<>(filtered.subList(from,to));
        
        }

    public int countTasks(TaskStatus statusFilter, String textQuery){
        String q = normalize(textQuery);
        int count = 0;

        for (Task t : tasks){
            if (statusFilter != null && t.getStatus() != statusFilter) continue;
            if (!q.isEmpty()){
                String title = normalize(t.getTitle());
                String desc = normalize(t.getDescription());
                if (!title.contains(q) && !desc.contains(q)) continue;
            }
            count++;
        }
        return count;
    }

    public Map<TaskStatus,Integer> countByStatus(){
        EnumMap<TaskStatus, Integer> counts = new EnumMap<>(TaskStatus.class);

        //initializam cu 0 ca sa apara toate statusurile in rezultat
        for(TaskStatus s : TaskStatus.values()){
            counts.put(s, 0);
        }

        for (Task t : tasks){
            counts.put(t.getStatus(), counts.get(t.getStatus()) + 1);
        }
        return counts;
    }

    public Task getMostUrgentTask(){
        Task best = null;
        for (Task t : tasks){
            if (t.getStatus() == TaskStatus.DONE) continue;
            if (best == null || t.getDeadline().isBefore(best.getDeadline())){
                best = t;
            }
        }
        return best; //poate fi null daca toate sunt DONE sau lista este goala
    }

    public Task getLeastUrgentTask() {
        Task worst = null;
        for (Task t : tasks) {
            if (t.getStatus() == TaskStatus.DONE) continue;
            if (worst == null || t.getDeadline().isAfter(worst.getDeadline())) {
                worst = t;
            }
        }
        return worst;
    }

    public double getAverageEstimatedMinutes() {
        if (tasks.isEmpty()) return 0.0;

        long sum = 0;
        for (Task t : tasks) {
            sum += t.getEstimatedMinutes();
        }
        return (double) sum / tasks.size();
    }

    public List<Task> getTopUrgent(int limit){
        if (limit < 1) throw new IllegalArgumentException("limit trebuie >= 1");
        List<Task> list = new ArrayList<>();

        for (Task t : tasks ){
            if (t.getStatus() == TaskStatus.TODO || t.getStatus() == TaskStatus.IN_PROGRESS) 
            list.add(t);
        }

        list.sort(
            Comparator.comparing(Task::getDeadline)
                      .thenComparing(Comparator.comparingInt(Task::getPriority).reversed())
                      .thenComparingInt(Task::getId)
        );

        if (list.size() <= limit) return list;
        return new ArrayList<>(list.subList(0, limit));

    }

    public Map<TaskStatus, Integer> overdueCountByStatus(StatsMode mode){
        EnumMap<TaskStatus, Integer> map = new EnumMap<>(TaskStatus.class);
        for (TaskStatus s : TaskStatus.values()) map.put(s, 0);

        LocalDate today = LocalDate.now();

        for(Task t : tasks){
            if (!mode.allowed().contains(t.getStatus())) continue;

            boolean overdue = t.getDeadline().isBefore(today) && t.getStatus() != TaskStatus.DONE;
            if (overdue) {
                map.put(t.getStatus(), map.get(t.getStatus()) + 1);
            }
        }
        return map;
    }

    public Map<TaskStatus, List<Task>> topUrgentPerStatus(StatsMode mode, int limit){
        if (limit < 1) throw new IllegalArgumentException("limit trebuie >= 1");

        EnumMap<TaskStatus, List<Task>> out = new EnumMap<>(TaskStatus.class);

        for (TaskStatus s : TaskStatus.values()) out.put(s, new ArrayList<>());

        for (Task t : tasks){
            if(!mode.allowed().contains(t.getStatus())) continue;
            out.get(t.getStatus()).add(t);
        }

        Comparator<Task> cmp = Comparator.comparing(Task::getDeadline)
            .thenComparing(Comparator.comparingInt(Task::getPriority).reversed())
            .thenComparingInt(Task::getId);

        for (TaskStatus s : TaskStatus.values()){
            List<Task> list = out.get(s);
            list.sort(cmp);
            if (list.size() > limit){
                out.put(s, new ArrayList<>(list.subList(0,limit)));
            }
        }    
        return out;
    }

    public boolean existsId(int id) {
        return findById(id) != null;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
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

    private String fmt(Task t){
        return "id=" + t.getId()
                + " title\"" + t.getTitle() + "\""
                + " description\"" + t.getDescription() + "\""
                + " priority=" + t.getPriority()
                + " deadline=" + t.getDeadline()
                + " status=" + t.getStatus()
                + " est=" + t.getEstimatedMinutes();
    }

}
