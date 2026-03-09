package ro.kutaba.taskmanager.ui;

import ro.kutaba.taskmanager.model.Task;
import ro.kutaba.taskmanager.storage.JsonTaskExporter;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.service.TaskService;
import ro.kutaba.taskmanager.service.TaskQuery;
import ro.kutaba.taskmanager.service.StatsMode;
import ro.kutaba.taskmanager.service.AuditStatusAnalytics;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import java.util.Map;
import java.util.function.ToLongFunction;


public class ConsoleMenu {

    private final TaskService service;
    
    private final Scanner sc = new Scanner(System.in);

    public ConsoleMenu(TaskService service) {
    this.service = service;
    }

    public void start() {
        
        while (true) {
            printMenu();
            int opt = readIntInRange("Alege optiunea: ", 0, 14);

            switch (opt) {
                case 0 -> {
                    System.out.println("La revedere!");
                    return;
                }
                case 1 -> handleAddTask();
                case 2 -> printTasks(service.getAllTasks());
                case 3 -> handleDeleteTask();
                case 4 -> handleEditStatus();
                case 5 -> {
                    service.sortByPriorityDescThenDeadlineAsc();
                    printTasks(service.getAllTasks());
                }
                case 6 -> {
                    service.sortByDeadlineAscThenPriorityDesc();
                    printTasks(service.getAllTasks());
                }
                case 7 -> handleSearch();
                case 8 -> handleFilterByStatus();
                case 9 -> handleEditTaskFull();
                case 10 -> handleUndo();
                case 11 -> handleRedo();
                case 12 -> handleExportJson();
                case 13 -> handleQueryTasks();
                case 14 -> handleAdvancedStats();
                default -> System.out.println("Optiune invalida.");
            }
        }
    }

    private void printMenu() {
        System.out.println("""
                
                === TASK MANAGER ===
                1. Adauga Task
                2. Afisare Task-uri
                3. Sterge Task
                4. Schimba Status Task
                5. Sorteaza dupa Prioritate
                6. Sorteaza dupa Deadline
                7. Cauta dupa titlu/descriere
                8. Afiseaza Task-urile dupa Status
                9. Editeaza Task (complet)
                10. Undo ultima actiune.
                11. Redo ultima actiune.
                12. Export JSON.
                13. Filtru + sort + paginare (query)
                14. Statistici Avansate.
                0. Iesire
                """);
    }

    // ===== Handlers =====

    private void handleAddTask() {
        System.out.println("\n--- Adauga Task ---");
        
        String title = readNonEmptyLine("Titlu: ");
        String description = readLine("Descriere: ");
        int priority = readIntInRange("Prioritate (1-5): ", 1, 5);
        LocalDate deadline = readDate("Deadline (YYYY-MM-DD): ");
        TaskStatus status = readStatus("Status (TODO, IN_PROGRESS, BLOCKED, DONE): ");
        int est = readPositiveInt("Timp estimat (minute): ");

        try {
            Task t = service.createTask(
                title,
                description,
                priority,
                deadline,
                status,
                est
                );
                 System.out.println("Task adaugat cu succes! Id = " +t.getId());
        } catch (IllegalArgumentException e){
             System.out.println("Eroare: " +e.getMessage());
        }
        
    }

    private void handleDeleteTask() {
        System.out.println("\n--- Sterge Task ---");
        int id = readPositiveInt("ID de sters: ");
        Task t = service.findById(id);

        if (t == null) {
            System.out.println("Nu exista task cu id dat");
            return;
        }
        System.out.println("Task de sters: ");
        System.out.println(t);

        String confirm = readLineAllowEmpty("Siguri vrei sa stergi?(y/n): ").trim().toLowerCase();
        if (!(confirm.equals("y") || confirm.equals("yes") || confirm.equals("da"))){
            System.out.println("Stergere anulata");
            return;
        }
        boolean ok = service.deleteById(id);
        if (ok) {
            System.out.println("Task sters");
        } else {
            System.out.println("Eroare: taskul nu a putut fi sters.");
        }

    }

