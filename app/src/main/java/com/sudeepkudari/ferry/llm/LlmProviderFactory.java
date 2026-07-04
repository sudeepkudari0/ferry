package com.sudeepkudari.ferry.llm;

import android.content.Context;
import com.sudeepkudari.ferry.util.SecureKeyStore;

public class LlmProviderFactory {
    
    public static LlmProvider getActiveProvider(Context context) {
        SecureKeyStore keyStore = new SecureKeyStore(context);
        String selected = keyStore.getSelectedProvider();
        String savedModel = keyStore.getSelectedModel(selected);
        
        switch (selected) {
            case "OPENAI": {
                String model = savedModel != null ? savedModel : "gpt-4o";
                return new OpenAICompatibleProvider(
                        "https://api.openai.com/v1/chat/completions",
                        keyStore.getKey("OPENAI"),
                        model
                );
            }
            case "GROQ": {
                String model = savedModel != null ? savedModel : "llama-3.3-70b-versatile";
                return new OpenAICompatibleProvider(
                        "https://api.groq.com/openai/v1/chat/completions",
                        keyStore.getKey("GROQ"),
                        model
                );
            }
            case "GEMINI": {
                String model = savedModel != null ? savedModel : "gemini-1.5-pro-latest";
                return new GeminiProvider(keyStore.getKey("GEMINI"), model);
            }
            case "MISTRAL": {
                String model = savedModel != null ? savedModel : "mistral-large-latest";
                return new OpenAICompatibleProvider(
                        "https://api.mistral.ai/v1/chat/completions",
                        keyStore.getKey("MISTRAL"),
                        model
                );
            }
            case "LOCAL": {
                String baseUrl = keyStore.getLocalServerUrl();
                String model = savedModel != null ? savedModel : "default";
                return new LocalLlmProvider(baseUrl, model);
            }
            case "ANTHROPIC":
            default: {
                String model = savedModel != null ? savedModel : "claude-sonnet-4-20250514";
                return new ClaudeProvider(keyStore.getKey("ANTHROPIC"), model);
            }
        }
    }

    /** Check if the selected provider requires an API key. */
    public static boolean requiresApiKey(String provider) {
        return !"LOCAL".equals(provider);
    }
}
