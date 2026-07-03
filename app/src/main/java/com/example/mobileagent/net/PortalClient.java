package com.example.mobileagent.net;

import android.content.Context;
import android.content.pm.PackageManager;

import com.example.mobileagent.agent.Action;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Client for the Portal app's local API (https://github.com/droidrun/mobilerun-portal),
 * which runs entirely on-device once installed and enabled by the user.
 *
 * IMPORTANT — integration boundary:
 * This class contains none of Portal's source. It is an independent HTTP/WebSocket
 * client that talks to Portal over loopback, the same way it would talk to any
 * third-party local service. Portal is a separate, user-installed dependency
 * (MIT licensed) — see THIRD_PARTY_NOTICES.md.
 *
 * TODO before first real run: the exact endpoint paths, request/response JSON
 * shapes, and the local auth-token handshake below are placeholders based on
 * Portal's publicly described capabilities (local HTTP on 8080 / WebSocket on
 * 8081, token-authenticated). Confirm the real paths and payloads against
 * Portal's own docs/OpenAPI spec before relying on this in production, and
 * update PORTAL_BASE_URL / endpoint constants accordingly.
 */
public class PortalClient implements PortalApi {

    private static final String PORTAL_PACKAGE_NAME = "com.mobilerun.portal";
    private static final String PORTAL_BASE_URL = "http://127.0.0.1:8080";

    // Placeholder paths — verify against real Portal API docs.
    private static final String ENDPOINT_STATE = "/state";
    private static final String ENDPOINT_ACTION = "/action";

    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();
    private final String authToken;

    public PortalClient(String localAuthToken) {
        this.authToken = localAuthToken;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    /** Returns true if the Portal app is installed on this device. */
    public static boolean isPortalInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(PORTAL_PACKAGE_NAME, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /** Fetches the current on-screen state (accessibility tree + metadata). */
    @Override
    public DeviceState fetchState() throws IOException {
        Request request = new Request.Builder()
                .url(PORTAL_BASE_URL + ENDPOINT_STATE)
                .header("Authorization", "Bearer " + authToken)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Portal state fetch failed: HTTP " + response.code()
                        + " — is Portal installed, running, and Accessibility enabled?");
            }
            String body = response.body().string();
            return gson.fromJson(body, DeviceState.class);
        }
    }

    /** Requests a screenshot explicitly — used only as a vision fallback, not on every step. */
    public DeviceState fetchStateWithScreenshot() throws IOException {
        // TODO: confirm whether this is a query param on /state or a separate endpoint in Portal's real API.
        Request request = new Request.Builder()
                .url(PORTAL_BASE_URL + ENDPOINT_STATE + "?include_screenshot=true")
                .header("Authorization", "Bearer " + authToken)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Portal screenshot fetch failed: HTTP " + response.code());
            }
            return gson.fromJson(response.body().string(), DeviceState.class);
        }
    }

    /** Dispatches a single decided action to Portal for execution. */
    @Override
    public void performAction(Action action) throws IOException {
        String json = actionToJson(action);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url(PORTAL_BASE_URL + ENDPOINT_ACTION)
                .header("Authorization", "Bearer " + authToken)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Portal action dispatch failed: HTTP " + response.code()
                        + " for action " + action);
            }
        }
    }

    /**
     * Maps our provider-agnostic Action to whatever wire format Portal's /action
     * endpoint actually expects. Kept as an isolated method so this is the single
     * place to update once the real API contract is confirmed.
     */
    private String actionToJson(Action action) {
        // Placeholder mapping — replace field names once verified against Portal's real API.
        switch (action.getType()) {
            case TAP:
                return String.format("{\"type\":\"tap\",\"nodeId\":\"%s\"}", action.getNodeId());
            case TAP_XY:
                return String.format("{\"type\":\"tap_xy\",\"x\":%d,\"y\":%d}", action.getX(), action.getY());
            case TYPE_TEXT:
                return String.format("{\"type\":\"type_text\",\"nodeId\":\"%s\",\"text\":%s}",
                        action.getNodeId(), gson.toJson(action.getText()));
            case SWIPE:
                return String.format("{\"type\":\"swipe\",\"x\":%d,\"y\":%d}", action.getX(), action.getY());
            case LAUNCH_APP:
                return String.format("{\"type\":\"launch\",\"target\":%s}", gson.toJson(action.getPackageOrDeepLink()));
            case WAIT:
                return "{\"type\":\"wait\"}";
            default:
                throw new IllegalArgumentException("Action type not dispatchable to Portal: " + action.getType());
        }
    }
}