    private void handleEditStatus() {
        System.out.println("\n--- Schimba Status Task ---");
        int id = readPositiveInt("ID: ");

        Task t = service.findById(id);
        if (t == null){
            System.out.println("Nu exista task cu id-ul dat");
            return;
        }
        System.out.println("Taskul curent:");
        System.out.println(t);

        TaskStatus newStatus = readStatus("Status nou (TODO, IN_PROGRESS, BLOCKED, DONE): ");

        System.out.println("\nVrei sa schimbi status-ul din " + t.getStatus() + " in " + newStatus + "?");

        String confirm = readLineAllowEmpty("Confirmi? (y/n): ").trim().toLowerCase();
        if (!(confirm.equals("y") || confirm.equals("yes") || confirm.equals("da"))){
            System.out.println("Schimbare anulata.");
            return;
        }
        boolean ok = service.updateStatus(id,newStatus);
        if (ok){
            System.out.println("Status editat");
        } else {
            System.out.println("Eroare: tranzitie status nepermisa.");
        }
    }

    private void handleSearch() {
        System.out.println("\n--- Cauta Task-uri ---");
        String q = readLine("Cauta dupa titlu/descriere: ");
        List<Task> results = service.searchByTitleOrDescription(q);
        if (results.isEmpty()) {
            System.out.println("Nu a fost gasit niciun task care sa contina \"" + q + "\".");
        } else {
            printTasks(results);
        }
    }

    private void handleFilterByStatus() {
        System.out.println("\n--- Task-uri dupa Status ---");
        TaskStatus status = readStatus("Status (TODO, IN_PROGRESS, BLOCKED, DONE): ");
        List<Task> results = service.filterByStatus(status);
        if (results.isEmpty()) {
            System.out.println("Nu exista task-uri cu status " + status);
        } else {
            printTasks(results);
        }
    }

   private void handleEditTaskFull() {
    System.out.println("\n--- Editare Task (complet pro) ---");
    int id = readPositiveInt("Id task de editat: ");

    Task t = service.findById(id);
    if (t == null) {
        System.out.println("Nu exista task cu ID-ul dat");
        return;
    }

    System.out.println("Task curent: ");
    System.out.println(t);
    System.out.println("\nENTER = pastrezi valoarea curenta.");
    System.out.println("La descriere: '-' = sterge descrierea.\n");
 
    Task draft = t.copy();

    // 1) Title
    while (true) {
        String input = readLineAllowEmpty("Titlu nou [" + draft.getTitle() + "]: ");
        if (input.isBlank()) break; // keep
        try {
            draft.setTitle(input); // setter-ul tau face trim
            break;
        } catch (IllegalArgumentException e) {
            System.out.println("Titlu invalid: " + e.getMessage());
        }
    }

    // 2) Description
    while (true) {
        String input = readLineAllowEmpty("Descriere noua ['-' sterge] [" + draft.getDescription() + "]: ");
        if (input.isBlank()) break; // keep
        if (input.trim().equals("-")) {
            draft.setDescription("");
            break;
        }
        try {
            draft.setDescription(input); // setter-ul tau face trim + null->""
            break;
        } catch (IllegalArgumentException e) {
            System.out.println("Descriere invalida: " + e.getMessage());
        }
    }

    // 3) Priority
    while (true) {
        String input = readLineAllowEmpty("Prioritate noua (1-5) [" + draft.getPriority() + "]: ");
        if (input.isBlank()) break; // keep
        try {
            int p = Integer.parseInt(input.trim());
            draft.setPriority(p);
            break;
        } catch (NumberFormatException e) {
            System.out.println("Introdu doar cifre (ex: 3).");
        } catch (IllegalArgumentException e) {
            System.out.println("Prioritate invalida: " + e.getMessage());
        }
    }

    // 4) Deadline
    while (true) {
        String input = readLineAllowEmpty("Deadline nou YYYY-MM-DD [" + draft.getDeadline() + "]: ");
        if (input.isBlank()) break; // keep
        try {
            LocalDate d = LocalDate.parse(input.trim());
            draft.setDeadline(d);
            break;
        } catch (DateTimeParseException e) {
            System.out.println("Data invalida. Format: YYYY-MM-DD (ex: 2026-01-22)");
        } catch (IllegalArgumentException e) {
            System.out.println("Deadline invalid: " + e.getMessage());
        }
    }

    // 5) Status
    while (true) {
        String input = readLineAllowEmpty("Status nou [" + draft.getStatus() + "]: ");
        if (input.isBlank()) break; // keep
        try {
            TaskStatus s = TaskStatus.valueOf(input.trim().toUpperCase());
            draft.setStatus(s);
            break;
        } catch (IllegalArgumentException e) {
            System.out.println("Status invalid. Exemple: TODO, IN_PROGRESS, BLOCKED, DONE");
        }
    }

    // 6) Estimated minutes
    while (true) {
        String input = readLineAllowEmpty("Timp estimat nou (minute) [" + draft.getEstimatedMinutes() + "]: ");
        if (input.isBlank()) break; // keep
        try {
            int est = Integer.parseInt(input.trim());
            draft.setEstimatedMinutes(est);
            break;
        } catch (NumberFormatException e) {
            System.out.println("Introdu doar cifre (ex: 60).");
        } catch (IllegalArgumentException e) {
            System.out.println("Timp invalid: " + e.getMessage());
        }
    }

    System.out.println("\n--- PREVIEW MODIFICARI ---");
    System.out.println("Inainte:");
    System.out.println(t);

    System.out.println("\nDupa:");
    System.out.println(draft);

    String confirm = readLineAllowEmpty("Salvezi modificarile? (y/n): ").trim().toLowerCase();

    boolean okConfirm = confirm.equals("y") || confirm.equals("yes") || confirm.equals("da");
    if (!okConfirm){
        System.out.println("Modificari anulate.");
        return;
        }

    boolean ok = service.updateTaskFull(
                id,
                draft.getTitle(),
                draft.getDescription(),
                draft.getPriority(),
                draft.getDeadline(),
                draft.getStatus(),
                draft.getEstimatedMinutes()
            );
    
   if (ok) System.out.println("Modificari salvate cu succes!");
    else System.out.println("Eroare: task-ul nu a putut fi modificat.");
   
   }

