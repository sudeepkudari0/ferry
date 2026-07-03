package com.example.mobileagent;

import com.example.mobileagent.agent.Action;
import com.example.mobileagent.net.DeviceState;
import com.example.mobileagent.net.PortalApi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Test double for PortalApi — records dispatched actions, returns a fixed state. */
public class FakePortalApi implements PortalApi {

    public final List<Action> dispatchedActions = new ArrayList<>();
    public boolean throwOnFetch = false;
    public boolean throwOnDispatch = false;

    @Override
    public DeviceState fetchState() throws IOException {
        if (throwOnFetch) throw new IOException("simulated fetch failure");
        DeviceState state = new DeviceState();
        state.accessibilityTreeJson = "{\"nodes\":[]}";
        state.currentPackage = "com.example.testtarget";
        state.timestampMs = System.currentTimeMillis();
        return state;
    }

    @Override
    public void performAction(Action action) throws IOException {
        if (throwOnDispatch) throw new IOException("simulated dispatch failure");
        dispatchedActions.add(action);
    }
}
