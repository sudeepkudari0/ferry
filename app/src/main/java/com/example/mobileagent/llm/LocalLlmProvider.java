package com.example.mobileagent.llm;

import com.example.mobileagent.agent.Action;
import com.example.mobileagent.net.DeviceState;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * LlmProvider that talks to a local OpenAI-compatible server running on-device.
 * 
 * Supports any server exposing /v1/chat/completions:
 *   - llama.cpp server (llama-server)
 *   - Ollama (via its OpenAI compatibility layer)
 *   - LM Studio
 *   - Any other local LLM server app
 * 
 * Unlike cloud providers, local models (especially small ones ≤7B) are unreliable
 * at tool/function calling. This provider uses structured JSON prompting instead:
 * it instructs the model to respond with a JSON object, then parses the action
 * from the text response. This is far more reliable with quantized local models.
 */
public class LocalLlmProvider implements LlmProvider {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    private final String baseUrl;
    private final String model;
    private final OkHttpClient httpClient;

    public LocalLlmProvider(String baseUrl, String model) {
        this.baseUrl = baseUrl != null && !baseUrl.isEmpty() ? baseUrl : DEFAULT_BASE_URL;
        this.model = model != null && !model.isEmpty() ? model : "default";
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)  // Local inference can be slow
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String getProviderName() {
        return "local:" + model;
    }

    @Override
    public Action decideNextAction(String task, DeviceState state, List<Action> history) throws IOException {
        JsonObject requestBody = buildRequest(task, state, history);

        String apiUrl = baseUrl.endsWith("/")
                ? baseUrl + "v1/chat/completions"
                : baseUrl + "/v1/chat/completions";

        RequestBody body = RequestBody.create(requestBody.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(apiUrl)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String errBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Local LLM server call failed: HTTP " + response.code() + " " + errBody);
            }
            JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            return parseActionFromResponse(json);
        } catch (java.net.ConnectException e) {
            throw new IOException("Cannot connect to local LLM server at " + baseUrl +
                    ". Make sure Ollama/llama.cpp server is running. Error: " + e.getMessage());
        }
    }

    private JsonObject buildRequest(String task, DeviceState state, List<Action> history) {
        JsonObject req = new JsonObject();
        req.addProperty("model", model);
        req.addProperty("temperature", 0.1);  // Low temperature for deterministic actions
        req.addProperty("max_tokens", 512);

        // Build the action type enum string
        StringBuilder actionTypes = new StringBuilder();
        for (Action.Type t : Action.Type.values()) {
            actionTypes.append(t.name()).append(", ");
        }

        StringBuilder historyText = new StringBuilder();
        for (Action a : history) {
            historyText.append("- ").append(a).append("\n");
        }

        // System prompt: instruct the model to respond ONLY with JSON
        String systemPrompt = "You are a mobile device automation agent. You analyze the current screen state "
                + "and decide the single next UI action to perform.\n\n"
                + "You MUST respond with ONLY a valid JSON object (no markdown, no explanation, no extra text).\n"
                + "The JSON object must have these fields:\n"
                + "- \"action_type\": one of [" + actionTypes + "]\n"
                + "- \"reasoning\": a brief explanation of why you chose this action\n"
                + "- \"node_id\": (optional) target node id for TAP or TYPE_TEXT actions\n"
                + "- \"text\": (optional) text to type for TYPE_TEXT actions\n"
                + "- \"x\": (optional) X coordinate for TAP_XY or SWIPE actions\n"
                + "- \"y\": (optional) Y coordinate for TAP_XY or SWIPE actions\n"
                + "- \"target\": (optional) package name or deep link for LAUNCH_APP\n\n"
                + "Example response:\n"
                + "{\"action_type\": \"TAP\", \"node_id\": \"node_42\", \"reasoning\": \"Tap the search button to begin search\"}\n\n"
                + "IMPORTANT: Output ONLY the JSON object. No other text.";

        String userText = "Task: " + task + "\n\n"
                + "Current screen state (accessibility tree):\n" + state.accessibilityTreeJson + "\n\n"
                + "Actions taken so far:\n" + (historyText.length() > 0 ? historyText : "(none yet)\n") + "\n"
                + "Respond with ONLY the JSON for the next action.";

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userText);
        messages.add(userMsg);

        req.add("messages", messages);

        // Request JSON response format if the server supports it (llama.cpp and Ollama do)
        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        req.add("response_format", responseFormat);

        return req;
    }

    /**
     * Parse the action from the LLM's text response. Since local models respond with
     * plain text (not tool calls), we extract JSON from the response content.
     */
    private Action parseActionFromResponse(JsonObject response) throws IOException {
        JsonArray choices = response.getAsJsonArray("choices");
        if (choices == null || choices.size() == 0) {
            throw new IOException("No choices returned from local LLM server.");
        }

        JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        if (message == null || !message.has("content")) {
            throw new IOException("No content in local LLM response.");
        }

        String content = message.get("content").getAsString().trim();
        JsonObject actionJson = extractJsonFromText(content);

        if (actionJson == null || !actionJson.has("action_type")) {
            throw new IOException("Local LLM did not return a valid action JSON. Raw response: "
                    + truncate(content, 200));
        }

        try {
            String typeStr = actionJson.get("action_type").getAsString().toUpperCase().trim();
            Action.Type type = Action.Type.valueOf(typeStr);
            String reasoning = actionJson.has("reasoning")
                    ? actionJson.get("reasoning").getAsString() : "";

            Action.Builder builder = new Action.Builder(type).reasoning(reasoning);
            if (actionJson.has("node_id")) builder.nodeId(actionJson.get("node_id").getAsString());
            if (actionJson.has("text")) builder.text(actionJson.get("text").getAsString());
            if (actionJson.has("x") && actionJson.has("y")) {
                builder.coords(actionJson.get("x").getAsInt(), actionJson.get("y").getAsInt());
            }
            if (actionJson.has("target")) builder.target(actionJson.get("target").getAsString());
            return builder.build();
        } catch (IllegalArgumentException e) {
            throw new IOException("Local LLM returned unknown action_type: "
                    + actionJson.get("action_type").getAsString());
        }
    }

    /**
     * Extract a JSON object from the model's text output. Handles common cases:
     * 1. Clean JSON: the entire response is valid JSON
     * 2. Wrapped JSON: JSON wrapped in markdown code fences (```json ... ```)
     * 3. Mixed text: JSON embedded somewhere in a text response
     */
    private JsonObject extractJsonFromText(String text) {
        // Try 1: parse the whole string as JSON directly
        try {
            return JsonParser.parseString(text).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException ignored) {
        }

        // Try 2: extract from markdown code fence
        Pattern codeFence = Pattern.compile("```(?:json)?\\s*\\n?(\\{.*?})\\s*```", Pattern.DOTALL);
        Matcher fenceMatcher = codeFence.matcher(text);
        if (fenceMatcher.find()) {
            try {
                return JsonParser.parseString(fenceMatcher.group(1)).getAsJsonObject();
            } catch (JsonSyntaxException | IllegalStateException ignored) {
            }
        }

        // Try 3: find the first { ... } block
        int braceStart = text.indexOf('{');
        int braceEnd = text.lastIndexOf('}');
        if (braceStart >= 0 && braceEnd > braceStart) {
            try {
                return JsonParser.parseString(text.substring(braceStart, braceEnd + 1)).getAsJsonObject();
            } catch (JsonSyntaxException | IllegalStateException ignored) {
            }
        }

        return null;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }
}
