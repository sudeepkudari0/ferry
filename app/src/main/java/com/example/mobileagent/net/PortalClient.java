package com.example.mobileagent.net;

import android.content.Context;
import com.example.mobileagent.agent.Action;
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
                // We use a basic heuristic for SWIPE if start/end x/y are not fully specified.
                // Action format from LLM usually only gives directional hint or start x/y. 
                // For a robust implementation, we should parse directions.
                // Using arbitrary default swipe up for now.
                GestureController.INSTANCE.swipe(500, 1500, 500, 500, 300);
                break;
            case LAUNCH_APP:
                if (action.getPackageOrDeepLink() != null) {
                    // Use intent to launch app natively
                    android.content.Intent launchIntent = service.getPackageManager().getLaunchIntentForPackage(action.getPackageOrDeepLink());
                    if (launchIntent != null) {
                        launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                        service.startActivity(launchIntent);
                    }
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
