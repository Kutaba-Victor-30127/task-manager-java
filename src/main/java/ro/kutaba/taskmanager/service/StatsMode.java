package ro.kutaba.taskmanager.service;

import ro.kutaba.taskmanager.model.TaskStatus;

import java.util.EnumSet;
import java.util.Set;

public enum StatsMode {
    ACTIVE(EnumSet.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.BLOCKED)),
    ALL(EnumSet.allOf(TaskStatus.class));

    private final Set<TaskStatus> allowed;

    StatsMode(Set<TaskStatus> allowed){
        this.allowed = allowed;
    }

    public Set<TaskStatus> allowed(){
        return allowed;
    }
}