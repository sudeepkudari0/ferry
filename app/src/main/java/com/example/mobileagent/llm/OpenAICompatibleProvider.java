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

public class OpenAICompatibleProvider implements LlmProvider {

    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final OkHttpClient httpClient;

    public OpenAICompatibleProvider(String apiUrl, String apiKey, String model) {
        this.apiUrl = apiUrl;
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
                .url(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String errBody = response.body() != null ? response.body().string() : "";
                throw new IOException("OpenAI/Groq API call failed: HTTP " + response.code() + " " + errBody);
            }
            JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            return parseActionFromResponse(json);
        }
    }

    private JsonObject buildRequest(String task, DeviceState state, List<Action> history) {
        JsonObject req = new JsonObject();
        req.addProperty("model", model);

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
        
        JsonObject toolChoice = new JsonObject();
        toolChoice.addProperty("type", "function");
        JsonObject functionChoice = new JsonObject();
        functionChoice.addProperty("name", "next_action");
        toolChoice.add("function", functionChoice);
        req.add("tool_choice", toolChoice);

        return req;
    }

    private JsonArray buildToolSchema() {
        JsonObject tool = new JsonObject();
        tool.addProperty("type", "function");

        JsonObject functionDef = new JsonObject();
        functionDef.addProperty("name", "next_action");
        functionDef.addProperty("description", "Report the single next UI action to perform on the device.");

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
        properties.add("target", simpleStringProp("Package name or deep link, required for LAUNCH_APP."));
        properties.add("reasoning", simpleStringProp("One sentence explaining why this action was chosen."));

        schema.add("properties", properties);

        JsonArray required = new JsonArray();
        required.add("action_type");
        required.add("reasoning");
        schema.add("required", required);

        functionDef.add("parameters", schema);
        tool.add("function", functionDef);

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
        JsonArray choices = response.getAsJsonArray("choices");
        if (choices == null || choices.size() == 0) {
            throw new IOException("No choices returned from API.");
        }
        
        JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        if (message != null && message.has("tool_calls")) {
            JsonArray toolCalls = message.getAsJsonArray("tool_calls");
            for (int i = 0; i < toolCalls.size(); i++) {
                JsonObject call = toolCalls.get(i).getAsJsonObject();
                if ("function".equals(call.get("type").getAsString())) {
                    JsonObject functionCall = call.getAsJsonObject("function");
                    if ("next_action".equals(functionCall.get("name").getAsString())) {
                        String argumentsStr = functionCall.get("arguments").getAsString();
                        JsonObject input = JsonParser.parseString(argumentsStr).getAsJsonObject();
                        
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
            }
        }
        throw new IOException("Response contained no valid tool_calls for next_action.");
    }
}
