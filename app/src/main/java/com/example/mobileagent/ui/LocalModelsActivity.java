package com.example.mobileagent.ui;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobileagent.R;
import com.example.mobileagent.databinding.ActivityLocalModelsBinding;
import com.example.mobileagent.llm.local.LocalModel;
import com.example.mobileagent.llm.local.LocalModelRegistry;
import com.example.mobileagent.llm.local.ModelDownloadManager;
import com.example.mobileagent.util.SecureKeyStore;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Activity for managing local LLM configuration:
 * - Configure the local server URL and model name
 * - Test connectivity to the local server
 * - Download/delete recommended GGUF model files
 */
public class LocalModelsActivity extends AppCompatActivity implements LocalModelAdapter.ModelActionListener {

    private ActivityLocalModelsBinding binding;
    private SecureKeyStore keyStore;
    private ModelDownloadManager downloadManager;
    private LocalModelAdapter adapter;
    private List<LocalModel> models;

    /** Listens for download completion broadcasts from Android DownloadManager. */
    private final BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Refresh the model list to show updated download states
            refreshModelList();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLocalModelsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        keyStore = new SecureKeyStore(this);
        downloadManager = new ModelDownloadManager(this);

        // Load saved configuration
        binding.serverUrlInput.setText(keyStore.getLocalServerUrl());
        String savedModel = keyStore.getSelectedModel("LOCAL");
        if (savedModel != null && !savedModel.isEmpty()) {
            binding.modelNameInput.setText(savedModel);
        }

        // Setup model list
        models = LocalModelRegistry.getAllModels();
        adapter = new LocalModelAdapter(models, downloadManager, this);

        RecyclerView recyclerView = binding.modelsRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Button listeners
        binding.testConnectionButton.setOnClickListener(v -> testConnection());
        binding.saveConfigButton.setOnClickListener(v -> saveConfiguration());

