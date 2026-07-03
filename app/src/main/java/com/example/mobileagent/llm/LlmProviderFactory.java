package com.example.mobileagent.llm;

import android.content.Context;
import com.example.mobileagent.util.SecureKeyStore;

public class LlmProviderFactory {
    
    public static LlmProvider getActiveProvider(Context context) {
        SecureKeyStore keyStore = new SecureKeyStore(context);
        String selected = keyStore.getSelectedProvider();
        
        switch (selected) {
            case "OPENAI":
                return new OpenAICompatibleProvider(
                        "https://api.openai.com/v1/chat/completions",
                        keyStore.getKey("OPENAI"),
                        "gpt-4o"
                );
            case "GROQ":
                return new OpenAICompatibleProvider(
                        "https://api.groq.com/openai/v1/chat/completions",
                        keyStore.getKey("GROQ"),
                        "openai/gpt-oss-120b"
                );
            case "GEMINI":
                return new GeminiProvider(keyStore.getKey("GEMINI"));
            case "ANTHROPIC":
            default:
                return new ClaudeProvider(keyStore.getKey("ANTHROPIC"));
        }
    }
}
