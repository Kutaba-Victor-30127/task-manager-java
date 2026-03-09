package ro.kutaba.taskmanager.service;

import ro.kutaba.taskmanager.model.TaskStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoUnit.MINUTES;

public class AuditStatusAnalytics{
    // log line format: 2026-02-21T11:54:15.123 | MESSAGE
    private static final Pattern LINE = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?) \\| (.*)$"
    );

    // CREATE | id=1 .... status = TODO
    private static final Pattern CREATE = Pattern.compile(
        "CREATE \\| .*id=(\\d+).*status=(TODO|IN_PROGRESS|BLOCKED|DONE).*"
    );

    // STATUS | id=1    TODO -> IN_PROGRESS
    private static final Pattern STATUS  = Pattern.compile(
        "STATUS \\| .*id=(\\d+) (TODO|IN_PROGRESS|BLOCKED|DONE) -> (TODO|IN_PROGRESS|BLOCKED|DONE)"
    );

    public record AvgStatusMinutes(
        Map<TaskStatus, Double> avgByStatus,
        Map<TaskStatus, Long> totalMinutesByStatus,
        Map<TaskStatus, Long> samplesByStatus
    ){}
    
    private record Event(LocalDateTime ts, int taskId, TaskStatus to, boolean isCreate) {}

    public static AvgStatusMinutes averageMinutesPerStatus(Path auditLogPath, StatsMode mode) {
        return averageMinutesPerStatus(auditLogPath, mode, LocalDateTime.now());
    }

    public static AvgStatusMinutes averageMinutesPerStatus(Path auditLogPath, StatsMode mode, LocalDateTime now){
        List<Event> events = readEvents(auditLogPath);

        //group events by task
        Map<Integer, List<Event>> byTask = new HashMap<>();
        for (Event e : events){
            byTask.computeIfAbsent(e.taskId, k -> new ArrayList<>()).add(e);
        }

        for (var list : byTask.values()){
            list.sort(Comparator.comparing(Event::ts));
        }

        EnumMap<TaskStatus, Long> total = new EnumMap<>(TaskStatus.class);
        EnumMap<TaskStatus, Long> samples = new EnumMap<>(TaskStatus.class);

        for (TaskStatus s : TaskStatus.values()){
            total.put(s, 0L);
            samples.put(s, 0L);
        }
 
        for (List<Event> tl : byTask.values()){
            if (tl.isEmpty()) continue;

            //trebuie sa avem create ca sa stim startul
            Event first = tl.get(0);
            if (!first.isCreate()) continue;

            TaskStatus current = first.to();
            LocalDateTime segmentStart = first.ts();

            for (int i = 1; i < tl.size(); i++){
                Event e = tl.get(i);
                if (e.isCreate()) continue;

                Long minutes = Math.max(0, MINUTES.between(segmentStart, e.ts()));
                if (mode.allowed().contains(current)){
                    total.put(current, total.get(current) + minutes);
                    samples.put(current, samples.get(current) + 1);
                }

                current = e.to();
                segmentStart = e.ts();
            }

            Long tail = Math.max(0, MINUTES.between(segmentStart,now));
            if (mode.allowed().contains(current)){
                total.put(current, total.get(current) + tail);
                samples.put(current, samples.get(current) + 1);
            }
        }

        EnumMap<TaskStatus, Double> avg = new EnumMap<>(TaskStatus.class);
        for (TaskStatus s : TaskStatus.values()){
            long n = samples.get(s);
            avg.put(s, n == 0 ? 0.0 : (total.get(s) * 1.0 / n));
        }

        return new AvgStatusMinutes(avg, total, samples);
    }


    private static List<Event> readEvents(Path p){
        List<Event> out = new ArrayList<>();
        if (p == null || Files.notExists(p)) return out;

        try{
            for(String line : Files.readAllLines(p)){
                Matcher m = LINE.matcher(line);
                if (!m.find()) continue;
                LocalDateTime ts = LocalDateTime.parse(m.group(1));
                String msg = m.group(2);

                Matcher c = CREATE.matcher(msg);
                if (c.find()) {
                    int id = Integer.parseInt(c.group(1));
                    TaskStatus st = TaskStatus.valueOf(c.group(2));
                    out.add(new Event(ts, id, st, true));
                    continue;
                }

                Matcher s = STATUS.matcher(msg);
                if (s.find()){
                    int id = Integer.parseInt(s.group(1));
                    TaskStatus to = TaskStatus.valueOf(s.group(3));
                    out.add(new Event(ts, id, to, false));

                }
            }
        }catch (Exception e){
             System.err.println("Audit parse failed: " + e.getMessage());
        }
        return out;
    }

}
