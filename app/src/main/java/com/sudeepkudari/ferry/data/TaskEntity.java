package com.sudeepkudari.ferry.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class TaskEntity {
    
    @PrimaryKey
    @NonNull
    public String id;

    public String prompt;
    public long startTime;
    public long endTime;
    public String status; // RUNNING, COMPLETED, FAILED

    public TaskEntity(@NonNull String id, String prompt, long startTime, String status) {
        this.id = id;
        this.prompt = prompt;
        this.startTime = startTime;
        this.status = status;
        this.endTime = 0;
    }
}
