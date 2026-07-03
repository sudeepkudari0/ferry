package com.example.mobileagent;

import com.example.mobileagent.agent.Action;
import com.example.mobileagent.llm.LlmProvider;
import com.example.mobileagent.net.DeviceState;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/** Test double for LlmProvider — returns a pre-scripted sequence of actions, one per call. */
public class FakeLlmProvider implements LlmProvider {

    private final Queue<Action> scriptedActions;
    public boolean throwOnNextCall = false;

    public FakeLlmProvider(List<Action> scriptedActions) {
        this.scriptedActions = new LinkedList<>(scriptedActions);
    }

    @Override
    public Action decideNextAction(String task, DeviceState state, List<Action> history) throws IOException {
        if (throwOnNextCall) throw new IOException("simulated provider failure");
        if (scriptedActions.isEmpty()) {
            throw new IllegalStateException("FakeLlmProvider ran out of scripted actions — test script too short");
        }
        return scriptedActions.poll();
    }

    @Override
    public String getProviderName() {
        return "fake-provider";
    }
}
