package com.example.mobileagent.agent;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.mobileagent.MobileAgentApp;
import com.example.mobileagent.R;
import com.example.mobileagent.llm.ClaudeProvider;
import com.example.mobileagent.llm.LlmProvider;
import com.example.mobileagent.net.PortalClient;
import com.example.mobileagent.ui.MainActivity;
import com.example.mobileagent.util.SecureKeyStore;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Foreground service that owns the AgentLoop's lifetime. Kept as a service
 * (rather than running the loop on the Activity) so a task survives screen
 * rotation/backgrounding and so the user gets a persistent notification
 * showing an agent is actively acting on their behalf — important both for
 * UX honesty and because Android will kill background work without a
 * foreground service + notification.
 */
public class AgentTaskService extends Service {

    public static final String EXTRA_TASK = "extra_task";
    private static final int NOTIFICATION_ID = 1001;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private AgentLoop agentLoop;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String task = intent != null ? intent.getStringExtra(EXTRA_TASK) : null;
        startForeground(NOTIFICATION_ID, buildNotification("Starting task…"));

        if (task == null || task.isEmpty()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        SecureKeyStore keyStore = new SecureKeyStore(this);
        if (!keyStore.hasAnthropicKey()) {
            updateNotification("No API key set — add one in Settings.");
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!PortalClient.isPortalInstalled(this)) {
            updateNotification("Portal is not installed — see README for setup.");
            stopSelf();
            return START_NOT_STICKY;
        }

        // TODO: the local auth token should come from Portal's own pairing/setup flow,
        // not be hardcoded — wire this up once Portal's actual pairing mechanism is confirmed.
        PortalClient portalClient = new PortalClient(/* localAuthToken= */ "");
        LlmProvider llmProvider = new ClaudeProvider(keyStore.getAnthropicKey());
        agentLoop = new AgentLoop(portalClient, llmProvider);

        executor.execute(() -> agentLoop.run(task, new AgentLoop.StepListener() {
            @Override
            public void onStep(int stepNumber, Action action) {
                updateNotification("Step " + stepNumber + ": " + action.getReasoning());
            }

            @Override
            public void onComplete(List<Action> history) {
                updateNotification("Task complete (" + history.size() + " actions).");
                stopSelf();
            }

            @Override
            public void onFailed(String reason, List<Action> history) {
                updateNotification("Task stopped: " + reason);
                stopSelf();
            }
        }));

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (agentLoop != null) {
            agentLoop.cancel();
        }
        executor.shutdownNow();
        super.onDestroy();
    }

    private Notification buildNotification(String text) {
        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, openApp,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? PendingIntent.FLAG_IMMUTABLE
                        : 0
        );

        return new NotificationCompat.Builder(this, MobileAgentApp.AGENT_NOTIFICATION_CHANNEL)
                .setContentTitle("Mobile Agent")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_agent_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(text));
        }
    }
}
