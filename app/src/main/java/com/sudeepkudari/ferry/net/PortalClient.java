package com.sudeepkudari.ferry.net;

import android.content.Context;
import com.sudeepkudari.ferry.agent.Action;
import com.mobilerun.portal.core.StateRepository;
import com.mobilerun.portal.service.MobilerunAccessibilityService;
import com.mobilerun.portal.service.GestureController;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import android.util.Log;

import java.io.IOException;

public class PortalClient implements PortalApi {
    private static final String TAG = "PortalClient";
    
    // Cache the last fetched tree so we can map nodeIds back to screen bounds
    private JSONObject lastFetchedTree;

    public PortalClient(String localAuthToken) {
        // No longer used since we are native
    }

    public static boolean isPortalInstalled(Context context) {
        return MobilerunAccessibilityService.Companion.getInstance() != null;
    }

    @Override
    public DeviceState fetchState() throws IOException {
        MobilerunAccessibilityService service = MobilerunAccessibilityService.Companion.getInstance();
        if (service == null) {
            throw new IOException("MobilerunAccessibilityService is not running. Please enable it in Accessibility Settings.");
        }

        StateRepository repo = new StateRepository(service);
        DeviceState state = new DeviceState();
        
        JSONObject tree = repo.getFullTree(false);
        if (tree != null) {
            lastFetchedTree = tree;
            state.accessibilityTreeJson = tree.toString();
        }

        state.currentPackage = repo.getPhoneState().getPackageName();
        state.timestampMs = System.currentTimeMillis();

        return state;
    }

    @Override
    public void performAction(Action action) throws IOException {
        MobilerunAccessibilityService service = MobilerunAccessibilityService.Companion.getInstance();
        if (service == null) {
            throw new IOException("MobilerunAccessibilityService is not running.");
        }
        StateRepository repo = new StateRepository(service);

        switch (action.getType()) {
            case TAP:
                if (action.getNodeId() != null && lastFetchedTree != null) {
                    int[] center = findNodeCenter(lastFetchedTree, action.getNodeId());
                    if (center != null) {
                        Log.d(TAG, "Tapping node " + action.getNodeId() + " at " + center[0] + ", " + center[1]);
                        GestureController.INSTANCE.tap(center[0], center[1]);
                    } else {
                        Log.w(TAG, "Could not find bounds for node_id: " + action.getNodeId());
                    }
                }
                break;
            case TAP_XY:
                GestureController.INSTANCE.tap(action.getX(), action.getY());
                break;
            case TYPE_TEXT:
                // If a nodeId was provided, we could tap it first to focus, but inputText operates globally on the active node.
                if (action.getNodeId() != null && lastFetchedTree != null) {
                    int[] center = findNodeCenter(lastFetchedTree, action.getNodeId());
                    if (center != null) {
                        GestureController.INSTANCE.tap(center[0], center[1]);
                        // Sleep briefly to allow focus
                        try { Thread.sleep(300); } catch (Exception e) {}
                    }
                }
                repo.inputText(action.getText(), false);
                break;
            case SWIPE:
                GestureController.INSTANCE.swipe(500, 1500, 500, 500, 300);
                break;
            case SCROLL:
                String dir = action.getText() != null ? action.getText().toLowerCase() : "down";
                // To scroll DOWN, we swipe UP from bottom to top
                if (dir.contains("up")) {
                    GestureController.INSTANCE.swipe(500, 500, 500, 1500, 300);
                } else if (dir.contains("left")) {
                    GestureController.INSTANCE.swipe(200, 1000, 800, 1000, 300);
                } else if (dir.contains("right")) {
                    GestureController.INSTANCE.swipe(800, 1000, 200, 1000, 300);
                } else {
                    // Default to scroll down
                    GestureController.INSTANCE.swipe(500, 1500, 500, 500, 300);
                }
                break;
            case BACK:
                service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
                break;
            case HOME:
                service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);
                break;
            case LAUNCH_APP:
                if (action.getPackageOrDeepLink() != null) {
                    String requestedPkg = action.getPackageOrDeepLink().trim();
                    String requestedLower = requestedPkg.toLowerCase();
                    
                    // If they typed something like "com.andoird.chrome", extract "chrome" for fuzzy fallback
                    String fallbackSearch = requestedLower;
                    if (requestedLower.contains(".")) {
                        String[] parts = requestedLower.split("\\.");
                        fallbackSearch = parts[parts.length - 1]; // e.g. "chrome"
                    }

                    java.util.List<String> candidates = new java.util.ArrayList<>();
                    candidates.add(requestedPkg);

                    // Fuzzy match against all installed packages
                    android.content.pm.PackageManager pm = service.getPackageManager();
                    java.util.List<android.content.pm.ApplicationInfo> apps = pm.getInstalledApplications(0);
                    
                    for (android.content.pm.ApplicationInfo app : apps) {
                        String pkgName = app.packageName;
                        // Skip if it doesn't have a launch intent (system background apps)
                        if (pm.getLaunchIntentForPackage(pkgName) == null) continue;
                        
                        CharSequence labelSeq = pm.getApplicationLabel(app);
                        String label = labelSeq != null ? labelSeq.toString().toLowerCase() : "";
                        
                        // Exact or partial match on label (e.g. "YouTube") or package name
                        if (label.contains(requestedLower) || pkgName.toLowerCase().contains(requestedLower)
                                || label.contains(fallbackSearch) || pkgName.toLowerCase().contains(fallbackSearch)) {
                            if (!candidates.contains(pkgName)) {
                                candidates.add(pkgName);
                            }
                        }
                    }

                    boolean launched = false;
                    for (String pkg : candidates) {
                        android.content.Intent launchIntent = pm.getLaunchIntentForPackage(pkg);
                        if (launchIntent != null) {
                            launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                            service.startActivity(launchIntent);
                            Log.d(TAG, "Launched package: " + pkg
                                    + (pkg.equals(requestedPkg) ? "" : " (fuzzy match from '" + requestedPkg + "')"));
                            launched = true;
                            break;
                        } else {
                            Log.w(TAG, "getLaunchIntentForPackage returned null for: " + pkg);
                        }
                    }

                    if (!launched) {
                        throw new IOException("LAUNCH_APP failed: no launchable package found for '"
                                + requestedPkg + "'. Tried fuzzy matching but found no installed apps.");
                    }
                } else {
                    throw new IOException("LAUNCH_APP action missing 'target' (package name or deep link).");
                }
                break;
            default:
                break;
        }
    }

    private int[] findNodeCenter(JSONObject node, String targetId) {
        try {
            if (node.has("node_id") && node.getString("node_id").equals(targetId)) {
                if (node.has("boundsInScreen")) {
                    JSONObject bounds = node.getJSONObject("boundsInScreen");
                    int left = bounds.getInt("left");
                    int top = bounds.getInt("top");
                    int right = bounds.getInt("right");
                    int bottom = bounds.getInt("bottom");
                    int centerX = (left + right) / 2;
                    int centerY = (top + bottom) / 2;
                    return new int[]{centerX, centerY};
                }
            }
            if (node.has("children")) {
                JSONArray children = node.getJSONArray("children");
                for (int i = 0; i < children.length(); i++) {
                    int[] found = findNodeCenter(children.getJSONObject(i), targetId);
                    if (found != null) return found;
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON tree while searching for node_id", e);
        }
        return null;
    }
}
