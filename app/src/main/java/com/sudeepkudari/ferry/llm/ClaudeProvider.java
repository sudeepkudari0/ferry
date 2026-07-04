package com.sudeepkudari.ferry.llm;

import com.sudeepkudari.ferry.agent.Action;
import com.sudeepkudari.ferry.net.DeviceState;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * BYOK Claude implementation of LlmProvider. The user supplies their own
 * Anthropic API key (stored via SecureKeyStore, never hardcoded or bundled),
 * and this class calls the public /v1/messages endpoint directly from the
 * device, using tool use to force a structured, parseable action back.
 *
 * NOTE: model id is a constructor parameter, not hardcoded, since current
 * model names change over time — check docs.claude.com for the current
 * model string rather than assuming this default stays accurate.
 */
public class ClaudeProvider implements LlmProvider {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String DEFAULT_MODEL = "claude-sonnet-5";

    private final String apiKey;
    private final String model;
    private final OkHttpClient httpClient;

    public ClaudeProvider(String apiKey) {
        this(apiKey, DEFAULT_MODEL);
    }

    public ClaudeProvider(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String getProviderName() {
        return model;
    }

    @Override
    public Action decideNextAction(String task, DeviceState state, List<Action> history) throws IOException {
        JsonObject requestBody = buildRequest(task, state, history);

        RequestBody body = RequestBody.create(requestBody.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(API_URL)
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String errBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Claude API call failed: HTTP " + response.code() + " " + errBody);
            }
            JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            return parseActionFromResponse(json);
        }
    }

    private JsonObject buildRequest(String task, DeviceState state, List<Action> history) {
        JsonObject req = new JsonObject();
        req.addProperty("model", model);
        req.addProperty("max_tokens", 1024);

        StringBuilder historyText = new StringBuilder();
        for (Action a : history) {
            historyText.append("- ").append(a).append("\n");
        }

        String userText = "Task: " + task + "\n\n"
                + "Current screen state (accessibility tree):\n" + state.accessibilityTreeJson + "\n\n"
                + "Actions taken so far:\n" + (historyText.length() > 0 ? historyText : "(none yet)\n") + "\n"
                + "Decide the single next action to move the task forward. "
                + "Call the next_action tool exactly once with your decision.";

        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userText);
        messages.add(userMsg);
        req.add("messages", messages);

        req.add("tools", buildToolSchema());
        req.add("tool_choice", toolChoiceForced());

        return req;
    }

    /** Forces the model to always respond via the structured tool rather than free text. */
    private JsonObject toolChoiceForced() {
        JsonObject choice = new JsonObject();
        choice.addProperty("type", "tool");
        choice.addProperty("name", "next_action");
        return choice;
    }

    private JsonArray buildToolSchema() {
        JsonObject tool = new JsonObject();
        tool.addProperty("name", "next_action");
        tool.addProperty("description", "Report the single next UI action to perform on the device.");

        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");

        JsonObject properties = new JsonObject();

        JsonObject actionType = new JsonObject();
        actionType.addProperty("type", "string");
        JsonArray enumVals = new JsonArray();
        for (Action.Type t : Action.Type.values()) enumVals.add(t.name());
        actionType.add("enum", enumVals);
        properties.add("action_type", actionType);

        properties.add("node_id", simpleStringProp("Target node id, required for TAP and TYPE_TEXT."));
        properties.add("text", simpleStringProp("Text to type, required for TYPE_TEXT."));
        properties.add("x", simpleIntProp("X coordinate, required for TAP_XY and SWIPE."));
        properties.add("y", simpleIntProp("Y coordinate, required for TAP_XY and SWIPE."));
        properties.add("target", simpleStringProp("Android package name or deep link URI, required for LAUNCH_APP. Use the real package name, e.g. 'com.android.chrome' for Chrome, 'com.google.android.youtube' for YouTube, 'com.whatsapp' for WhatsApp."));
        properties.add("reasoning", simpleStringProp("One sentence explaining why this action was chosen."));

        schema.add("properties", properties);

        JsonArray required = new JsonArray();
        required.add("action_type");
        required.add("reasoning");
        schema.add("required", required);

        tool.add("input_schema", schema);

        JsonArray tools = new JsonArray();
        tools.add(tool);
        return tools;
    }

    private JsonObject simpleStringProp(String description) {
        JsonObject p = new JsonObject();
        p.addProperty("type", "string");
        p.addProperty("description", description);
        return p;
    }

    private JsonObject simpleIntProp(String description) {
        JsonObject p = new JsonObject();
        p.addProperty("type", "integer");
        p.addProperty("description", description);
        return p;
    }

    private Action parseActionFromResponse(JsonObject response) throws IOException {
        JsonArray content = response.getAsJsonArray("content");
        for (int i = 0; i < content.size(); i++) {
            JsonObject block = content.get(i).getAsJsonObject();
            if ("tool_use".equals(block.get("type").getAsString())) {
                JsonObject input = block.getAsJsonObject("input");
                String typeStr = input.get("action_type").getAsString();
                Action.Type type = Action.Type.valueOf(typeStr);
                String reasoning = input.has("reasoning") ? input.get("reasoning").getAsString() : "";

                Action.Builder builder = new Action.Builder(type).reasoning(reasoning);
                if (input.has("node_id")) builder.nodeId(input.get("node_id").getAsString());
                if (input.has("text")) builder.text(input.get("text").getAsString());
                if (input.has("x") && input.has("y")) {
                    builder.coords(input.get("x").getAsInt(), input.get("y").getAsInt());
                }
                if (input.has("target")) builder.target(input.get("target").getAsString());
                return builder.build();
            }
        }
        throw new IOException("Claude response contained no tool_use block — cannot determine next action.");
    }
}
