package com.example.mobileagent.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobileagent.databinding.ActivitySettingsBinding;
import com.example.mobileagent.util.SecureKeyStore;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SecureKeyStore keyStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        keyStore = new SecureKeyStore(this);
        if (keyStore.hasAnthropicKey()) {
            binding.apiKeyInput.setHint("Key saved — enter a new one to replace it");
        }

        binding.saveKeyButton.setOnClickListener(v -> {
            String key = binding.apiKeyInput.getText() != null
                    ? binding.apiKeyInput.getText().toString().trim()
                    : "";
            if (key.isEmpty()) {
                Toast.makeText(this, "Enter a key first", Toast.LENGTH_SHORT).show();
                return;
            }
            keyStore.saveAnthropicKey(key);
            binding.apiKeyInput.setText("");
            Toast.makeText(this, "Key saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
