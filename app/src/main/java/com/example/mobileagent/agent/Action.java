package com.example.mobileagent.agent;

/**
 * A single, atomic action the agent has decided to take on the device.
 * This is the contract between {@link com.example.mobileagent.llm.LlmProvider}
 * (which decides actions) and {@link com.example.mobileagent.net.PortalClient}
 * (which executes them via Portal's local API).
 *
 * Keeping this a plain, provider-agnostic model means swapping LLM providers
 * (Claude / OpenAI / a local model) never touches the execution layer.
 */
public class Action {

    public enum Type {
        TAP,        // tap a specific node, identified by nodeId
        TAP_XY,     // fallback: raw coordinate tap when no clean node exists
        TYPE_TEXT,  // set text on a focused/targeted input node
        SWIPE,      // directional swipe/scroll
        LAUNCH_APP, // launch an app via package name / deep link instead of simulating navigation
        WAIT,       // deliberate pause (e.g. waiting for a screen to load)
        DONE,       // task considered complete
        FAILED      // agent gave up; include reason
    }

    private final Type type;
    private final String nodeId;
    private final String text;
    private final int x;
    private final int y;
    private final String packageOrDeepLink;
    private final String reasoning; // short LLM-provided justification, useful for logs/debugging

    private Action(Builder b) {
        this.type = b.type;
        this.nodeId = b.nodeId;
        this.text = b.text;
        this.x = b.x;
        this.y = b.y;
        this.packageOrDeepLink = b.packageOrDeepLink;
        this.reasoning = b.reasoning;
    }

    public Type getType() { return type; }
    public String getNodeId() { return nodeId; }
    public String getText() { return text; }
    public int getX() { return x; }
    public int getY() { return y; }
    public String getPackageOrDeepLink() { return packageOrDeepLink; }
    public String getReasoning() { return reasoning; }

    public static class Builder {
        private Type type;
        private String nodeId;
        private String text;
        private int x;
        private int y;
        private String packageOrDeepLink;
        private String reasoning;

        public Builder(Type type) { this.type = type; }
        public Builder nodeId(String v) { this.nodeId = v; return this; }
        public Builder text(String v) { this.text = v; return this; }
        public Builder coords(int x, int y) { this.x = x; this.y = y; return this; }
        public Builder target(String v) { this.packageOrDeepLink = v; return this; }
        public Builder reasoning(String v) { this.reasoning = v; return this; }
        public Action build() { return new Action(this); }
    }

    @Override
    public String toString() {
        return "Action{" + type +
                (nodeId != null ? ", node=" + nodeId : "") +
                (text != null ? ", text=" + text : "") +
                (packageOrDeepLink != null ? ", target=" + packageOrDeepLink : "") +
                "}";
    }
}
