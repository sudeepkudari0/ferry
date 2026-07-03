package com.example.mobileagent.llm;

import com.example.mobileagent.agent.Action;
import com.example.mobileagent.net.DeviceState;
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

public class GeminiProvider implements LlmProvider {

    private static final String API_URL_FORMAT = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    private static final String DEFAULT_MODEL = "gemini-1.5-pro-latest";

    private final String apiKey;
    private final String model;
    private final OkHttpClient httpClient;

    public GeminiProvider(String apiKey) {
        this(apiKey, DEFAULT_MODEL);
    }

    public GeminiProvider(String apiKey, String model) {
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
                .url(String.format(API_URL_FORMAT, model, apiKey))
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String errBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Gemini API call failed: HTTP " + response.code() + " " + errBody);
            }
            JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            return parseActionFromResponse(json);
        }
    }

    private JsonObject buildRequest(String task, DeviceState state, List<Action> history) {
        JsonObject req = new JsonObject();

        StringBuilder historyText = new StringBuilder();
        for (Action a : history) {
            historyText.append("- ").append(a).append("\n");
        }

        String userText = "Task: " + task + "\n\n"
                + "Current screen state (accessibility tree):\n" + state.accessibilityTreeJson + "\n\n"
                + "Actions taken so far:\n" + (historyText.length() > 0 ? historyText : "(none yet)\n") + "\n"
                + "Decide the single next action to move the task forward. "
                + "Call the next_action tool exactly once with your decision.";

        JsonArray contents = new JsonArray();
        JsonObject userContent = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", userText);
        parts.add(textPart);
        userContent.add("parts", parts);
        contents.add(userContent);
        
        req.add("contents", contents);

        req.add("tools", buildToolSchema());
        
        JsonObject toolConfig = new JsonObject();
        JsonObject functionCallingConfig = new JsonObject();
        functionCallingConfig.addProperty("mode", "ANY");
        JsonArray allowed = new JsonArray();
        allowed.add("next_action");
        functionCallingConfig.add("allowedFunctionNames", allowed);
        toolConfig.add("functionCallingConfig", functionCallingConfig);
        
        req.add("toolConfig", toolConfig);

        return req;
    }

    private JsonArray buildToolSchema() {
        JsonObject tool = new JsonObject();
        JsonArray functionDeclarations = new JsonArray();

        JsonObject functionDef = new JsonObject();
        functionDef.addProperty("name", "next_action");
        functionDef.addProperty("description", "Report the single next UI action to perform on the device.");

        JsonObject schema = new JsonObject();
        schema.addProperty("type", "OBJECT");

        JsonObject properties = new JsonObject();

        JsonObject actionType = new JsonObject();
        actionType.addProperty("type", "STRING");
        // Note: Gemini uses uppercase enum for types
        JsonArray enumVals = new JsonArray();
        for (Action.Type t : Action.Type.values()) enumVals.add(t.name());
        actionType.add("enum", enumVals);
        properties.add("action_type", actionType);

        properties.add("node_id", simpleStringProp("Target node id, required for TAP and TYPE_TEXT."));
        properties.add("text", simpleStringProp("Text to type, required for TYPE_TEXT."));
        properties.add("x", simpleIntProp("X coordinate, required for TAP_XY and SWIPE."));
        properties.add("y", simpleIntProp("Y coordinate, required for TAP_XY and SWIPE."));
        properties.add("target", simpleStringProp("Package name or deep link, required for LAUNCH_APP."));
        properties.add("reasoning", simpleStringProp("One sentence explaining why this action was chosen."));

        schema.add("properties", properties);

        JsonArray required = new JsonArray();
        required.add("action_type");
        required.add("reasoning");
        schema.add("required", required);

        functionDef.add("parameters", schema);
        functionDeclarations.add(functionDef);
        tool.add("functionDeclarations", functionDeclarations);

        JsonArray tools = new JsonArray();
        tools.add(tool);
        return tools;
    }

    private JsonObject simpleStringProp(String description) {
        JsonObject p = new JsonObject();
        p.addProperty("type", "STRING");
        p.addProperty("description", description);
        return p;
    }

    private JsonObject simpleIntProp(String description) {
        JsonObject p = new JsonObject();
        p.addProperty("type", "INTEGER");
        p.addProperty("description", description);
        return p;
    }

    private Action parseActionFromResponse(JsonObject response) throws IOException {
        if (response.has("candidates")) {
            JsonArray candidates = response.getAsJsonArray("candidates");
            if (candidates.size() > 0) {
                JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                if (content != null && content.has("parts")) {
                    JsonArray parts = content.getAsJsonArray("parts");
                    for (int i = 0; i < parts.size(); i++) {
                        JsonObject part = parts.get(i).getAsJsonObject();
                        if (part.has("functionCall")) {
                            JsonObject functionCall = part.getAsJsonObject("functionCall");
                            if ("next_action".equals(functionCall.get("name").getAsString())) {
                                JsonObject args = functionCall.getAsJsonObject("args");
                                
                                String typeStr = args.get("action_type").getAsString();
                                Action.Type type = Action.Type.valueOf(typeStr);
                                String reasoning = args.has("reasoning") ? args.get("reasoning").getAsString() : "";

                                Action.Builder builder = new Action.Builder(type).reasoning(reasoning);
                                if (args.has("node_id")) builder.nodeId(args.get("node_id").getAsString());
                                if (args.has("text")) builder.text(args.get("text").getAsString());
                                if (args.has("x") && args.has("y")) {
                                    builder.coords(args.get("x").getAsInt(), args.get("y").getAsInt());
                                }
                                if (args.has("target")) builder.target(args.get("target").getAsString());
                                return builder.build();
                            }
                        }
                    }
                }
            }
        }
        throw new IOException("Response contained no valid functionCall for next_action.");
    }
}
