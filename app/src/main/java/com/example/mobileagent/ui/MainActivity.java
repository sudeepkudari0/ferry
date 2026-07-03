package com.example.mobileagent.ui;

import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.runButton.setOnClickListener(v -> onRunClicked());
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean portalOk = PortalClient.isPortalInstalled(this);
        binding.portalWarning.setVisibility(portalOk ? android.view.View.GONE : android.view.View.VISIBLE);

        SecureKeyStore keyStore = new SecureKeyStore(this);
        if (!keyStore.hasAnthropicKey()) {
            binding.statusText.setText("No API key set. Open Settings to add your Anthropic key (BYOK).");
        }
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
            binding.statusText.setText("Cannot start: Portal is not installed. See README.");
            return;
        }

        Intent serviceIntent = new Intent(this, AgentTaskService.class);
        serviceIntent.putExtra(AgentTaskService.EXTRA_TASK, task);
        startForegroundService(serviceIntent);

        binding.statusText.setText("Task started — check the notification for live progress.\n"
                + "(A full in-app step log/history view is a natural next feature to add here.)");
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
