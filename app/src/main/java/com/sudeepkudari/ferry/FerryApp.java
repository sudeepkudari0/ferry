package com.sudeepkudari.ferry;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class FerryApp extends Application {

    public static final String AGENT_NOTIFICATION_CHANNEL = "agent_task_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    AGENT_NOTIFICATION_CHANNEL,
                    "Agent task running",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows progress while an automation task is running.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
