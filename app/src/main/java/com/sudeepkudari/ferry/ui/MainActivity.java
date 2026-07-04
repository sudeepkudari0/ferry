package com.sudeepkudari.ferry.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.app.Activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.widget.Toolbar;

import com.sudeepkudari.ferry.R;
import com.sudeepkudari.ferry.agent.AgentTaskService;
import com.sudeepkudari.ferry.databinding.ActivityMainBinding;
import com.sudeepkudari.ferry.net.PortalClient;
import com.sudeepkudari.ferry.net.PortalClient;
import com.sudeepkudari.ferry.util.SecureKeyStore;
import com.sudeepkudari.ferry.data.AppDatabase;
import com.sudeepkudari.ferry.data.TaskDao;
import com.sudeepkudari.ferry.data.TaskEntity;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private TaskHistoryAdapter historyAdapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;
    
    private final BroadcastReceiver logReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Refresh history when an update comes in
            loadHistory();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        historyAdapter = new TaskHistoryAdapter();
        binding.taskHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.taskHistoryRecyclerView.setAdapter(historyAdapter);

        binding.runButton.setOnClickListener(v -> onRunClicked());
        
        speechRecognizerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        List<String> results = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (results != null && !results.isEmpty()) {
                            binding.taskInput.setText(results.get(0));
                        }
                    }
                }
        );

        binding.taskInputLayout.setEndIconOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "What would you like Ferry to do?");
            try {
                speechRecognizerLauncher.launch(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean portalOk = PortalClient.isPortalInstalled(this);
        binding.portalWarning.setVisibility(portalOk ? android.view.View.GONE : android.view.View.VISIBLE);
        binding.portalWarning.setOnClickListener(v -> {
            startActivity(new android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
        });

        SecureKeyStore keyStore = new SecureKeyStore(this);
        String selected = keyStore.getSelectedProvider();
        if (!keyStore.hasKey(selected)) {
            binding.statusLabel.setText("No API key for " + selected);
        } else {
            binding.statusLabel.setText("Ready · " + selected);
        }
        
        loadHistory();
        
        // Register receiver for logs
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(logReceiver, new IntentFilter("com.sudeepkudari.ferry.LOG"), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(logReceiver, new IntentFilter("com.sudeepkudari.ferry.LOG"));
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(logReceiver);
    }

    private void onRunClicked() {
        String task = binding.taskInput.getText() != null
                ? binding.taskInput.getText().toString().trim()
                : "";

        if (task.isEmpty()) {
            binding.taskInput.setError("Enter a task first");
            return;
        }
        if (!PortalClient.isPortalInstalled(this)) {
            Toast.makeText(this, "Cannot start: Accessibility Service is not enabled.", Toast.LENGTH_LONG).show();
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Cannot start: Please grant the 'Draw over other apps' permission.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:" + getPackageName())));
            return;
        }

        Intent serviceIntent = new Intent(this, AgentTaskService.class);
        serviceIntent.putExtra(AgentTaskService.EXTRA_TASK, task);
        startForegroundService(serviceIntent);
        
        binding.taskInput.setText("");
        Toast.makeText(this, "Task started", Toast.LENGTH_SHORT).show();
    }

    private void loadHistory() {
        executor.execute(() -> {
            TaskDao dao = AppDatabase.getDatabase(this).taskDao();
            List<TaskEntity> tasks = dao.getAllTasks();
            runOnUiThread(() -> historyAdapter.setTasks(tasks));
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
