package com.sudeepkudari.ferry.llm.local;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry of curated local models that are known to work well for the
 * mobile agent use case. These models are chosen for:
 * 
 * 1. Good instruction following / JSON output capability
 * 2. Reasonable size for mobile devices (≤ 8B parameters quantized)
 * 3. Available as GGUF from HuggingFace
 * 
 * Models are organized by RAM tier so the UI can recommend appropriate
 * models based on the user's device capabilities.
 */
public class LocalModelRegistry {

    private static final List<LocalModel> MODELS = new ArrayList<>();

    static {
        // ──────────────────────────────────────────────────────────
        // Tier 1: Ultra-light (4 GB RAM, ~1-2 GB download)
        // ──────────────────────────────────────────────────────────
        MODELS.add(new LocalModel.Builder("qwen3-1.7b-q4", "Qwen 3 1.7B")
                .description("Smallest capable model. Fast on most devices. Good for simple tasks.")
                .parameterCount("1.7B")
                .quantization("Q4_K_M")
                .fileSizeBytes(1_200_000_000L) // ~1.2 GB
                .recommendedRamGb(4)
                .fileName("Qwen3-1.7B-Q4_K_M.gguf")
                .downloadUrl("https://huggingface.co/Qwen/Qwen3-1.7B-GGUF/resolve/main/Qwen3-1.7B-Q4_K_M.gguf")
                .build());

        MODELS.add(new LocalModel.Builder("gemma3-1b-q8", "Gemma 3 1B")
                .description("Google's tiny model. Ultra-fast, good at simple tasks.")
                .parameterCount("1B")
                .quantization("Q8_0")
                .fileSizeBytes(1_100_000_000L) // ~1.1 GB
                .recommendedRamGb(4)
                .fileName("gemma-3-1b-it-Q8_0.gguf")
                .downloadUrl("https://huggingface.co/bartowski/gemma-3-1b-it-GGUF/resolve/main/gemma-3-1b-it-Q8_0.gguf")
                .build());

        // ──────────────────────────────────────────────────────────
        // Tier 2: Balanced (6-8 GB RAM, ~2-4 GB download)
        // ──────────────────────────────────────────────────────────
        MODELS.add(new LocalModel.Builder("qwen3-4b-q4", "Qwen 3 4B (Recommended)")
                .description("Best balance of speed and intelligence. Recommended for most devices.")
                .parameterCount("4B")
                .quantization("Q4_K_M")
                .fileSizeBytes(2_700_000_000L) // ~2.7 GB
                .recommendedRamGb(6)
                .fileName("Qwen3-4B-Q4_K_M.gguf")
                .downloadUrl("https://huggingface.co/Qwen/Qwen3-4B-GGUF/resolve/main/Qwen3-4B-Q4_K_M.gguf")
                .build());

        MODELS.add(new LocalModel.Builder("phi4-mini-q4", "Phi-4 Mini 3.8B")
                .description("Microsoft's small model. Great at reasoning and instruction following.")
                .parameterCount("3.8B")
                .quantization("Q4_K_M")
                .fileSizeBytes(2_400_000_000L) // ~2.4 GB
                .recommendedRamGb(6)
                .fileName("Phi-4-mini-instruct-Q4_K_M.gguf")
                .downloadUrl("https://huggingface.co/bartowski/Phi-4-mini-instruct-GGUF/resolve/main/Phi-4-mini-instruct-Q4_K_M.gguf")
                .build());

        MODELS.add(new LocalModel.Builder("gemma3-4b-q4", "Gemma 3 4B")
                .description("Google's 4B model. Strong at structured output and reasoning.")
                .parameterCount("4B")
                .quantization("Q4_K_M")
                .fileSizeBytes(2_500_000_000L) // ~2.5 GB
                .recommendedRamGb(6)
                .fileName("gemma-3-4b-it-Q4_K_M.gguf")
                .downloadUrl("https://huggingface.co/bartowski/gemma-3-4b-it-GGUF/resolve/main/gemma-3-4b-it-Q4_K_M.gguf")
                .build());

        // ──────────────────────────────────────────────────────────
        // Tier 3: High quality (12+ GB RAM, ~4-5 GB download)
        // ──────────────────────────────────────────────────────────
        MODELS.add(new LocalModel.Builder("qwen3-8b-q4", "Qwen 3 8B")
                .description("Most capable local model. Needs 12+ GB RAM. Best accuracy.")
                .parameterCount("8B")
                .quantization("Q4_K_M")
                .fileSizeBytes(5_000_000_000L) // ~5 GB
                .recommendedRamGb(12)
                .fileName("Qwen3-8B-Q4_K_M.gguf")
                .downloadUrl("https://huggingface.co/Qwen/Qwen3-8B-GGUF/resolve/main/Qwen3-8B-Q4_K_M.gguf")
                .build());

        MODELS.add(new LocalModel.Builder("llama3.2-3b-q4", "Llama 3.2 3B")
                .description("Meta's compact model. Good general-purpose capability.")
                .parameterCount("3B")
                .quantization("Q4_K_M")
                .fileSizeBytes(2_000_000_000L) // ~2 GB
                .recommendedRamGb(6)
                .fileName("Llama-3.2-3B-Instruct-Q4_K_M.gguf")
                .downloadUrl("https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf")
                .build());
    }

    /** Returns all available models, sorted by recommended RAM (lightest first). */
    public static List<LocalModel> getAllModels() {
        return Collections.unmodifiableList(MODELS);
    }

    /** Returns models suitable for the given RAM amount. */
    public static List<LocalModel> getModelsForRam(int availableRamGb) {
        List<LocalModel> suitable = new ArrayList<>();
        for (LocalModel model : MODELS) {
            if (model.getRecommendedRamGb() <= availableRamGb) {
                suitable.add(model);
            }
        }
        return suitable;
    }

    /** Find a model by its ID. */
    public static LocalModel findById(String id) {
        for (LocalModel model : MODELS) {
            if (model.getId().equals(id)) {
                return model;
            }
        }
        return null;
    }
}
