package com.sudeepkudari.ferry.agent;

import com.sudeepkudari.ferry.llm.LlmProvider;
import com.sudeepkudari.ferry.net.DeviceState;
import com.sudeepkudari.ferry.net.PortalApi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The core observe -> decide -> act loop. Deliberately dumb and provider-agnostic:
 * it doesn't know if the "brain" is Claude, another cloud provider, or a local
 * model, and it doesn't know Portal's wire format — both are hidden behind
 * their respective interfaces/clients. This class should be trivially unit
 * testable with a fake PortalClient and a fake LlmProvider.
 */
public class AgentLoop {

    /** Safety ceiling — prevents a confused agent from looping forever and burning API cost. */
    private static final int MAX_STEPS = 25;

    public interface StepListener {
        void onStep(int stepNumber, Action action);
        void onComplete(List<Action> history);
        void onFailed(String reason, List<Action> history);
    }

    private final PortalApi portalClient;
    private final LlmProvider llmProvider;
    private volatile boolean cancelled = false;

    public AgentLoop(PortalApi portalClient, LlmProvider llmProvider) {
        this.portalClient = portalClient;
        this.llmProvider = llmProvider;
    }

    /** Signals the loop to stop before its next step. Safe to call from another thread. */
    public void cancel() {
        cancelled = true;
    }

    /**
     * Runs the task to completion, failure, or cancellation. Intended to be called
     * from a background thread (e.g. from AgentTaskService) — this method blocks.
     */
    public void run(String task, StepListener listener) {
        List<Action> history = new ArrayList<>();

        for (int step = 1; step <= MAX_STEPS; step++) {
            if (cancelled) {
                listener.onFailed("Cancelled by user", history);
                return;
            }

            DeviceState state;
            try {
                state = portalClient.fetchState();
            } catch (IOException e) {
                listener.onFailed("Could not read device state from Portal: " + e.getMessage(), history);
                return;
            }

            Action action;
            try {
                action = llmProvider.decideNextAction(task, state, history);
            } catch (IOException e) {
                listener.onFailed("LLM provider (" + llmProvider.getProviderName() + ") call failed: " + e.getMessage(), history);
                return;
            }

            if (action.getType() == Action.Type.DONE) {
                listener.onComplete(history);
                return;
            }
            if (action.getType() == Action.Type.FAILED) {
                listener.onFailed("Agent gave up: " + action.getReasoning(), history);
                return;
            }

            try {
                portalClient.performAction(action);
            } catch (IOException e) {
                listener.onFailed("Failed to execute action " + action + ": " + e.getMessage(), history);
                return;
            }

            history.add(action);
            listener.onStep(step, action);
        }

        listener.onFailed("Reached step limit (" + MAX_STEPS + ") without completing task", history);
    }
}
