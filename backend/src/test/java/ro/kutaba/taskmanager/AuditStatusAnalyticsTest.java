package ro.kutaba.taskmanager.service;

import org.junit.jupiter.api.Test;
import ro.kutaba.taskmanager.model.TaskStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuditStatusAnalyticsTest{

    @Test
    void averageMinutesPerStatus_singleTask_segmentsAndTail_allMode() throws Exception{
        Path tmp = Files.createTempFile("audit", ".log");
        Files.writeString(tmp, String.join(System.lineSeparator(),
            "2026-02-21T10:00:00 | CREATE | id=1 title\"A\" description\"\" priority=3 deadline=2026-02-25 status=TODO est=60",
            "2026-02-21T10:30:00 | STATUS | id=1 TODO -> IN_PROGRESS",
            "2026-02-21T11:00:00 | STATUS | id=1 IN_PROGRESS -> DONE"
        ));

        LocalDateTime now = LocalDateTime.parse("2026-02-21T12:00:00");

        var res = AuditStatusAnalytics.averageMinutesPerStatus(tmp, StatsMode.ALL, now);

        assertEquals(30l, res.totalMinutesByStatus().get(TaskStatus.TODO));
        assertEquals(1l, res.samplesByStatus().get(TaskStatus.TODO));

        assertEquals(30L, res.totalMinutesByStatus().get(TaskStatus.IN_PROGRESS));
        assertEquals(1L, res.samplesByStatus().get(TaskStatus.IN_PROGRESS));

        // DONE tail: 11:00 -> 12:00 = 60
        assertEquals(60L, res.totalMinutesByStatus().get(TaskStatus.DONE));
        assertEquals(1L, res.samplesByStatus().get(TaskStatus.DONE));

        assertEquals(30.0, res.avgByStatus().get(TaskStatus.TODO));
        assertEquals(30.0, res.avgByStatus().get(TaskStatus.IN_PROGRESS));
        assertEquals(60.0, res.avgByStatus().get(TaskStatus.DONE));
    }

    @Test
    void averageMinutesPerStatus_activeMode_ignoresDone() throws Exception{
        Path tmp = Files.createTempFile("audit", ".log");
        Files.writeString(tmp, String.join(System.lineSeparator(),
                "2026-02-21T10:00:00 | CREATE | id=1 title\"A\" description\"\" priority=3 deadline=2026-02-25 status=TODO est=60",
                "2026-02-21T10:30:00 | STATUS | id=1 TODO -> DONE"
        ));

        LocalDateTime now = LocalDateTime.parse("2026-02-21T11:30:00");

        var res = AuditStatusAnalytics.averageMinutesPerStatus(tmp, StatsMode.ACTIVE, now);

        // TODO: 10:00 -> 10:30 = 30
        assertEquals(30L, res.totalMinutesByStatus().get(TaskStatus.TODO));
        assertEquals(1L, res.samplesByStatus().get(TaskStatus.TODO));

        // DONE ignored in ACTIVE
        assertEquals(0L, res.totalMinutesByStatus().get(TaskStatus.DONE));
        assertEquals(0L, res.samplesByStatus().get(TaskStatus.DONE));
    }

    @Test
    void averageMinutesPerStatus_missingCreate_isIgnored() throws Exception {
        Path tmp = Files.createTempFile("audit", ".log");
        Files.writeString(tmp, String.join(System.lineSeparator(),
                "2026-02-21T10:30:00 | STATUS | id=99 TODO -> IN_PROGRESS"
        ));

        LocalDateTime now = LocalDateTime.parse("2026-02-21T12:00:00");

        var res = AuditStatusAnalytics.averageMinutesPerStatus(tmp, StatsMode.ALL, now);

        // totul rămâne 0 pentru că nu avem CREATE ca start
        for (TaskStatus s : TaskStatus.values()) {
            assertEquals(0L, res.totalMinutesByStatus().get(s));
            assertEquals(0L, res.samplesByStatus().get(s));
            assertEquals(0.0, res.avgByStatus().get(s));
        }
    }
}