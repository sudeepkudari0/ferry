package com.sudeepkudari.ferry.ui;

import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sudeepkudari.ferry.R;
import com.sudeepkudari.ferry.data.TaskEntity;

import java.util.ArrayList;
import java.util.List;

public class TaskHistoryAdapter extends RecyclerView.Adapter<TaskHistoryAdapter.TaskViewHolder> {

    private List<TaskEntity> tasks = new ArrayList<>();

    public void setTasks(List<TaskEntity> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_history, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskEntity task = tasks.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView promptText;
        private final TextView statusText;
        private final TextView timeText;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            promptText = itemView.findViewById(R.id.taskPrompt);
            statusText = itemView.findViewById(R.id.taskStatus);
            timeText = itemView.findViewById(R.id.taskTime);
        }

        public void bind(TaskEntity task) {
            promptText.setText(task.prompt);
            statusText.setText(task.status);
            
            CharSequence timeAgo = android.text.format.DateUtils.getRelativeTimeSpanString(
                    task.startTime,
                    System.currentTimeMillis(),
                    android.text.format.DateUtils.MINUTE_IN_MILLIS
            );
            timeText.setText(timeAgo);
            
            if ("COMPLETED".equals(task.status)) {
                statusText.setTextColor(Color.parseColor("#059669")); // success
                statusText.setBackgroundColor(Color.parseColor("#1A059669"));
            } else if ("FAILED".equals(task.status)) {
                statusText.setTextColor(Color.parseColor("#DC2626")); // error
                statusText.setBackgroundColor(Color.parseColor("#1ADC2626"));
            } else {
                statusText.setTextColor(Color.parseColor("#D97706")); // warning
                statusText.setBackgroundColor(Color.parseColor("#1AD97706"));
            }
            
            itemView.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(itemView.getContext(), TaskDetailActivity.class);
                intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.id);
                itemView.getContext().startActivity(intent);
            });
        }
    }
}
