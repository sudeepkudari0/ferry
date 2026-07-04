package com.sudeepkudari.ferry.model;

import java.util.List;

public class UseCaseParameter {
    public enum Type {
        TEXT,
        MULTILINE_TEXT,
        RADIO,
        DROPDOWN
    }

    private String id;
    private String name;
    private Type type;
    private List<String> options;

    public UseCaseParameter(String id, String name, Type type, List<String> options) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.options = options;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Type getType() { return type; }
    public List<String> getOptions() { return options; }
}
