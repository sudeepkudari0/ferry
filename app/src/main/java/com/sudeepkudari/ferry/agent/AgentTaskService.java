package com.sudeepkudari.ferry.agent;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.sudeepkudari.ferry.FerryApp;
import com.sudeepkudari.ferry.R;
import com.sudeepkudari.ferry.llm.LlmProvider;
import com.sudeepkudari.ferry.llm.LlmProviderFactory;
import com.sudeepkudari.ferry.net.PortalClient;
import com.sudeepkudari.ferry.ui.MainActivity;
import com.sudeepkudari.ferry.ui.FloatingOverlayManager;
import com.sudeepkudari.ferry.util.SecureKeyStore;
import com.sudeepkudari.ferry.data.AppDatabase;
import com.sudeepkudari.ferry.data.StepEntity;
import com.sudeepkudari.ferry.data.TaskDao;
import com.sudeepkudari.ferry.data.TaskEntity;
import java.util.UUID;

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
    private FloatingOverlayManager overlayManager;
    private String currentTaskId;

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

        // Initialize and show floating overlay if we have permission
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || android.provider.Settings.canDrawOverlays(this)) {
            // Need to create overlay on main thread
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                overlayManager = new FloatingOverlayManager(this, () -> {
                    if (agentLoop != null) agentLoop.cancel();
                    stopSelf();
                });
                overlayManager.show();
                overlayManager.appendLog("Task: " + task);
            });
        }

        SecureKeyStore keyStore = new SecureKeyStore(this);
        String selectedProvider = keyStore.getSelectedProvider();
        if (!keyStore.hasKey(selectedProvider)) {
            String msg = "LOCAL".equals(selectedProvider)
                    ? "Local LLM server not configured — set it up in Settings."
                    : "No API key set for " + selectedProvider + " — add one in Settings.";
            updateNotification(msg);
            broadcastLog(msg);
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
        LlmProvider llmProvider = LlmProviderFactory.getActiveProvider(this);
        int maxSteps = keyStore.getMaxSteps();
        agentLoop = new AgentLoop(portalClient, llmProvider, maxSteps);

        AppDatabase db = AppDatabase.getDatabase(this);
        TaskDao dao = db.taskDao();
        currentTaskId = UUID.randomUUID().toString();

        executor.execute(() -> {
            dao.insertTask(new TaskEntity(currentTaskId, task, System.currentTimeMillis(), "RUNNING"));
            
            agentLoop.run(task, new AgentLoop.StepListener() {
                @Override
                public void onStep(int stepNumber, Action action) {
                    String log = "Step " + stepNumber + ": " + action.getType().name() + " -> " + action.getReasoning();
                    updateNotification(log);
                    broadcastLog(log);
                    
                    String summary = action.getType().name();
                    dao.insertStep(new StepEntity(currentTaskId, stepNumber, action.getType().name(), action.getReasoning(), summary, System.currentTimeMillis()));
                }

                @Override
                public void onComplete(List<Action> history) {
                    String log = "Task complete (" + history.size() + " actions).";
                    updateNotification(log);
                    broadcastLog(log);
                    
                    dao.updateTaskStatus(currentTaskId, "COMPLETED", System.currentTimeMillis());
                    stopSelf();
                }

                @Override
                public void onFailed(String reason, List<Action> history) {
                    String log = "Task stopped: " + reason;
                    updateNotification(log);
                    broadcastLog(log);
                    
                    dao.updateTaskStatus(currentTaskId, "FAILED", System.currentTimeMillis());
                    stopSelf();
                }
            });
        });

        return START_NOT_STICKY;
    }

    private void broadcastLog(String message) {
        Intent intent = new Intent("com.sudeepkudari.ferry.LOG");
        intent.putExtra("log_line", message);
        sendBroadcast(intent);
        
        if (overlayManager != null) {
            overlayManager.appendLog(message);
        }
    }

    @Override
    public void onDestroy() {
        if (agentLoop != null) {
            agentLoop.cancel();
        }
        executor.shutdownNow();
        
        if (currentTaskId != null) {
            // Update the DB immediately on the main thread (or a new thread) if it was destroyed abruptly
            new Thread(() -> {
                AppDatabase.getDatabase(this).taskDao().updateTaskStatus(currentTaskId, "FAILED", System.currentTimeMillis());
            }).start();
        }
        
        if (overlayManager != null) {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> overlayManager.destroy());
        }
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

        return new NotificationCompat.Builder(this, FerryApp.AGENT_NOTIFICATION_CHANNEL)
                .setContentTitle("Ferry")
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
