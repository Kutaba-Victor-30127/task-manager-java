package ro.kutaba.taskmanager.storage;

import ro.kutaba.taskmanager.model.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FileTaskRepositoryAdapter implements TaskRepository{

    private final FileTaskRepository fileRepo;  // clasa ta existenta
    private final List<Task> tasks;

    public FileTaskRepositoryAdapter(FileTaskRepository fileRepo){
        this.fileRepo = fileRepo;
        this.tasks = new ArrayList<>(fileRepo.loadAll());
    }

    @Override
    public List<Task> findAll(){
        return new ArrayList<>(tasks);
    }

    @Override
    public Optional<Task> findById(int id){
        return tasks.stream().filter(t -> t.getId() == id).findFirst();
    }

    @Override
    public Task save(Task t){
        // update daca exista daca nu create
        Optional<Task> existing = findById(t.getId());
        if (existing.isPresent()){
            int idx = tasks.indexOf(existing.get());
            tasks.set(idx, t);
        }else {
            tasks.add(t);
        }
        fileRepo.saveAll(tasks);
        return t;
    }

    @Override
    public boolean deleteById(int id){
        boolean removed = tasks.removeIf(t -> t.getId() == id);
        if (removed) fileRepo.saveAll(tasks);
        return removed;
    }
    


}