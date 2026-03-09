package ro.kutaba.taskmanager.api.dto;

import ro.kutaba.taskmanager.model.TaskStatus;

public record BottleneckResponse(
    String mode,
    TaskStatus worstAvgStatus,
    double worstAvgMinutes,
    TaskStatus worstTotalStatus,
    long worstTotalMinutes,
    TaskStatus worstOverdueStatus,
    int worstOverdueCount
){}