package ro.kutaba.taskmanager.service;

import ro.kutaba.taskmanager.model.TaskStatus;

public record TaskQuery(
        TaskStatus statusFilter,
        String textQuery,
        TaskService.SortBy sortBy,
        boolean desc,
        int page,
        int pageSize
) {}