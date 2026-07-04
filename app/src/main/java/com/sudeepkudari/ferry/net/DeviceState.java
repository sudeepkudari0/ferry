package com.sudeepkudari.ferry.net;

/**
 * Structured snapshot of on-screen state as reported by Portal.
 *
 * This object is manually populated by PortalClient by parsing Portal's
 * real API responses (e.g. extracting the tree and package from /state,
 * and fetching /screenshot if requested).
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
