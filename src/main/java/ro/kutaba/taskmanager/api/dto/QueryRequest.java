package ro.kutaba.taskmanager.api.dto;

import ro.kutaba.taskmanager.model.TaskStatus;
import ro.kutaba.taskmanager.service.TaskService;

public record QueryRequest(
        TaskStatus statusFilter,
        String textQuery,
        TaskService.SortBy sortBy,
        boolean desc,
        int page,
        int pageSize
) {}