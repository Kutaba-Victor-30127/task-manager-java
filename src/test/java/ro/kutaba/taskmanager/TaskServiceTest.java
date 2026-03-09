package ro.kutaba.taskmanager.service;

import org.junit.jupiter.api.Test;
import ro.kutaba.taskmanager.model.Task;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.storage.InMemoryTaskRepository;
import ro.kutaba.taskmanager.storage.TaskRepository;
import ro.kutaba.taskmanager.storage.AuditLogger;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {
    private TaskService service;

    static class NoOpAuditLogger implements AuditLogger{
        public void log(String message){ }
    }
    
    @Test
    void addTask_shouldAddTaskToList(){
        //Arrange in pregatire
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        Task t = service.createTask(
            "Primul task",
            "Descriere",
            3,
            LocalDate.now().plusDays(1),
            TaskStatus.TODO,
            60
        );
        
        //Assert(verificare)
        List<Task> all = service.getAllTasks();
        assertEquals(1,all.size());
        assertEquals(1, all.get(0).getId());
        assertEquals("Primul task", all.get(0).getTitle());
        assertEquals(TaskStatus.TODO, all.get(0).getStatus());
    }

    @Test
    void addTask_duplicateId_shouldThrowExeption(){
        // Arrange
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());
        Task t1 = service.createTask(
            "Primul task",
            "Descriere",
            3,
            LocalDate.now().plusDays(1),
            TaskStatus.TODO,
            60
        );

        Task t2 = service.createTask(
            "Alt task",
            "Alta descriere",
            4,
            LocalDate.now().plusDays(2),
            TaskStatus.IN_PROGRESS,
            30
        );
        
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () ->{
            service.addTask(t2);
        });
    }

    // Delete existent -> true + scade lista 
    @Test
    void deleteById_existing_shouldRemoveTask(){
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        Task t1 = service.createTask("Task A","",3,LocalDate.now().plusDays(1),TaskStatus.TODO,10);
        Task t2 = service.createTask("Task B","",3,LocalDate.now().plusDays(1),TaskStatus.TODO,10);

        boolean ok = service.deleteById(1);
        assertTrue(ok);
        assertEquals(1,service.getAllTasks().size());
        assertNull(service.findById(1));
        assertNotNull(service.findById(2));
    }

    @Test
    void deleteById_missing_shouldReturnFalse(){
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        Task t1 = service.createTask("Task A", "", 3, LocalDate.now().plusDays(1), TaskStatus.TODO, 10);

        boolean ok = service.deleteById(999);
        
        assertFalse(ok);
        assertEquals(1,service.getAllTasks().size());
    }

    @Test
    void updateTaskFull_existing_shouldUpdateFields(){
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        Task t = service.createTask("Old Task", "OldDesc", 2, LocalDate.now().plusDays(5), TaskStatus.TODO, 30);
        int id = t.getId();
        boolean ok = service.updateTaskFull(
            id,
            "New Task",
            "NewDesc",
            5,
            LocalDate.now().plusDays(10),
            TaskStatus.DONE,
            90
            );
        
        assertTrue(ok);
        Task updated = service.findById(id);
        assertNotNull(updated);
        assertEquals("New Task",updated.getTitle());
        assertEquals("NewDesc", updated.getDescription());
        assertEquals(5, updated.getPriority());
        assertEquals(TaskStatus.DONE, updated.getStatus());
        assertEquals(90, updated.getEstimatedMinutes());
    }

    @Test
    void updateTaskFull_missing_shouldReturnFalse(){
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        boolean ok = service.updateTaskFull(
            999,
            "Xxxx",
            "Y",
            3,
            LocalDate.now().plusDays(1),
            TaskStatus.TODO,
            10
        );

        assertFalse(ok);
    }

    @Test
    void undo_afterAdd_shouldRemoveAddedTask() {
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        Task t1 = service.createTask("Task A", "", 3, LocalDate.now().plusDays(1), TaskStatus.TODO, 10);
        Task t2 = service.createTask("Task B", "", 3, LocalDate.now().plusDays(1), TaskStatus.TODO, 10);


        // Ultima acțiune e ADD task 2
        boolean undone = service.undo();

        assertTrue(undone);
        assertNotNull(service.findById(1));
        assertNull(service.findById(2));
        assertEquals(1, service.getAllTasks().size());
    }

    @Test
    void redo_afterUndoAdd_shouldRestoreTask(){
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        Task t1 = service.createTask("Task A", "", 3, LocalDate.now().plusDays(1), TaskStatus.TODO, 10);
        Task t2 = service.createTask("Task B", "", 3, LocalDate.now().plusDays(1), TaskStatus.TODO, 10);

        assertTrue(service.undo()); //sterge taskul t2
        assertTrue(service.redo()); //il aduce inapoi

        assertNotNull(service.findById(2));
        assertEquals(2,service.getAllTasks().size());
    }

    @Test
    void undo_afterUndoDelete_shouldRestoreDeletedTask(){
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        Task t1 = service.createTask("Task A", "", 3, LocalDate.now().plusDays(1), TaskStatus.TODO, 10);
        Task t2 = service.createTask("Task B", "", 3, LocalDate.now().plusDays(1), TaskStatus.TODO, 10);


        assertTrue(service.deleteById(1));
        assertNull(service.findById(1));

        assertTrue(service.undo());
        assertNotNull(service.findById(1));
        assertEquals(2,service.getAllTasks().size());
    }

    @Test
    void redo_afterUndoDelete_shouldDeleteAgain(){
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        // pregătim 2 taskuri ca să verificăm și index-ul (optional)
        Task t1 = service.createTask("Task A", "a", 3, LocalDate.now().plusDays(1), TaskStatus.TODO, 10);
        Task t2 = service.createTask("Task B", "b", 3, LocalDate.now().plusDays(1), TaskStatus.TODO, 10);

        int idToDelete = t1.getId();
        //delete
        assertTrue(service.deleteById(idToDelete));
        assertNull(service.findById(idToDelete));

        //undo
        assertTrue(service.undo());
        assertNotNull(service.findById(idToDelete));

        //redo
        assertTrue(service.redo());
        assertNull(service.findById(idToDelete));

        //t2 ramane
        assertNotNull(service.findById(t2.getId()));
    }

    @Test
    void undo_afterEdit_shouldRestoreOldValues(){
    TaskRepository repo = new InMemoryTaskRepository();
    TaskService service = new TaskService(repo, new NoOpAuditLogger());

    // Seed folosind doar API-ul oficial
    Task seeded = service.createTask(
            "Old Task",
            "Desc",
            2,
            LocalDate.now().plusDays(5),
            TaskStatus.TODO,
            30
    );
    int id = seeded.getId();

    assertTrue(service.updateTaskFull(
        id,
        "New Task",
        "New desc",
        5,
        LocalDate.now().plusDays(10),
        TaskStatus.DONE,
        90
        ));
    Task afterEdit = service.findById(id);
    assertEquals("New Task",afterEdit.getTitle());

    //undo
    assertTrue(service.undo());

    Task afterUndo = service.findById(id);
    assertEquals("Old Task", afterUndo.getTitle());
    assertEquals("Desc", afterUndo.getDescription());
    assertEquals(2, afterUndo.getPriority());
    assertEquals(TaskStatus.TODO, afterUndo.getStatus());
    assertEquals(30, afterUndo.getEstimatedMinutes());
    } 

    @Test
    void redo_afterUndoEdit_shouldReapplyNewValues(){
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        Task seeded = service.createTask(
            "Old Task",
            "Desc",
            2,
            LocalDate.now().plusDays(5),
            TaskStatus.TODO,
            30
        );
        int id = seeded.getId();

        assertTrue(service.updateTaskFull(
            id,
            "New Task",
            "NewDesc",
            5,
            LocalDate.now().plusDays(10),
            TaskStatus.DONE,
            90
        ));

        assertTrue(service.undo());
        assertTrue(service.redo());

        Task t = service.findById(id);
        assertEquals("New Task", t.getTitle());
        assertEquals("NewDesc", t.getDescription());
        assertEquals(5, t.getPriority());
        assertEquals(TaskStatus.DONE, t.getStatus());
        assertEquals(90, t.getEstimatedMinutes());
    }

    @Test
    void undoRedo_chain_shouldReturnToSameState(){
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        //add A
        Task a = service.createTask("Task A","",3,LocalDate.now().plusDays(5),TaskStatus.TODO,10);
        int idA = a.getId();

        //add B
        Task b = service.createTask("Task B", "", 2, LocalDate.now().plusDays(6), TaskStatus.TODO, 20);
        int idB = b.getId();

        //edit B
        assertTrue(service.updateTaskFull(
            idB, "Task B2", "desc", 5, LocalDate.now().plusDays(10), TaskStatus.DONE, 99
        ));
        assertEquals("Task B2", service.findById(idB).getTitle());

        //delete A
        assertTrue(service.deleteById(idA));
        assertNull(service.findById(idA));
        assertNotNull(service.findById(idB));

        //Undo *4
        assertTrue(service.undo());//undo delete A revine
        assertNotNull(service.findById(idA));

        assertTrue(service.undo());//undo la edit B
        assertEquals("Task B", service.findById(idB).getTitle());
        assertEquals(TaskStatus.TODO, service.findById(idB).getStatus());

        assertTrue(service.undo());//uno la add -> B dispare
        assertNull(service.findById(idB));

        assertTrue(service.undo()); // undo ADD A -> A dispare
        assertNull(service.findById(idA));

        assertEquals(0,service.getAllTasks().size());

        //redo *4
        assertTrue(service.redo()); // redo ADD A
        assertNotNull(service.findById(idA));

        assertTrue(service.redo()); // redo ADD B
        assertNotNull(service.findById(idB));
        assertEquals("Task B", service.findById(idB).getTitle());

        assertTrue(service.redo()); // redo EDIT B
        assertEquals("Task B2", service.findById(idB).getTitle());
        assertEquals(TaskStatus.DONE, service.findById(idB).getStatus());

        assertTrue(service.redo()); // redo DELETE A
        assertNull(service.findById(idA));
        assertNotNull(service.findById(idB));

        assertEquals(1, service.getAllTasks().size());
    }

    @Test
    void undo_whenEmpty_shouldReturnFalse() {
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        assertFalse(service.undo());
    }

    @Test
    void redo_whenEmpty_shouldReturnFalse() {
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        assertFalse(service.redo());
    }

    @Test
    void redo_shouldBeCleared_afterNewAction(){
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());
        Task a = service.createTask("Task A","",3,LocalDate.now().plusDays(5),TaskStatus.TODO,30);
        int idA = a.getId();

        assertTrue(service.undo()); // undo la add -> A dispare
        assertNull(service.findById(idA));

        //Actiune noua(ADD) redo trebuie golit
        Task b = service.createTask("Task B","",5,LocalDate.now().plusDays(5),TaskStatus.TODO,40);
        int idB = b.getId();
        assertNotNull(service.findById(idB));

        assertFalse(service.redo());
    }

    @Test
    void createTask_priorityOutOfRange_shouldThrow() {
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        assertThrows(IllegalArgumentException.class, () ->
            service.createTask("Titlu", "desc", 10, LocalDate.now().plusDays(1), TaskStatus.TODO, 10)
        );
    }

    @Test
    void createTask_deadlineInPast_shouldThrow() {
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        assertThrows(IllegalArgumentException.class, () ->
            service.createTask("Titlu", "desc", 3, LocalDate.now().minusDays(1), TaskStatus.TODO, 10)
        );
    }

    @Test
    void createTask_titleTooShort_shouldThrow() {
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        assertThrows(IllegalArgumentException.class, () ->
            service.createTask("A", "desc", 3, LocalDate.now().plusDays(1), TaskStatus.TODO, 10)
        );
    }

    @Test
    void updateStatus_doneToTodo_shouldThrow() {
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        Task t = service.createTask("Xxxx","",3,LocalDate.now().plusDays(1),TaskStatus.DONE,10);

        assertThrows(IllegalArgumentException.class, () ->
            service.updateStatus(t.getId(), TaskStatus.TODO)
        );
    }

    @Test
    void updateStatus_inProgressToTodo_shouldBeAllowed() {
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        Task t = service.createTask("Xxxx","",3,LocalDate.now().plusDays(1),TaskStatus.IN_PROGRESS,10);

        assertTrue(service.updateStatus(t.getId(), TaskStatus.TODO));
        assertEquals(TaskStatus.TODO, service.findById(t.getId()).getStatus());
    }

    @Test 
    void createTask_titleBlank_shouldThrow(){
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        assertThrows(IllegalArgumentException.class, ()->
                    service.createTask("","desc",3,LocalDate.now().plusDays(1),TaskStatus.TODO,10)
                    );
    }

    @Test
    void createTask_estimatedMinutesTooBig_shouldThrow() {
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());
        
        assertThrows(IllegalArgumentException.class, () ->
                service.createTask("Titlu", "desc", 3, LocalDate.now().plusDays(1), TaskStatus.TODO, 20000)
        );
    }

    @Test
    void updateStatus_todoToDone_shouldThrow() {
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        Task t = service.createTask("Task A", "", 3, LocalDate.now().plusDays(1), TaskStatus.TODO, 10);

        assertThrows(IllegalArgumentException.class, () ->
                service.updateStatus(t.getId(), TaskStatus.DONE)
        );
    }

    @Test
    void updateStatus_todoToInProgress_shouldWork() {
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        Task t = service.createTask("Task A", "", 3, LocalDate.now().plusDays(1), TaskStatus.TODO, 10);

        assertTrue(service.updateStatus(t.getId(), TaskStatus.IN_PROGRESS));
        assertEquals(TaskStatus.IN_PROGRESS, service.findById(t.getId()).getStatus());
    }

    @Test
    void undo_afterUpdateStatus_shouldRestoreOldStatus() {
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        Task t = service.createTask("Task A", "", 3, LocalDate.now().plusDays(1), TaskStatus.TODO, 10);
        int id = t.getId();
        assertTrue(service.updateStatus(id, TaskStatus.IN_PROGRESS));
        assertEquals(TaskStatus.IN_PROGRESS, service.findById(id).getStatus());

        assertTrue(service.undo());
        assertEquals(TaskStatus.TODO, service.findById(id).getStatus());
    }

    @Test
    void redo_afterUndoEditStatus_shouldRestoreNewStatus() {
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        Task t = service.createTask("Task A", "", 3, LocalDate.now().plusDays(1), TaskStatus.TODO, 10);
        int id = t.getId();
        assertTrue(service.updateStatus(id, TaskStatus.IN_PROGRESS));
        assertEquals(TaskStatus.IN_PROGRESS, service.findById(id).getStatus());

        assertTrue(service.undo());
        assertEquals(TaskStatus.TODO, service.findById(id).getStatus());

        assertTrue(service.redo());
        assertEquals(TaskStatus.IN_PROGRESS, service.findById(id).getStatus());
    }

    @Test
    void query_filterSortPaginate_shouldWorkCorrectly(){
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        service.createTask("Aaaa", "", 3, LocalDate.now().plusDays(5), TaskStatus.TODO, 10);
        service.createTask("Bbbb", "", 3, LocalDate.now().plusDays(1), TaskStatus.TODO, 10);
        service.createTask("Cccc", "", 3, LocalDate.now().plusDays(3), TaskStatus.DONE, 10);
        service.createTask("Dddd", "", 3, LocalDate.now().plusDays(2), TaskStatus.TODO, 10);

        TaskQuery q = new TaskQuery(
                    TaskStatus.TODO,
                    "",
                    TaskService.SortBy.DEADLINE,
                    false,
                    1,
                    2
                    );

        List<Task> result = service.queryTasks(q);
        assertEquals(2,result.size());
        assertEquals("Bbbb", result.get(0).getTitle()); //cel mai apropiat deadline
        assertEquals("Dddd", result.get(1).getTitle());                                    
    }

    @Test
    void query_textSearch_descSort_shouldWork() {
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());
        
        service.createTask("Alpha task", "desc", 3, LocalDate.now().plusDays(1), TaskStatus.TODO, 10);
        service.createTask("Beta task", "alpha inside", 5, LocalDate.now().plusDays(2), TaskStatus.TODO, 10);
        service.createTask("Gamma", "nothing", 1, LocalDate.now().plusDays(3), TaskStatus.TODO, 10);

        TaskQuery q = new TaskQuery(
                    TaskStatus.TODO,
                    "alpha",
                    TaskService.SortBy.DEADLINE,
                    true,
                    1,
                    2
                    );
        List<Task> result = service.queryTasks(q);
        assertEquals(2, result.size());
        assertTrue(result.get(0).getPriority() >= result.get(1).getPriority());
        }

    @Test
    void query_secondPage_shouldReturnCorrectItems() {
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        for (int i = 1; i <= 5; i++) {
            service.createTask(
                    "Task " + i,
                    "",
                    3,
                    LocalDate.now().plusDays(i),
                    TaskStatus.TODO,
                    10
            );
        }

        TaskQuery q = new TaskQuery(
                    TaskStatus.TODO,
                    "",
                    TaskService.SortBy.DEADLINE,
                    false,
                    2,
                    2
                    );
        List<Task> result = service.queryTasks(q);

        assertEquals(2, result.size());
        assertEquals("Task 3", result.get(0).getTitle());
        assertEquals("Task 4", result.get(1).getTitle());
    }

    @Test
    void countByStatus_shouldReturnAllStatusesEvenIfZero(){
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        var map = service.countByStatus();
        for (TaskStatus s : TaskStatus.values()){
            assertTrue(map.containsKey(s));
            assertEquals(0, map.get(s));
        }
    }

    @Test
    void countByStatus_shouldCountCorrectly() {
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo, new NoOpAuditLogger());

        service.createTask("Aaaa", "", 3, LocalDate.now().plusDays(1), TaskStatus.TODO, 10);
        service.createTask("Bbbb", "", 3, LocalDate.now().plusDays(1), TaskStatus.TODO, 10);
        service.createTask("Cccc", "", 3, LocalDate.now().plusDays(1), TaskStatus.IN_PROGRESS, 10);

        var map = service.countByStatus();

        assertEquals(2, map.get(TaskStatus.TODO));
        assertEquals(1, map.get(TaskStatus.IN_PROGRESS));
        assertEquals(0, map.get(TaskStatus.BLOCKED));
        assertEquals(0, map.get(TaskStatus.DONE));
    }



}