        updateDiskUsage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register for download complete notifications
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadCompleteReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(downloadCompleteReceiver, filter);
        }
        refreshModelList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(downloadCompleteReceiver);
    }

    private void testConnection() {
        String serverUrl = binding.serverUrlInput.getText() != null
                ? binding.serverUrlInput.getText().toString().trim() : "";

        if (serverUrl.isEmpty()) {
            showStatus("Please enter a server URL.", false);
            return;
        }

        binding.testConnectionButton.setEnabled(false);
        binding.testConnectionButton.setText("Testing…");
        binding.connectionStatusText.setVisibility(View.GONE);

        new Thread(() -> {
            ConnectionResult result = performConnectionTest(serverUrl);
            runOnUiThread(() -> {
                binding.testConnectionButton.setEnabled(true);
                binding.testConnectionButton.setText("Test Connection");
                showStatus(result.message, result.success);

                // If we got model list from the server, offer to auto-fill model name
                if (result.success && result.models != null && !result.models.isEmpty()) {
                    String currentModel = binding.modelNameInput.getText() != null
                            ? binding.modelNameInput.getText().toString().trim() : "";
                    if (currentModel.isEmpty() || "default".equals(currentModel)) {
                        binding.modelNameInput.setText(result.models.get(0));
                    }
                }
            });
        }).start();
    }

    private void saveConfiguration() {
        String serverUrl = binding.serverUrlInput.getText() != null
                ? binding.serverUrlInput.getText().toString().trim() : "";
        String modelName = binding.modelNameInput.getText() != null
                ? binding.modelNameInput.getText().toString().trim() : "";

        if (serverUrl.isEmpty()) {
            showStatus("Please enter a server URL.", false);
            return;
        }

        keyStore.setLocalServerUrl(serverUrl);
        keyStore.setSelectedProvider("LOCAL");
        if (!modelName.isEmpty()) {
            keyStore.setSelectedModel("LOCAL", modelName);
        }

        Toast.makeText(this, "Local LLM activated!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void showStatus(String message, boolean success) {
        binding.connectionStatusText.setVisibility(View.VISIBLE);
        binding.connectionStatusText.setText(message);
        binding.connectionStatusText.setTextColor(
                success ? Color.parseColor("#2E7D32") : Color.parseColor("#C62828"));
    }

    @Override
    public void onDownloadClicked(LocalModel model) {
        new AlertDialog.Builder(this)
                .setTitle("Download " + model.getDisplayName() + "?")
                .setMessage("This will download " + model.getFormattedSize() 
                        + " to your device.\n\n"
                        + "Requires: " + model.getRecommendedRamGb() + " GB+ RAM\n"
                        + "Quantization: " + model.getQuantization() + "\n\n"
                        + "Download will start in the background. You can use the app while it downloads.")
                .setPositiveButton("Download (Wi-Fi only)", (d, w) -> {
                    long id = downloadManager.startDownload(model);
                    if (id != -1) {
                        Toast.makeText(this, "Download started — check notifications for progress", 
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Model already downloaded or download failed", 
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Download (any network)", (d, w) -> {
                    long id = downloadManager.startDownloadAllowMobileData(model);
                    if (id != -1) {
                        Toast.makeText(this, "Download started — check notifications for progress", 
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDeleteClicked(LocalModel model) {
        new AlertDialog.Builder(this)
                .setTitle("Delete " + model.getDisplayName() + "?")
                .setMessage("This will free up " + model.getFormattedSize() + " of storage.")
                .setPositiveButton("Delete", (d, w) -> {
                    downloadManager.deleteModel(model);
                    refreshModelList();
                    updateDiskUsage();
                    Toast.makeText(this, model.getDisplayName() + " deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshModelList() {
        adapter.notifyDataSetChanged();
        updateDiskUsage();
    }

    private void updateDiskUsage() {
        long usage = downloadManager.getTotalDiskUsage();
        if (usage > 0) {
            binding.diskUsageText.setText("Storage used: " + ModelDownloadManager.formatBytes(usage));
        } else {
            binding.diskUsageText.setText("No models downloaded yet.");
        }
    }

    /**
     * Tests connection to the local LLM server. Tries:
     * 1. GET /v1/models (OpenAI-compatible models endpoint)
     * 2. GET /api/tags (Ollama-specific endpoint)
     * 3. GET / (basic health check)
     */
    private ConnectionResult performConnectionTest(String serverUrl) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();

        String baseUrl = serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";

        // Try OpenAI-compatible /v1/models endpoint first
        try {
            Request request = new Request.Builder()
                    .url(baseUrl + "v1/models")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    List<String> models = parseModels(body);
                    if (!models.isEmpty()) {
                        return new ConnectionResult(true,
                                "✓ Connected! Found " + models.size() + " model(s): " + String.join(", ", models),
                                models);
                    }
                    return new ConnectionResult(true, "✓ Connected to server!", null);
                }
            }
        } catch (Exception ignored) {
        }

        // Try Ollama /api/tags endpoint
        try {
            Request request = new Request.Builder()
                    .url(baseUrl + "api/tags")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    List<String> models = parseOllamaModels(body);
                    if (!models.isEmpty()) {
                        return new ConnectionResult(true,
                                "✓ Connected to Ollama! Found " + models.size() + " model(s): " + String.join(", ", models),
                                models);
                    }
                    return new ConnectionResult(true, "✓ Connected to Ollama! No models pulled yet — run `ollama pull <model>`", null);
                }
            }
        } catch (Exception ignored) {
        }

        // Try basic health check
        try {
            Request request = new Request.Builder()
                    .url(baseUrl)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return new ConnectionResult(true,
                            "✓ Server is running! Couldn't detect models — enter model name manually.", null);
                } else {
                    return new ConnectionResult(false,
                            "✗ Server responded with HTTP " + response.code(), null);
                }
            }
        } catch (java.net.ConnectException e) {
            return new ConnectionResult(false,
                    "✗ Cannot connect to " + serverUrl + " — is the server running?", null);
        } catch (Exception e) {
            return new ConnectionResult(false,
                    "✗ Connection error: " + e.getMessage(), null);
        }
    }

    /** Parse model IDs from OpenAI-compatible /v1/models response. */
    private List<String> parseModels(String responseBody) {
        List<String> models = new java.util.ArrayList<>();
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray data = json.getAsJsonArray("data");
            if (data != null) {
                for (int i = 0; i < data.size(); i++) {
                    JsonObject model = data.get(i).getAsJsonObject();
                    models.add(model.get("id").getAsString());
                }
            }
        } catch (Exception ignored) {
        }
        return models;
    }

    /** Parse model names from Ollama /api/tags response. */
    private List<String> parseOllamaModels(String responseBody) {
        List<String> models = new java.util.ArrayList<>();
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray modelsArray = json.getAsJsonArray("models");
            if (modelsArray != null) {
                for (int i = 0; i < modelsArray.size(); i++) {
                    JsonObject model = modelsArray.get(i).getAsJsonObject();
                    models.add(model.get("name").getAsString());
                }
            }
        } catch (Exception ignored) {
        }
        return models;
    }

    private static class ConnectionResult {
        final boolean success;
        final String message;
        final List<String> models;

        ConnectionResult(boolean success, String message, List<String> models) {
            this.success = success;
            this.message = message;
            this.models = models;
        }
    }
}