   private void handleUndo(){
        System.out.println("\n---Undo---");
        boolean ok = service.undo();
        System.out.println(ok ? "Undo ok." : "Nu mai ai la ce face undo"); 
   }

   private void handleRedo(){
        System.out.println("\n---Redo---");
        boolean ok = service.redo();
        System.out.println(ok ? "Redo ok." : "Nu mai ai la ce face redo");
   }

    private void handleExportJson(){
        System.out.println("\n---Export Json---");

        try{
            List<Task> tasks = service.getAllTasks();

            if (tasks.isEmpty()){
                System.out.println("Nu exista taskuri pentru export");
                return;
            }

            JsonTaskExporter exporter = new JsonTaskExporter();
            Path path = Paths.get("data/tasks_export.json");

            exporter.exportToFile(tasks, path);

            System.out.println("Export realizat cu succes!");
            System.out.println("Fisier generat: " + path.toAbsolutePath());
        }catch (Exception e){
            System.out.println("Eroare la export: " + e.getMessage());
        }
    }

    private void handleQueryTasks(){
        System.out.println("\n--- Query (filtru + sort + paginare) ---");

        //status filter
        String statusInput = readLineAllowEmpty("Filtru status (TODO/IN_PROGRESS/BLOCKED/DONE sau ENTER=toate): ").trim();
        TaskStatus status = null;

        if (!statusInput.isBlank()){
            try{
                status = TaskStatus.valueOf(statusInput.toUpperCase());
            }catch (IllegalArgumentException e){
                System.out.println("Status invalid");
                return;
            }
        }

        // text search
        String text = readLineAllowEmpty("Filtru status (TODO/IN_PROGRESS/BLOCKED/DONE sau ENTER=toate): ");

        // sort by
        System.out.println("Sort by: ID, PRIORITY, DEADLINE, TITLE, STATUS, ESTIMATED_MINUTES");
        String sortInput = readNonEmptyLine("Sort by: ").trim().toUpperCase();

        TaskService.SortBy sortBy;
        try{
            sortBy = TaskService.SortBy.valueOf(sortInput);
        }catch (IllegalArgumentException e){
            System.out.println("Sort invalid");
            return;
        }

        //direction
        String dir = readLineAllowEmpty("Directie (asc/desc) [asc]: ").trim().toLowerCase();
        boolean desc = dir.equals("desc");

        int pageSize = readIntInRange("Page size (1-100): [10]",1,100);
        int page = readPositiveInt("Page (>=1): ");

        int total = service.countTasks(status,text);
        int totalPages = (int) Math.ceil(total / (double) pageSize);

        TaskQuery query = new TaskQuery(status, text, sortBy, desc, page, pageSize);
        List<Task> pageItems = service.queryTasks(query);

        System.out.println("\n Total rezultate: " + total + "| Pagini: " + totalPages + "| Pagina curenta: " + page);
        if (pageItems.isEmpty()){
            System.out.println("Nimic de afisat pe pagina asta");
            return;
        }

        printTasks(pageItems);
    }

