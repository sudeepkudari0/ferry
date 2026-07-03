package com.example.mobileagent.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.mobileagent.R;
import com.example.mobileagent.agent.AgentTaskService;
import com.example.mobileagent.databinding.ActivityMainBinding;
import com.example.mobileagent.net.PortalClient;
import com.example.mobileagent.util.SecureKeyStore;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    
    private final BroadcastReceiver logReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.hasExtra("log_line")) {
                String line = intent.getStringExtra("log_line");
                String current = binding.statusText.getText().toString();
                binding.statusText.setText(line + "\n\n" + current);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        binding.runButton.setOnClickListener(v -> onRunClicked());
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
            binding.statusText.setText("No API key set for " + selected + ". Open Settings to add your key.");
        } else {
            binding.statusText.setText("Ready to run on " + selected);
        }
        
        // Register receiver for logs
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(logReceiver, new IntentFilter("com.example.mobileagent.LOG"), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(logReceiver, new IntentFilter("com.example.mobileagent.LOG"));
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
            binding.statusText.setText("Cannot start: Accessibility Service is not enabled. Tap the warning banner to enable it.");
            return;
        }

        Intent serviceIntent = new Intent(this, AgentTaskService.class);
        serviceIntent.putExtra(AgentTaskService.EXTRA_TASK, task);
        startForegroundService(serviceIntent);

        binding.statusText.setText("Task started — check below for live progress.\n");
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
