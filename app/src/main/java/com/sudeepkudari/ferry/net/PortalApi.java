package com.sudeepkudari.ferry.net;

import com.sudeepkudari.ferry.agent.Action;

import java.io.IOException;

/**
 * Contract for anything that can report device state and execute actions.
 * PortalClient is the real implementation (talks to Portal over local HTTP).
 * Extracted as an interface purely so AgentLoop can be unit tested against a
 * fake, without spinning up a real HTTP server in tests.
 */
public interface PortalApi {
    DeviceState fetchState() throws IOException;
    void performAction(Action action) throws IOException;
}
