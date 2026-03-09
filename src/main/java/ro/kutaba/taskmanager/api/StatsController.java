package ro.kutaba.taskmanager.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.*;

import ro.kutaba.taskmanager.api.dto.BottleneckResponse;
import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.service.AuditStatusAnalytics;
import ro.kutaba.taskmanager.service.StatsMode;
import ro.kutaba.taskmanager.service.TaskService;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.ToLongFunction;

@RestController
@RequestMapping("/api/stats")
@Tag(name = "Stats", description = "Statistici pentru task-uri + analiză din audit.log")
public class StatsController {

    private final TaskService service;

    public StatsController(TaskService service) {
        this.service = service;
    }

    @Operation(summary = "Count task-uri by status (ALL, fără filtru)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")
    })
    @GetMapping("/count-by-status")
    public Map<TaskStatus, Integer> countByStatus() {
        return service.countByStatus();
    }

    @Operation(summary = "Overdue count by status (filtrat după mode=ACTIVE/ALL)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")
    })
    @GetMapping("/overdue-by-status")
    public Map<TaskStatus, Integer> overdueByStatus(
            @RequestParam(defaultValue = "ACTIVE") StatsMode mode
    ) {
        return service.overdueCountByStatus(mode);
    }

    @Operation(summary = "Average minutes per status din audit.log (filtrat după mode)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")
    })
    @GetMapping("/audit/avg-minutes-by-status")
    public AuditStatusAnalytics.AvgStatusMinutes avgMinutesByStatus(
            @RequestParam(defaultValue = "ACTIVE") StatsMode mode
    ) {
        return AuditStatusAnalytics.averageMinutesPerStatus(Path.of("data/audit.log"), mode);
    }

    @Operation(summary = "Bottleneck summary (status cu max avg time / max total time / max overdue)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")
    })
    @GetMapping("/bottleneck")
    public BottleneckResponse bottleneck(
            @RequestParam(defaultValue = "ACTIVE") StatsMode mode
    ) {
        var overdue = service.overdueCountByStatus(mode);
        var res = AuditStatusAnalytics.averageMinutesPerStatus(Path.of("data/audit.log"), mode);

        TaskStatus worstAvg = argMaxStatus(s -> Math.round(res.avgByStatus().getOrDefault(s, 0.0)), mode);
        TaskStatus worstTotal = argMaxStatus(s -> res.totalMinutesByStatus().getOrDefault(s, 0L), mode);
        TaskStatus worstOverdue = argMaxStatus(s -> overdue.getOrDefault(s, 0), mode);

        return new BottleneckResponse(
                mode.name(),
                worstAvg,
                res.avgByStatus().getOrDefault(worstAvg, 0.0),
                worstTotal,
                res.totalMinutesByStatus().getOrDefault(worstTotal, 0L),
                worstOverdue,
                overdue.getOrDefault(worstOverdue, 0)
        );
    }

    private TaskStatus argMaxStatus(ToLongFunction<TaskStatus> scoreFn, StatsMode mode) {
        TaskStatus best = null;
        long bestScore = Long.MIN_VALUE;

        for (TaskStatus s : TaskStatus.values()) {
            if (!mode.allowed().contains(s)) continue;

            long score = scoreFn.applyAsLong(s);
            if (best == null || score > bestScore) {
                best = s;
                bestScore = score;
            }
        }
        return best;
    }
}