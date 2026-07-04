package com.sudeepkudari.ferry.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sudeepkudari.ferry.R;
import com.sudeepkudari.ferry.data.AppDatabase;
import com.sudeepkudari.ferry.data.StepEntity;
import com.sudeepkudari.ferry.data.TaskDao;
import com.sudeepkudari.ferry.data.TaskEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "extra_task_id";

    private StepHistoryAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        RecyclerView recyclerView = findViewById(R.id.stepsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StepHistoryAdapter();
        recyclerView.setAdapter(adapter);

        String taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        if (taskId != null) {
            loadTaskDetails(taskId);
        }
    }

    private void loadTaskDetails(String taskId) {
        executor.execute(() -> {
            TaskDao dao = AppDatabase.getDatabase(this).taskDao();
            TaskEntity task = dao.getTaskById(taskId);
            List<StepEntity> steps = dao.getStepsForTask(taskId);

            runOnUiThread(() -> {
                if (task != null) {
                    TextView promptText = findViewById(R.id.taskPrompt);
                    TextView statusText = findViewById(R.id.taskStatus);

                    promptText.setText(task.prompt);
                    statusText.setText(task.status);
                    
                    if ("COMPLETED".equals(task.status)) {
                        statusText.setTextColor(Color.parseColor("#059669"));
                        statusText.setBackgroundColor(Color.parseColor("#1A059669"));
                    } else if ("FAILED".equals(task.status)) {
                        statusText.setTextColor(Color.parseColor("#DC2626"));
                        statusText.setBackgroundColor(Color.parseColor("#1ADC2626"));
                    } else {
                        statusText.setTextColor(Color.parseColor("#D97706"));
                        statusText.setBackgroundColor(Color.parseColor("#1AD97706"));
                    }
                }
                adapter.setSteps(steps);
            });
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
