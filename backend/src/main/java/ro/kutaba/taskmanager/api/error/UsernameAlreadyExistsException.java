package ro.kutaba.taskmanager.api.error;

public class UsernameAlreadyExistsException extends RuntimeException{
    public UsernameAlreadyExistsException(String username){
        super("Username already exists: " + username);
    }
}