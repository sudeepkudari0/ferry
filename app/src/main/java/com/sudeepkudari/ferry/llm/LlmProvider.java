package com.sudeepkudari.ferry.llm;

import com.sudeepkudari.ferry.agent.Action;
import com.sudeepkudari.ferry.net.DeviceState;

import java.io.IOException;
import java.util.List;

/**
 * The "brain" seam: anything that can look at the current task, the device
 * state, and the action history, and decide the single next action.
 *
 * This interface is deliberately the only thing AgentLoop depends on — it
 * doesn't know or care whether the implementation is a cloud API call under
 * the user's own key (BYOK) or an on-device model. That's the whole point:
 * swapping providers is a one-line change in whoever constructs AgentLoop.
 */
public interface LlmProvider {

    /**
     * Decide the next action given the task and current context.
     *
     * @param task           the user's original natural-language goal
     * @param state          current on-screen state from Portal
     * @param history        prior actions taken this task, oldest first (for context/self-correction)
     * @throws IOException on network/provider failure — callers should treat this as
     *                      retryable a bounded number of times before surfacing to the user
     */
    Action decideNextAction(String task, DeviceState state, List<Action> history) throws IOException;

    /** Short identifier for logging/UI, e.g. "claude-sonnet-5", "local-gemma-3b". */
    String getProviderName();
}
