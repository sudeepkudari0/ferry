package com.example.mobileagent.net;

/**
 * Structured snapshot of on-screen state as reported by Portal.
 *
 * NOTE: field names here are a reasonable placeholder shape based on Portal's
 * publicly described capabilities (accessibility tree, current package/activity,
 * optional screenshot). Before wiring this against the real API, confirm the
 * exact JSON shape against Portal's own API docs/OpenAPI spec (check the
 * mobilerun-portal repo and docs.mobilerun.ai) and adjust the Gson field
 * mappings in PortalClient accordingly — do not assume this is byte-for-byte
 * accurate until verified against a running Portal instance.
 */
public class DeviceState {

    /** Raw accessibility tree, serialized as Portal returns it (JSON string or nested object — TBD on verification). */
    public String accessibilityTreeJson;

    /** Package name of the currently focused app, if Portal exposes it. */
    public String currentPackage;

    /** Base64-encoded screenshot, populated only when explicitly requested (vision fallback path). */
    public String screenshotBase64;

    public long timestampMs;

    @Override
    public String toString() {
        return "DeviceState{pkg=" + currentPackage +
                ", hasTree=" + (accessibilityTreeJson != null) +
                ", hasScreenshot=" + (screenshotBase64 != null) + "}";
    }
}
