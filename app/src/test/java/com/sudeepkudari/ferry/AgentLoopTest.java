package com.sudeepkudari.ferry;

import com.sudeepkudari.ferry.agent.Action;
import com.sudeepkudari.ferry.agent.AgentLoop;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AgentLoopTest {

    private FakePortalApi portal;

    @Before
    public void setUp() {
        portal = new FakePortalApi();
    }

    @Test
    public void completesSuccessfully_whenProviderEventuallyReturnsDone() {
        FakeLlmProvider llm = new FakeLlmProvider(Arrays.asList(
                new Action.Builder(Action.Type.LAUNCH_APP).target("com.google.android.gm").reasoning("open gmail").build(),
                new Action.Builder(Action.Type.TAP).nodeId("email_1").reasoning("open first unread email").build(),
                new Action.Builder(Action.Type.DONE).reasoning("task complete").build()
        ));
        AgentLoop loop = new AgentLoop(portal, llm);

        Result result = runSync(loop, "Summarize my last email");

        assertTrue(result.completed);
        assertFalse(result.failed);
        assertEquals(2, portal.dispatchedActions.size()); // DONE is not dispatched to Portal
        assertEquals(Action.Type.LAUNCH_APP, portal.dispatchedActions.get(0).getType());
        assertEquals(Action.Type.TAP, portal.dispatchedActions.get(1).getType());
    }

    @Test
    public void fails_whenProviderReturnsFailed() {
        FakeLlmProvider llm = new FakeLlmProvider(Arrays.asList(
                new Action.Builder(Action.Type.FAILED).reasoning("could not find target element").build()
        ));
        AgentLoop loop = new AgentLoop(portal, llm);

        Result result = runSync(loop, "Do something impossible");

        assertTrue(result.failed);
        assertTrue(result.failureReason.contains("could not find target element"));
        assertTrue(portal.dispatchedActions.isEmpty());
    }

    @Test
    public void fails_whenPortalStateFetchThrows() {
        portal.throwOnFetch = true;
        FakeLlmProvider llm = new FakeLlmProvider(Arrays.asList(
                new Action.Builder(Action.Type.DONE).reasoning("unreachable").build()
        ));
        AgentLoop loop = new AgentLoop(portal, llm);

        Result result = runSync(loop, "Any task");

        assertTrue(result.failed);
        assertTrue(result.failureReason.contains("device state"));
    }

    @Test
    public void fails_whenActionDispatchThrows() {
        portal.throwOnDispatch = true;
        FakeLlmProvider llm = new FakeLlmProvider(Arrays.asList(
                new Action.Builder(Action.Type.TAP).nodeId("x").reasoning("tap something").build()
        ));
        AgentLoop loop = new AgentLoop(portal, llm);

        Result result = runSync(loop, "Any task");

        assertTrue(result.failed);
        assertTrue(result.failureReason.contains("Failed to execute action"));
    }

    @Test
    public void stopsImmediately_whenCancelledBeforeRun() {
        FakeLlmProvider llm = new FakeLlmProvider(Arrays.asList(
                new Action.Builder(Action.Type.DONE).reasoning("should never run").build()
        ));
        AgentLoop loop = new AgentLoop(portal, llm);
        loop.cancel();

        Result result = runSync(loop, "Any task");

        assertTrue(result.failed);
        assertTrue(result.failureReason.contains("Cancelled"));
        assertTrue(portal.dispatchedActions.isEmpty());
    }

    // --- helper: AgentLoop.run is synchronous/blocking by design, so we just capture the callback result. ---

    private static class Result {
        boolean completed;
        boolean failed;
        String failureReason;
        List<Action> history = new ArrayList<>();
    }

    private Result runSync(AgentLoop loop, String task) {
        Result result = new Result();
        loop.run(task, new AgentLoop.StepListener() {
            @Override
            public void onStep(int stepNumber, Action action) { /* no-op for these tests */ }

            @Override
            public void onComplete(List<Action> history) {
                result.completed = true;
                result.history = history;
            }

            @Override
            public void onFailed(String reason, List<Action> history) {
                result.failed = true;
                result.failureReason = reason;
                result.history = history;
            }
        });
        return result;
    }
}
