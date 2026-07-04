package com.sudeepkudari.ferry.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TaskDao {
    
    @Insert
    void insertTask(TaskEntity task);

    @Query("UPDATE tasks SET status = :status, endTime = :endTime WHERE id = :id")
    void updateTaskStatus(String id, String status, long endTime);

    @Insert
    void insertStep(StepEntity step);

    @Query("SELECT * FROM tasks ORDER BY startTime DESC")
    List<TaskEntity> getAllTasks();

    @Query("SELECT * FROM steps WHERE taskId = :taskId ORDER BY stepNumber ASC")
    List<StepEntity> getStepsForTask(String taskId);
    
    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    TaskEntity getTaskById(String taskId);
}