    private void handleAdvancedStats() {
        System.out.println("\n--- Statistici Avansate ---");

        // 0) Mode
        StatsMode mode = readStatsMode("Mod statistici (active/all) [active]: ");

        // 1) Count by status (filtrat după mode)
        System.out.println("\n[1] Count by status (" + mode + "):");
        var counts = service.countByStatus();

        int shownTotal = 0;
        for (TaskStatus s : TaskStatus.values()) {
            if (!mode.allowed().contains(s)) continue;
            shownTotal += counts.getOrDefault(s, 0);
        }

        System.out.println("Total in mode: " + shownTotal);
        for (TaskStatus s : TaskStatus.values()) {
            if (!mode.allowed().contains(s)) continue;
            int c = counts.getOrDefault(s, 0);
            double pct = shownTotal == 0 ? 0.0 : (c * 100.0 / shownTotal);
            System.out.printf("%-12s : %d (%.1f%%)%n", s, c, pct);
        }

        // 2) Overdue count per status
        System.out.println("\n[2] Overdue count by status (" + mode + "):");
        var overdue = service.overdueCountByStatus(mode);

        int overdueTotal = 0;
        for (TaskStatus s : TaskStatus.values()) {
            if (!mode.allowed().contains(s)) continue;
            overdueTotal += overdue.getOrDefault(s, 0);
        }
        System.out.println("Total overdue: " + overdueTotal);

        for (TaskStatus s : TaskStatus.values()) {
            if (!mode.allowed().contains(s)) continue;
            int c = overdue.getOrDefault(s, 0);
            System.out.printf("%-12s : %d%n", s, c);
        }

        // 3) Top urgent per status
        int limit = readIntInRange("\n[3] Cate task-uri in Top Urgente per status? (1-10): ", 1, 10);
        var topMap = service.topUrgentPerStatus(mode, limit);

        System.out.println("\nTop " + limit + " urgente per status (" + mode + "):");
        for (TaskStatus s : TaskStatus.values()) {
            if (!mode.allowed().contains(s)) continue;

            List<Task> list = topMap.getOrDefault(s, List.of());
            System.out.println("\n== " + s + " ==");
            if (list.isEmpty()) {
                System.out.println("(nimic de afisat)");
            } else {
                for (Task t : list) {
                    System.out.printf(" - #%d | %s | prio=%d | deadline=%s | est=%dmin%n",
                            t.getId(),
                            t.getTitle(),
                            t.getPriority(),
                            t.getDeadline(),
                            t.getEstimatedMinutes()
                    );
                }
            }
        }

        // 4) Average minutes per status (din audit.log)
        System.out.println("\n[4] Average minutes per status (from audit.log) (" + mode + "):");

        var res = AuditStatusAnalytics.averageMinutesPerStatus(Path.of("data/audit.log"), mode);

        for (TaskStatus s : TaskStatus.values()) {
            if (!mode.allowed().contains(s)) continue;

            double avgMin = res.avgByStatus().getOrDefault(s, 0.0);
            long totalMin = res.totalMinutesByStatus().getOrDefault(s, 0L);
            long samples = res.samplesByStatus().getOrDefault(s, 0L);

            System.out.printf("%-12s avg=%.1f min | total=%d min | segments=%d%n",
                    s, avgMin, totalMin, samples);
        }

        System.out.println("\n Bottleneck summary:");

        TaskStatus worstAvg = argMaxStatus(
            s -> Math.round(res.avgByStatus().getOrDefault(s, 0.0)),
            mode
        );

        TaskStatus worstTotal = argMaxStatus(
            s -> res.totalMinutesByStatus().getOrDefault(s, 0L),
            mode
        );

        TaskStatus worstOverdue = argMaxStatus(
            s -> overdue.getOrDefault(s, 0),
            mode
        );

        System.out.printf("• Max AVG time    : %s (%.1f min)%n",
                worstAvg,
                res.avgByStatus().getOrDefault(worstAvg, 0.0)
        );

        System.out.printf("• Max TOTAL time  : %s (%d min)%n",
                worstTotal,
                res.totalMinutesByStatus().getOrDefault(worstTotal, 0L)
        );

        System.out.printf("• Most OVERDUE    : %s (%d tasks)%n",
                worstOverdue,
                overdue.getOrDefault(worstOverdue, 0)
        );

        if (worstOverdue != null && overdue.getOrDefault(worstOverdue, 0) > 0) {
            System.out.println("Recomandare: incepe cu task-urile overdue din " + worstOverdue + ".");
        }
        if (worstAvg != null && res.avgByStatus().getOrDefault(worstAvg, 0.0) > 0) {
            System.out.println("Recomandare: investigheaza de ce task-urile stau mult in " + worstAvg + ".");
        }
        // 5) Extra (global)
        System.out.println("\n[5] Extra (global):");
        Task mostUrgent = service.getMostUrgentTask();
        Task leastUrgent = service.getLeastUrgentTask();

        if (mostUrgent == null) {
            System.out.println("Nu exista task-uri active (toate sunt DONE sau lista e goala).");
        } else {
            System.out.println("Cel mai urgent (global): #" + mostUrgent.getId()
                    + " | " + mostUrgent.getTitle() + " | " + mostUrgent.getDeadline());
            System.out.println("Cel mai putin urgent (global): #" + leastUrgent.getId()
                    + " | " + leastUrgent.getTitle() + " | " + leastUrgent.getDeadline());
        }

        double avgEst = service.getAverageEstimatedMinutes();
        System.out.printf("Media timp estimat (global): %.2f minute%n", avgEst);
        }
    // ===== Read helpers =====

