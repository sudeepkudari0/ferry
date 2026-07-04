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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sudeepkudari.ferry.model.UseCase;
import com.sudeepkudari.ferry.model.UseCaseParameter;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private TaskHistoryAdapter historyAdapter;
    private UseCaseAdapter useCaseAdapter;
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

        useCaseAdapter = new UseCaseAdapter(useCase -> {
            UseCaseBottomSheetFragment.newInstance(useCase)
                    .show(getSupportFragmentManager(), "useCaseSheet");
        });
        binding.useCasesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.useCasesRecyclerView.setAdapter(useCaseAdapter);
        setupUseCases();

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

    private void setupUseCases() {
        List<UseCase> useCases = new ArrayList<>();

        useCases.add(new UseCase(
                "linkedin_jobs",
                "LinkedIn Jobs",
                "Auto-apply for jobs",
                android.R.drawable.ic_menu_myplaces,
                Arrays.asList(
                        new UseCaseParameter("role", "Job Role (e.g. Android Developer)", UseCaseParameter.Type.TEXT, null),
                        new UseCaseParameter("location", "Location (e.g. Remote, NY)", UseCaseParameter.Type.TEXT, null),
                        new UseCaseParameter("apply_type", "Application Type", UseCaseParameter.Type.RADIO, Arrays.asList("Easy Apply", "External Site"))
                ),
                "Open LinkedIn app, search for {role} jobs in {location}, and start applying using {apply_type}."
        ));

        useCases.add(new UseCase(
                "naukri_jobs",
                "Naukri Jobs",
                "Apply on Naukri",
                android.R.drawable.ic_menu_recent_history,
                Arrays.asList(
                        new UseCaseParameter("role", "Job Role", UseCaseParameter.Type.TEXT, null),
                        new UseCaseParameter("experience", "Experience (Years)", UseCaseParameter.Type.TEXT, null)
                ),
                "Open Naukri app, search for {role} jobs for {experience} years experience, and apply."
        ));

        useCases.add(new UseCase(
                "messaging",
                "Send Message",
                "WhatsApp / SMS",
                android.R.drawable.ic_dialog_email,
                Arrays.asList(
                        new UseCaseParameter("app", "App", UseCaseParameter.Type.DROPDOWN, Arrays.asList("WhatsApp", "SMS")),
                        new UseCaseParameter("recipient", "Recipient Name", UseCaseParameter.Type.TEXT, null),
                        new UseCaseParameter("message", "Message", UseCaseParameter.Type.MULTILINE_TEXT, null)
                ),
                "Open {app}, find {recipient}, and send this message: {message}"
        ));

        useCases.add(new UseCase(
                "gmail",
                "Send Email",
                "Compose a Gmail",
                android.R.drawable.ic_dialog_email,
                Arrays.asList(
                        new UseCaseParameter("to", "To", UseCaseParameter.Type.TEXT, null),
                        new UseCaseParameter("subject", "Subject", UseCaseParameter.Type.TEXT, null),
                        new UseCaseParameter("body", "Body", UseCaseParameter.Type.MULTILINE_TEXT, null)
                ),
                "Open Gmail, compose an email to {to} with subject '{subject}' and body: '{body}', then send."
        ));

        useCaseAdapter.setUseCases(useCases);
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
