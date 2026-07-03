package com.example.mobileagent.llm.local;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages downloading, tracking, and deleting local GGUF model files.
 * 
 * Uses Android's DownloadManager for robust background downloads with
 * built-in retry, progress notification, and Wi-Fi/cellular awareness.
 * 
 * Models are stored in the app's external files directory under "models/":
 *   /Android/data/com.example.mobileagent/files/models/
 * 
 * This location is app-private (no extra permissions needed), survives
 * app updates, and is large enough for multi-GB model files.
 */
public class ModelDownloadManager {

    private static final String MODELS_DIR = "models";

    private final Context context;
    private final DownloadManager downloadManager;
    private final Handler mainHandler;

    public interface DownloadProgressListener {
        void onProgressUpdate(String modelId, int percent, long bytesDownloaded, long totalBytes);
        void onDownloadComplete(String modelId, boolean success);
    }

    public ModelDownloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /** Get the directory where models are stored. Creates it if it doesn't exist. */
    public File getModelsDirectory() {
        File modelsDir = new File(context.getExternalFilesDir(null), MODELS_DIR);
        if (!modelsDir.exists()) {
            modelsDir.mkdirs();
        }
        return modelsDir;
    }

    /** Get the file path for a specific model. */
    public File getModelFile(LocalModel model) {
        return new File(getModelsDirectory(), model.getFileName());
    }

    /** Check if a model file has already been downloaded. */
    public boolean isModelDownloaded(LocalModel model) {
        File file = getModelFile(model);
        return file.exists() && file.length() > 0;
    }

    /** Get the file size of a downloaded model (0 if not downloaded). */
    public long getDownloadedSize(LocalModel model) {
        File file = getModelFile(model);
        return file.exists() ? file.length() : 0;
    }

    /** Returns a list of all downloaded model IDs. */
    public List<String> getDownloadedModelIds() {
        List<String> downloaded = new ArrayList<>();
        for (LocalModel model : LocalModelRegistry.getAllModels()) {
            if (isModelDownloaded(model)) {
                downloaded.add(model.getId());
            }
        }
        return downloaded;
    }

    /**
     * Start downloading a model file. Uses Android DownloadManager for
     * robust background downloading with progress notifications.
     *
     * @return the DownloadManager download ID, or -1 if download couldn't be started
     */
    public long startDownload(LocalModel model) {
        if (isModelDownloaded(model)) {
            return -1; // Already downloaded
        }

        // Ensure the models directory exists
        getModelsDirectory();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(model.getDownloadUrl()));
        request.setTitle("Downloading " + model.getDisplayName());
        request.setDescription(model.getParameterCount() + " • " + model.getFormattedSize());
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalFilesDir(context, null, MODELS_DIR + "/" + model.getFileName());
        request.setAllowedOverMetered(false); // Require Wi-Fi by default for large files
        request.setAllowedOverRoaming(false);

        try {
            return downloadManager.enqueue(request);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Start a download and allow it over mobile data.
     */
    public long startDownloadAllowMobileData(LocalModel model) {
        if (isModelDownloaded(model)) {
            return -1;
        }

        getModelsDirectory();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(model.getDownloadUrl()));
        request.setTitle("Downloading " + model.getDisplayName());
        request.setDescription(model.getParameterCount() + " • " + model.getFormattedSize());
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalFilesDir(context, null, MODELS_DIR + "/" + model.getFileName());
        request.setAllowedOverMetered(true);

        try {
            return downloadManager.enqueue(request);
        } catch (Exception e) {
            return -1;
        }
    }

    /** Delete a downloaded model file. */
    public boolean deleteModel(LocalModel model) {
        File file = getModelFile(model);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    /** Get the total disk space used by downloaded models. */
    public long getTotalDiskUsage() {
        long total = 0;
        File modelsDir = getModelsDirectory();
        File[] files = modelsDir.listFiles();
        if (files != null) {
            for (File f : files) {
                total += f.length();
            }
        }
        return total;
    }

    /** Format bytes to human-readable string. */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Query the download progress for a given download ID.
     * Returns an array: [bytesDownloaded, totalBytes, status]
     * Status values: DownloadManager.STATUS_*
     */
    public long[] queryProgress(long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor != null && cursor.moveToFirst()) {
                long bytesDownloaded = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                long totalBytes = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                int status = cursor.getInt(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                return new long[]{bytesDownloaded, totalBytes, status};
            }
        }
        return new long[]{0, 0, -1};
    }
}
