package com.sudeepkudari.ferry.ui;

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

import com.sudeepkudari.ferry.R;
import com.sudeepkudari.ferry.agent.AgentTaskService;
import com.sudeepkudari.ferry.databinding.ActivityMainBinding;
import com.sudeepkudari.ferry.net.PortalClient;
import com.sudeepkudari.ferry.util.SecureKeyStore;
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
            binding.statusLabel.setText("No API key for " + selected);
            binding.statusText.setText("Open Settings to add your key.");
        } else {
            binding.statusLabel.setText("Ready · " + selected);
            binding.statusText.setText("");
        }
        
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
            binding.statusText.setText("Cannot start: Accessibility Service is not enabled. Tap the warning banner to enable it.");
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            binding.statusText.setText("Cannot start: Please grant the 'Draw over other apps' permission so the agent can show the floating status window.");
            startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:" + getPackageName())));
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
