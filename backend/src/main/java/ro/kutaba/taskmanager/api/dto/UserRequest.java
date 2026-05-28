package ro.kutaba.taskmanager.api.dto;

public record UserRequest (
    String username,
    String password
){}
