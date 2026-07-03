package com.example.mobileagent.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Stores BYOK API keys using Android Keystore-backed encryption at rest.
 * Keys never leave the device except as the Authorization header on requests
 * the user's own key authorizes — never logged, never bundled, never sent
 * anywhere but the provider's own API endpoint.
 */
public class SecureKeyStore {

    private static final String PREFS_FILE = "secure_provider_keys";
    private static final String KEY_ANTHROPIC = "anthropic_api_key";

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

    public void saveAnthropicKey(String key) {
        prefs.edit().putString(KEY_ANTHROPIC, key).apply();
    }

    public String getAnthropicKey() {
        return prefs.getString(KEY_ANTHROPIC, null);
    }

    public boolean hasAnthropicKey() {
        String key = getAnthropicKey();
        return key != null && !key.isEmpty();
    }

    public void clearAnthropicKey() {
        prefs.edit().remove(KEY_ANTHROPIC).apply();
    }
}
