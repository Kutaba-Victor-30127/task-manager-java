package ro.kutaba.taskmanager.ui;

import ro.kutaba.taskmanager.model.Task;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.service.StatsMode;
import ro.kutaba.taskmanager.service.TaskQuery;
import ro.kutaba.taskmanager.service.TaskService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import java.util.Scanner;

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

            try {
                switch (opt) {
                    case 0 -> {
                        System.out.println("La revedere!");
                        return;
                    }
                    case 1 -> handleAddTask();
                    case 2 -> printTasks(service.getAllTasks());
                    case 3 -> handleDeleteTask();
                    case 4 -> handleEditStatus();
                    case 5 -> printTasks(service.sortByPriorityDescThenDeadlineAsc());
                    case 6 -> printTasks(service.sortByDeadlineAscThenPriorityDesc());
                    case 7 -> handleSearch();
                    case 8 -> handleFilterByStatus();
                    case 9 -> handleEditTaskFull();
                    case 10 -> handleUndo();
                    case 11 -> handleRedo();
                    case 12 -> handleQueryTasks();
                    case 13 -> handleBasicStats();
                    case 14 -> handleAdvancedStats();
                    default -> System.out.println("Optiune invalida.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Eroare: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println("""
                
                === TASK MANAGER ===
                1. Adauga Task
                2. Afiseaza Task-uri
                3. Sterge Task
                4. Schimba Status Task
                5. Sorteaza dupa Prioritate desc + Deadline asc
                6. Sorteaza dupa Deadline asc + Prioritate desc
                7. Cauta dupa titlu/descriere
                8. Afiseaza Task-urile dupa Status
                9. Editeaza Task complet
                10. Undo
                11. Redo
                12. Query: filtru + sort + paginare
                13. Statistici basic
                14. Statistici avansate
                0. Iesire
                """);
    }

    private void handleAddTask() {
        System.out.println("\n--- Adauga Task ---");

        String title = readNonEmptyLine("Titlu: ");
        String description = readLineAllowEmpty("Descriere: ");
        int priority = readIntInRange("Prioritate (1-5): ", 1, 5);
        LocalDate deadline = readDate("Deadline (YYYY-MM-DD): ");
        TaskStatus status = readStatus("Status (TODO, IN_PROGRESS, BLOCKED, DONE): ");
        int estimatedMinutes = readPositiveInt("Timp estimat minute: ");

        Task task = service.createTask(
                title,
                description,
                priority,
                deadline,
                status,
                estimatedMinutes
        );

        System.out.println("Task adaugat cu succes. ID = " + task.getId());
    }

    private void handleDeleteTask() {
        System.out.println("\n--- Sterge Task ---");

        int id = readPositiveInt("ID de sters: ");
        Task task = service.findById(id);

        if (task == null) {
            System.out.println("Nu exista task cu acest ID.");
            return;
        }

        System.out.println("Task gasit:");
        System.out.println(task);

        String confirm = readLineAllowEmpty("Sigur stergi? (y/n): ").trim().toLowerCase();

        if (!(confirm.equals("y") || confirm.equals("yes") || confirm.equals("da"))) {
            System.out.println("Stergere anulata.");
            return;
        }

        boolean ok = service.deleteById(id);

        System.out.println(ok ? "Task sters." : "Task-ul nu a putut fi sters.");
    }

    private void handleEditStatus() {
        System.out.println("\n--- Schimba Status ---");

        int id = readPositiveInt("ID: ");
        Task task = service.findById(id);

        if (task == null) {
            System.out.println("Nu exista task cu acest ID.");
            return;
        }

        System.out.println("Task curent:");
        System.out.println(task);

        TaskStatus newStatus = readStatus("Status nou: ");

        boolean ok = service.updateStatus(id, newStatus);

        System.out.println(ok ? "Status modificat." : "Statusul nu a putut fi modificat.");
    }

    private void handleSearch() {
        System.out.println("\n--- Search ---");

        String query = readLineAllowEmpty("Cauta: ");
        List<Task> result = service.searchByTitleOrDescription(query);

        printTasks(result);
    }

    private void handleFilterByStatus() {
        System.out.println("\n--- Filtru dupa status ---");

        TaskStatus status = readStatus("Status: ");
        List<Task> result = service.filterByStatus(status);

        printTasks(result);
    }

    private void handleEditTaskFull() {
        System.out.println("\n--- Editare Task complet ---");

        int id = readPositiveInt("ID task: ");
        Task old = service.findById(id);

        if (old == null) {
            System.out.println("Nu exista task cu acest ID.");
            return;
        }

        System.out.println("Task curent:");
        System.out.println(old);

        System.out.println("ENTER = pastrezi valoarea curenta.");
        System.out.println("La descriere, '-' = stergi descrierea.");

        Task draft = old.copy();

        while (true) {
            String input = readLineAllowEmpty("Titlu nou [" + draft.getTitle() + "]: ");
            if (input.isBlank()) break;

            try {
                draft.setTitle(input);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("Titlu invalid: " + e.getMessage());
            }
        }

        while (true) {
            String input = readLineAllowEmpty("Descriere noua [" + draft.getDescription() + "]: ");
            if (input.isBlank()) break;

            try {
                if (input.trim().equals("-")) {
                    draft.setDescription("");
                } else {
                    draft.setDescription(input);
                }
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("Descriere invalida: " + e.getMessage());
            }
        }

        while (true) {
            String input = readLineAllowEmpty("Prioritate noua [" + draft.getPriority() + "]: ");
            if (input.isBlank()) break;

            try {
                draft.setPriority(Integer.parseInt(input.trim()));
                break;
            } catch (NumberFormatException e) {
                System.out.println("Introdu un numar.");
            } catch (IllegalArgumentException e) {
                System.out.println("Prioritate invalida: " + e.getMessage());
            }
        }

        while (true) {
            String input = readLineAllowEmpty("Deadline nou [" + draft.getDeadline() + "]: ");
            if (input.isBlank()) break;

            try {
                draft.setDeadline(LocalDate.parse(input.trim()));
                break;
            } catch (DateTimeParseException e) {
                System.out.println("Data invalida. Format: YYYY-MM-DD.");
            } catch (IllegalArgumentException e) {
                System.out.println("Deadline invalid: " + e.getMessage());
            }
        }

        while (true) {
            String input = readLineAllowEmpty("Status nou [" + draft.getStatus() + "]: ");
            if (input.isBlank()) break;

            try {
                draft.setStatus(TaskStatus.valueOf(input.trim().toUpperCase()));
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("Status invalid.");
            }
        }

        while (true) {
            String input = readLineAllowEmpty("Timp estimat nou [" + draft.getEstimatedMinutes() + "]: ");
            if (input.isBlank()) break;

            try {
                draft.setEstimatedMinutes(Integer.parseInt(input.trim()));
                break;
            } catch (NumberFormatException e) {
                System.out.println("Introdu un numar.");
            } catch (IllegalArgumentException e) {
                System.out.println("Timp invalid: " + e.getMessage());
            }
        }

        System.out.println("\n--- PREVIEW ---");
        System.out.println("Inainte:");
        System.out.println(old);

        System.out.println("Dupa:");
        System.out.println(draft);

        String confirm = readLineAllowEmpty("Salvezi? (y/n): ").trim().toLowerCase();

        if (!(confirm.equals("y") || confirm.equals("yes") || confirm.equals("da"))) {
            System.out.println("Editare anulata.");
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

        System.out.println(ok ? "Task editat." : "Task-ul nu a putut fi editat.");
    }

    private void handleUndo() {
        boolean ok = service.undo();
        System.out.println(ok ? "Undo realizat." : "Nu exista actiune pentru undo.");
    }

    private void handleRedo() {
        boolean ok = service.redo();
        System.out.println(ok ? "Redo realizat." : "Nu exista actiune pentru redo.");
    }

    private void handleQueryTasks() {
        System.out.println("\n--- Query: filtru + sort + paginare ---");

        String statusInput = readLineAllowEmpty("Status sau ENTER pentru toate: ").trim().toUpperCase();

        TaskStatus status = null;

        if (!statusInput.isBlank()) {
            status = TaskStatus.valueOf(statusInput);
        }

        String text = readLineAllowEmpty("Text search sau ENTER: ");

        System.out.println("Sort options: ID, PRIORITY, DEADLINE, TITLE, STATUS, ESTIMATED_MINUTES");
        String sortInput = readLineAllowEmpty("Sort by [ID]: ").trim().toUpperCase();

        TaskService.SortBy sortBy = sortInput.isBlank()
                ? TaskService.SortBy.ID
                : TaskService.SortBy.valueOf(sortInput);

        String dir = readLineAllowEmpty("Directie asc/desc [asc]: ").trim().toLowerCase();
        boolean desc = dir.equals("desc");

        int page = readPositiveInt("Page: ");
        int pageSize = readIntInRange("Page size (1-100): ", 1, 100);

        TaskQuery query = new TaskQuery(
                status,
                text,
                sortBy,
                desc,
                page,
                pageSize
        );

        int total = service.countTasks(status, text);
        List<Task> result = service.queryTasks(query);

        System.out.println("Total rezultate: " + total);
        printTasks(result);
    }

    private void handleBasicStats() {
        System.out.println("\n--- Statistici basic ---");

        Map<TaskStatus, Integer> counts = service.countByStatus();

        for (TaskStatus status : TaskStatus.values()) {
            System.out.println(status + ": " + counts.get(status));
        }

        Task mostUrgent = service.getMostUrgentTask();
        Task leastUrgent = service.getLeastUrgentTask();

        System.out.println("\nCel mai urgent:");
        System.out.println(mostUrgent == null ? "Nu exista task activ." : mostUrgent);

        System.out.println("\nCel mai putin urgent:");
        System.out.println(leastUrgent == null ? "Nu exista task activ." : leastUrgent);

        System.out.printf("Media timpului estimat: %.2f minute%n", service.getAverageEstimatedMinutes());
    }

    private void handleAdvancedStats() {
        System.out.println("\n--- Statistici avansate ---");

        StatsMode mode = readStatsMode("Mode active/all [active]: ");

        System.out.println("\nOverdue count by status:");
        Map<TaskStatus, Integer> overdue = service.overdueCountByStatus(mode);

        for (TaskStatus status : TaskStatus.values()) {
            if (!mode.allowed().contains(status)) continue;
            System.out.println(status + ": " + overdue.get(status));
        }

        int limit = readIntInRange("Top urgent per status limit (1-10): ", 1, 10);

        Map<TaskStatus, List<Task>> topMap = service.topUrgentPerStatus(mode, limit);

        for (TaskStatus status : TaskStatus.values()) {
            if (!mode.allowed().contains(status)) continue;

            System.out.println("\n== " + status + " ==");

            List<Task> list = topMap.get(status);

            if (list.isEmpty()) {
                System.out.println("Nimic.");
            } else {
                for (Task task : list) {
                    System.out.println("#" + task.getId()
                            + " | " + task.getTitle()
                            + " | prio=" + task.getPriority()
                            + " | deadline=" + task.getDeadline());
                }
            }
        }
    }

    private int readPositiveInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();

            try {
                int value = Integer.parseInt(line);

                if (value <= 0) {
                    System.out.println("Trebuie > 0.");
                    continue;
                }

                return value;

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
                int value = Integer.parseInt(line);

                if (value < min || value > max) {
                    System.out.println("Trebuie intre " + min + " si " + max + ".");
                    continue;
                }

                return value;

            } catch (NumberFormatException e) {
                System.out.println("Introdu doar cifre.");
            }
        }
    }

    private String readNonEmptyLine(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();

            if (line.isEmpty()) {
                System.out.println("Nu poate fi gol.");
                continue;
            }

            return line;
        }
    }

    private String readLineAllowEmpty(String prompt) {
        System.out.print(prompt);
        return sc.nextLine();
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();

            try {
                return LocalDate.parse(line);
            } catch (DateTimeParseException e) {
                System.out.println("Data invalida. Format corect: YYYY-MM-DD.");
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
                System.out.println("Status invalid. Exemple: TODO, IN_PROGRESS, BLOCKED, DONE.");
            }
        }
    }

    private StatsMode readStatsMode(String prompt) {
        while (true) {
            String line = readLineAllowEmpty(prompt).trim().toLowerCase();

            if (line.isEmpty() || line.equals("active")) {
                return StatsMode.ACTIVE;
            }

            if (line.equals("all")) {
                return StatsMode.ALL;
            }

            System.out.println("Scrie active sau all.");
        }
    }

    private void printTasks(List<Task> tasks) {
        System.out.println("\n--- TASKS ---");

        if (tasks == null || tasks.isEmpty()) {
            System.out.println("Nu exista task-uri.");
            return;
        }

        for (Task task : tasks) {
            System.out.println(task);
        }
    }
}