package com.sudeepkudari.ferry.model;

import java.util.List;
import java.util.Map;

public class UseCase {
    private String id;
    private String title;
    private String subtitle;
    private int iconResId;
    private List<UseCaseParameter> parameters;
    private String promptTemplate;

    public UseCase(String id, String title, String subtitle, int iconResId, List<UseCaseParameter> parameters, String promptTemplate) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.iconResId = iconResId;
        this.parameters = parameters;
        this.promptTemplate = promptTemplate;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public int getIconResId() { return iconResId; }
    public List<UseCaseParameter> getParameters() { return parameters; }
    public String getPromptTemplate() { return promptTemplate; }

    public String buildPrompt(Map<String, String> inputs) {
        String prompt = promptTemplate;
        for (Map.Entry<String, String> entry : inputs.entrySet()) {
            prompt = prompt.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return prompt;
    }
}
