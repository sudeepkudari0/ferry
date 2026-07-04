package com.sudeepkudari.ferry.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "steps",
    foreignKeys = @ForeignKey(
        entity = TaskEntity.class,
        parentColumns = "id",
        childColumns = "taskId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("taskId")}
)
public class StepEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String taskId;
    public int stepNumber;
    public String actionType;
    public String reasoning;
    public String summary; // concise string of the action taken
    public long timestamp;

    public StepEntity(String taskId, int stepNumber, String actionType, String reasoning, String summary, long timestamp) {
        this.taskId = taskId;
        this.stepNumber = stepNumber;
        this.actionType = actionType;
        this.reasoning = reasoning;
        this.summary = summary;
        this.timestamp = timestamp;
    }
}
