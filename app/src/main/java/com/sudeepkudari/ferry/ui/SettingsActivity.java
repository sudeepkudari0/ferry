package com.sudeepkudari.ferry.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sudeepkudari.ferry.databinding.ActivitySettingsBinding;
import com.sudeepkudari.ferry.llm.ProviderType;
import com.sudeepkudari.ferry.util.SecureKeyStore;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SecureKeyStore keyStore;
    
    private String currentSelectedProvider;
    private String currentSelectedModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        keyStore = new SecureKeyStore(this);
        
        List<String> providers = new ArrayList<>();
        providers.add(ProviderType.ANTHROPIC.name());
        providers.add(ProviderType.OPENAI.name());
        providers.add(ProviderType.GROQ.name());
        providers.add(ProviderType.GEMINI.name());
        providers.add(ProviderType.MISTRAL.name());
        providers.add(ProviderType.LOCAL.name());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, providers);
        binding.providerDropdown.setAdapter(adapter);

        currentSelectedProvider = keyStore.getSelectedProvider();
        binding.providerDropdown.setText(currentSelectedProvider, false);

        updateKeyHint(currentSelectedProvider);
        loadSavedModel(currentSelectedProvider);

        binding.providerDropdown.setOnItemClickListener((parent, view, position, id) -> {
            currentSelectedProvider = providers.get(position);
            updateKeyHint(currentSelectedProvider);
            binding.apiKeyInput.setText("");
            binding.verifyStatusText.setVisibility(View.GONE);
            binding.modelDropdownLayout.setVisibility(View.GONE);
            currentSelectedModel = null;
            loadSavedModel(currentSelectedProvider);
        });

        binding.verifyKeyButton.setOnClickListener(v -> {
            if ("LOCAL".equals(currentSelectedProvider)) {
                startActivity(new android.content.Intent(this, LocalModelsActivity.class));
            } else {
                verifyApiKey();
            }
        });

        binding.modelDropdown.setOnItemClickListener((parent, view, position, id) -> {
            currentSelectedModel = (String) parent.getItemAtPosition(position);
        });

        binding.saveKeyButton.setOnClickListener(v -> {
            keyStore.setSelectedProvider(currentSelectedProvider);
            if ("LOCAL".equals(currentSelectedProvider)) {
                Toast.makeText(this, "Switched to LOCAL provider", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String key = binding.apiKeyInput.getText() != null
                    ? binding.apiKeyInput.getText().toString().trim()
                    : "";
            
            // Save model selection if one was chosen
            if (currentSelectedModel != null && !currentSelectedModel.isEmpty()) {
                keyStore.setSelectedModel(currentSelectedProvider, currentSelectedModel);
            }
            
            if (!key.isEmpty()) {
                keyStore.saveKey(currentSelectedProvider, key);
                binding.apiKeyInput.setText("");
                Toast.makeText(this, currentSelectedProvider + " key saved and selected", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                // If key is empty but they clicked save, just change the active provider
                if (keyStore.hasKey(currentSelectedProvider)) {
                    Toast.makeText(this, "Switched to " + currentSelectedProvider, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Please enter an API key for " + currentSelectedProvider, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateKeyHint(String provider) {
        if ("LOCAL".equals(provider)) {
            binding.apiKeyInputLayout.setVisibility(View.GONE);
            binding.verifyKeyButton.setText("Configure & Download Local Models");
            binding.saveKeyButton.setText("Activate Local LLM");
        } else {
            binding.apiKeyInputLayout.setVisibility(View.VISIBLE);
            binding.verifyKeyButton.setText(getString(com.sudeepkudari.ferry.R.string.verify_key));
            binding.saveKeyButton.setText(getString(com.sudeepkudari.ferry.R.string.save_key));
            if (keyStore.hasKey(provider)) {
                binding.apiKeyInputLayout.setHint("Key saved for " + provider + " — enter a new one to replace");
            } else {
                binding.apiKeyInputLayout.setHint(provider + " API Key");
            }
        }
    }

    /** If the user previously saved a model for this provider, show it. */
    private void loadSavedModel(String provider) {
        String savedModel = keyStore.getSelectedModel(provider);
        if (savedModel != null && !savedModel.isEmpty()) {
            currentSelectedModel = savedModel;
        }
    }

    /**
     * Resolves the API key to verify: uses the text field input if present,
     * otherwise falls back to the already-saved key for the selected provider.
     */
    private String resolveKeyToVerify() {
        String inputKey = binding.apiKeyInput.getText() != null
                ? binding.apiKeyInput.getText().toString().trim()
                : "";
        if (!inputKey.isEmpty()) {
            return inputKey;
        }
        return keyStore.getKey(currentSelectedProvider);
    }

    private void verifyApiKey() {
        String key = resolveKeyToVerify();
        if (key == null || key.isEmpty()) {
            showVerifyStatus("No key to verify — enter one or save it first.", false);
            return;
        }

        binding.verifyKeyButton.setEnabled(false);
        binding.verifyKeyButton.setText("Verifying…");
        binding.verifyStatusText.setVisibility(View.GONE);
        binding.modelDropdownLayout.setVisibility(View.GONE);

        new Thread(() -> {
            VerifyResult result = performVerification(currentSelectedProvider, key);
            runOnUiThread(() -> {
                binding.verifyKeyButton.setEnabled(true);
                binding.verifyKeyButton.setText(getString(com.sudeepkudari.ferry.R.string.verify_key));
                showVerifyStatus(result.message, result.success);
                
                if (result.success && result.models != null && !result.models.isEmpty()) {
                    showModelDropdown(result.models);
                }
            });
        }).start();
    }

    private void showVerifyStatus(String message, boolean success) {
        binding.verifyStatusText.setVisibility(View.VISIBLE);
        binding.verifyStatusText.setText(message);
        binding.verifyStatusText.setTextColor(success ? Color.parseColor("#2E7D32") : Color.parseColor("#C62828"));
    }

    private void showModelDropdown(List<String> models) {
        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, models);
        binding.modelDropdown.setAdapter(modelAdapter);
        binding.modelDropdownLayout.setVisibility(View.VISIBLE);

        // Pre-select the previously saved model if it exists in the list
        String savedModel = keyStore.getSelectedModel(currentSelectedProvider);
        if (savedModel != null && models.contains(savedModel)) {
            binding.modelDropdown.setText(savedModel, false);
            currentSelectedModel = savedModel;
        } else {
            // Default to the first model
            binding.modelDropdown.setText(models.get(0), false);
            currentSelectedModel = models.get(0);
        }
    }

    private VerifyResult performVerification(String provider, String key) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();

        try {
            switch (provider) {
                case "OPENAI":
                    return verifyAndFetchModels(client, "https://api.openai.com/v1/models", key, "OpenAI");
                case "GROQ":
                    return verifyAndFetchModels(client, "https://api.groq.com/openai/v1/models", key, "Groq");
                case "MISTRAL":
                    return verifyAndFetchModels(client, "https://api.mistral.ai/v1/models", key, "Mistral");
                case "GEMINI":
                    return verifyAndFetchGeminiModels(client, key);
                case "ANTHROPIC":
                default:
                    return verifyClaude(client, key);
            }
        } catch (Exception e) {
            return new VerifyResult(false, "Connection failed: " + e.getMessage(), null);
        }
    }

    /** OpenAI / Groq: GET /v1/models — validates key AND returns model list. */
    private VerifyResult verifyAndFetchModels(OkHttpClient client, String url, String key, String name) throws Exception {
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + key)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                List<String> models = parseOpenAIModels(responseBody);
                Collections.sort(models);
                return new VerifyResult(true, "✓ " + name + " key is valid! (" + models.size() + " models available)", models);
            } else if (response.code() == 401) {
                return new VerifyResult(false, "✗ Invalid API key for " + name + ".", null);
            } else {
                String body = response.body() != null ? response.body().string() : "";
                return new VerifyResult(false, "✗ " + name + " returned HTTP " + response.code() + ": " + truncate(body, 120), null);
            }
        }
    }

    /** Parse model IDs from OpenAI-compatible /v1/models response. */
    private List<String> parseOpenAIModels(String responseBody) {
        List<String> models = new ArrayList<>();
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray data = json.getAsJsonArray("data");
            if (data != null) {
                for (int i = 0; i < data.size(); i++) {
                    JsonObject model = data.get(i).getAsJsonObject();
                    String id = model.get("id").getAsString();
                    models.add(id);
                }
            }
        } catch (Exception e) {
            // If parsing fails, return empty
        }
        return models;
    }

    /** Anthropic: POST /v1/messages with minimal payload to test auth.
     *  Anthropic doesn't have a models list endpoint, so we return known models. */
    private VerifyResult verifyClaude(OkHttpClient client, String key) throws Exception {
        String json = "{\"model\":\"claude-sonnet-4-20250514\",\"max_tokens\":1,\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}";
        Request request = new Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .header("x-api-key", key)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .post(RequestBody.create(json, MediaType.get("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            // Anthropic known models — no list API available
            List<String> knownModels = Arrays.asList(
                    "claude-sonnet-4-20250514",
                    "claude-opus-4-20250514",
                    "claude-3-5-haiku-20241022",
                    "claude-3-5-sonnet-20241022"
            );
            
            if (response.isSuccessful()) {
                return new VerifyResult(true, "✓ Anthropic key is valid!", knownModels);
            } else if (response.code() == 401) {
                return new VerifyResult(false, "✗ Invalid API key for Anthropic.", null);
            } else {
                // Many non-401 errors (like 400 overloaded) still mean the key itself is valid
                String body = response.body() != null ? response.body().string() : "";
                if (response.code() == 529 || response.code() == 429 || response.code() == 400) {
                    return new VerifyResult(true, "✓ Anthropic key is valid! (API returned " + response.code() + " but auth passed)", knownModels);
                }
                return new VerifyResult(false, "✗ Anthropic returned HTTP " + response.code() + ": " + truncate(body, 120), null);
            }
        }
    }

    /** Gemini: GET /v1beta/models?key=... — validates key AND returns model list. */
    private VerifyResult verifyAndFetchGeminiModels(OkHttpClient client, String key) throws Exception {
        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models?key=" + key)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                List<String> models = parseGeminiModels(responseBody);
                Collections.sort(models);
                return new VerifyResult(true, "✓ Gemini key is valid! (" + models.size() + " models available)", models);
            } else if (response.code() == 400 || response.code() == 403) {
                return new VerifyResult(false, "✗ Invalid API key for Gemini.", null);
            } else {
                String body = response.body() != null ? response.body().string() : "";
                return new VerifyResult(false, "✗ Gemini returned HTTP " + response.code() + ": " + truncate(body, 120), null);
            }
        }
    }

    /** Parse model names from Gemini /v1beta/models response. 
     *  Filters to only generateContent-capable models. */
    private List<String> parseGeminiModels(String responseBody) {
        List<String> models = new ArrayList<>();
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray modelsArray = json.getAsJsonArray("models");
            if (modelsArray != null) {
                for (int i = 0; i < modelsArray.size(); i++) {
                    JsonObject model = modelsArray.get(i).getAsJsonObject();
                    // Check if model supports generateContent
                    if (model.has("supportedGenerationMethods")) {
                        JsonArray methods = model.getAsJsonArray("supportedGenerationMethods");
                        boolean supportsGenerate = false;
                        for (int j = 0; j < methods.size(); j++) {
                            if ("generateContent".equals(methods.get(j).getAsString())) {
                                supportsGenerate = true;
                                break;
                            }
                        }
                        if (supportsGenerate) {
                            // Name comes as "models/gemini-1.5-pro" — strip the "models/" prefix
                            String name = model.get("name").getAsString();
                            if (name.startsWith("models/")) {
                                name = name.substring("models/".length());
                            }
                            models.add(name);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // If parsing fails, return empty
        }
        return models;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }

    private static class VerifyResult {
        final boolean success;
        final String message;
        final List<String> models;

        VerifyResult(boolean success, String message, List<String> models) {
            this.success = success;
            this.message = message;
            this.models = models;
        }
    }
}
