package com.example.mobileagent.ui;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobileagent.databinding.ActivitySettingsBinding;
import com.example.mobileagent.llm.ProviderType;
import com.example.mobileagent.util.SecureKeyStore;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SecureKeyStore keyStore;
    
    private String currentSelectedProvider;

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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, providers);
        binding.providerDropdown.setAdapter(adapter);

        currentSelectedProvider = keyStore.getSelectedProvider();
        binding.providerDropdown.setText(currentSelectedProvider, false);

        updateKeyHint(currentSelectedProvider);

        binding.providerDropdown.setOnItemClickListener((parent, view, position, id) -> {
            currentSelectedProvider = providers.get(position);
            updateKeyHint(currentSelectedProvider);
            binding.apiKeyInput.setText("");
        });

        binding.saveKeyButton.setOnClickListener(v -> {
            String key = binding.apiKeyInput.getText() != null
                    ? binding.apiKeyInput.getText().toString().trim()
                    : "";
            
            keyStore.setSelectedProvider(currentSelectedProvider);
            
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
        if (keyStore.hasKey(provider)) {
            binding.apiKeyInputLayout.setHint("Key saved for " + provider + " — enter a new one to replace");
        } else {
            binding.apiKeyInputLayout.setHint(provider + " API Key");
        }
    }
}