    private int readPositiveInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                int v = Integer.parseInt(line);
                if (v <= 0) {
                    System.out.println("Trebuie > 0.");
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Introdu doar cifre.");
            }
        }
    }

    private int readIntInRange(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                int v = Integer.parseInt(line);
                if (v < min || v > max) {
                    System.out.println("Trebuie intre " + min + " si " + max);
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Introdu doar cifre.");
            }
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private String readNonEmptyLine(String prompt) {
        while (true) {
            String s = readLine(prompt);
            if (s.isEmpty()) {
                System.out.println("Nu poate fi gol.");
                continue;
            }
            return s;
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                return LocalDate.parse(line); // format ISO: YYYY-MM-DD
            } catch (DateTimeParseException e) {
                System.out.println("Data invalida. Format corect: YYYY-MM-DD (ex: 2025-12-19)");
            }
        }
    }

    private TaskStatus readStatus(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim().toUpperCase();
            try {
                return TaskStatus.valueOf(line);
            } catch (IllegalArgumentException e) {
                System.out.println("Status invalid. Exemple: TODO, IN_PROGRESS, BLOCKED, DONE");
            }
        }
    }
    
    private String readLineAllowEmpty(String prompt){
        System.out.print(prompt);
        return sc.nextLine();
    }

    private StatsMode readStatsMode(String prompt){
        while (true){
            String line = readLineAllowEmpty(prompt).trim().toLowerCase();
            if (line.isEmpty() || line.equals("active")) return StatsMode.ACTIVE;
            if (line.equals("all")) return StatsMode.ALL;
            System.out.println("Valoare invalida. Scrie 'active' sau 'all'.");
        }
    }

    private TaskStatus argMaxStatus(ToLongFunction<TaskStatus> scoreFn, StatsMode mode){
        TaskStatus best = null;
        long bestScore = Long.MIN_VALUE;

        for (TaskStatus s : TaskStatus.values()){
            if(!mode.allowed().contains(s)) continue;
            long score = scoreFn.applyAsLong(s);
            if (best == null || score > bestScore){
                best = s;
                bestScore = score;
            }
        }
        return best;
    }

    // ===== Print helper =====

    private void printTasks(List<Task> tasks) {
        System.out.println("\n--- TASKS ---");
        for (Task t : tasks) {
            System.out.println(t);
        }
    }
    
}    