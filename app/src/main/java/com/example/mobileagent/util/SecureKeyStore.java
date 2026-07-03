package com.example.mobileagent.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecureKeyStore {

    private static final String PREFS_FILE = "secure_provider_keys";
    private static final String KEY_ANTHROPIC = "anthropic_api_key";
    private static final String KEY_OPENAI = "openai_api_key";
    private static final String KEY_GROQ = "groq_api_key";
    private static final String KEY_GEMINI = "gemini_api_key";
    
    private static final String KEY_SELECTED_PROVIDER = "selected_provider";
    private static final String KEY_MODEL_PREFIX = "selected_model_";
    
    // Local LLM settings
    private static final String KEY_LOCAL_SERVER_URL = "local_server_url";
    private static final String KEY_LOCAL_SELECTED_MODEL_ID = "local_selected_model_id";
    private static final String DEFAULT_LOCAL_SERVER_URL = "http://localhost:8080";

    private final SharedPreferences prefs;

    public SecureKeyStore(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            prefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Failed to initialize encrypted key storage", e);
        }
    }
    
    public void setSelectedProvider(String providerName) {
        prefs.edit().putString(KEY_SELECTED_PROVIDER, providerName).apply();
    }
    
    public String getSelectedProvider() {
        return prefs.getString(KEY_SELECTED_PROVIDER, "ANTHROPIC");
    }

    public void saveKey(String provider, String key) {
        switch (provider) {
            case "ANTHROPIC": prefs.edit().putString(KEY_ANTHROPIC, key).apply(); break;
            case "OPENAI": prefs.edit().putString(KEY_OPENAI, key).apply(); break;
            case "GROQ": prefs.edit().putString(KEY_GROQ, key).apply(); break;
            case "GEMINI": prefs.edit().putString(KEY_GEMINI, key).apply(); break;
        }
    }

    public String getKey(String provider) {
        switch (provider) {
            case "ANTHROPIC": return prefs.getString(KEY_ANTHROPIC, null);
            case "OPENAI": return prefs.getString(KEY_OPENAI, null);
            case "GROQ": return prefs.getString(KEY_GROQ, null);
            case "GEMINI": return prefs.getString(KEY_GEMINI, null);
            default: return null;
        }
    }

    public boolean hasKey(String provider) {
        // LOCAL provider doesn't need an API key
        if ("LOCAL".equals(provider)) return true;
        String key = getKey(provider);
        return key != null && !key.isEmpty();
    }

    public void setSelectedModel(String provider, String model) {
        prefs.edit().putString(KEY_MODEL_PREFIX + provider, model).apply();
    }

    public String getSelectedModel(String provider) {
        return prefs.getString(KEY_MODEL_PREFIX + provider, null);
    }

    // ── Local LLM settings ──────────────────────────────────────

    public void setLocalServerUrl(String url) {
        prefs.edit().putString(KEY_LOCAL_SERVER_URL, url).apply();
    }

    public String getLocalServerUrl() {
        return prefs.getString(KEY_LOCAL_SERVER_URL, DEFAULT_LOCAL_SERVER_URL);
    }

    public void setLocalSelectedModelId(String modelId) {
        prefs.edit().putString(KEY_LOCAL_SELECTED_MODEL_ID, modelId).apply();
    }

    public String getLocalSelectedModelId() {
        return prefs.getString(KEY_LOCAL_SELECTED_MODEL_ID, null);
    }

    // Legacy support methods during migration
    public void saveAnthropicKey(String key) { saveKey("ANTHROPIC", key); }
    public String getAnthropicKey() { return getKey("ANTHROPIC"); }
    public boolean hasAnthropicKey() { return hasKey("ANTHROPIC"); }
    public void clearAnthropicKey() { prefs.edit().remove(KEY_ANTHROPIC).apply(); }
}
