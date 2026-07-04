package com.sudeepkudari.ferry.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sudeepkudari.ferry.R;
import com.sudeepkudari.ferry.llm.local.LocalModel;
import com.sudeepkudari.ferry.llm.local.ModelDownloadManager;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Set;

/**
 * RecyclerView adapter for displaying local model cards with download/delete actions.
 */
public class LocalModelAdapter extends RecyclerView.Adapter<LocalModelAdapter.ViewHolder> {

    public interface ModelActionListener {
        void onDownloadClicked(LocalModel model);
        void onDeleteClicked(LocalModel model);
    }

    private final List<LocalModel> models;
    private final ModelDownloadManager downloadManager;
    private final ModelActionListener listener;

    public LocalModelAdapter(List<LocalModel> models, ModelDownloadManager downloadManager,
                             ModelActionListener listener) {
        this.models = models;
        this.downloadManager = downloadManager;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_local_model, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocalModel model = models.get(position);
        boolean isDownloaded = downloadManager.isModelDownloaded(model);

        holder.modelName.setText(model.getDisplayName());
        holder.modelMeta.setText(model.getParameterCount() + " • " 
                + model.getQuantization() + " • " + model.getFormattedSize()
                + " • " + model.getRecommendedRamGb() + "GB+ RAM");
        holder.modelDescription.setText(model.getDescription());

        if (isDownloaded) {
            holder.downloadButton.setVisibility(View.GONE);
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.downloadedBadge.setVisibility(View.VISIBLE);
            holder.progressLayout.setVisibility(View.GONE);

            long actualSize = downloadManager.getDownloadedSize(model);
            holder.downloadedBadge.setText("✓ Downloaded (" 
                    + ModelDownloadManager.formatBytes(actualSize) + ")");
        } else {
            holder.downloadButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setVisibility(View.GONE);
            holder.downloadedBadge.setVisibility(View.GONE);
            holder.progressLayout.setVisibility(View.GONE);
        }

        holder.downloadButton.setOnClickListener(v -> {
            if (listener != null) listener.onDownloadClicked(model);
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClicked(model);
        });
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView modelName;
        final TextView modelMeta;
        final TextView modelDescription;
        final MaterialButton downloadButton;
        final MaterialButton deleteButton;
        final View progressLayout;
        final ProgressBar downloadProgress;
        final TextView progressText;
        final TextView downloadedBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            modelName = itemView.findViewById(R.id.modelName);
            modelMeta = itemView.findViewById(R.id.modelMeta);
            modelDescription = itemView.findViewById(R.id.modelDescription);
            downloadButton = itemView.findViewById(R.id.downloadButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            progressLayout = itemView.findViewById(R.id.progressLayout);
            downloadProgress = itemView.findViewById(R.id.downloadProgress);
            progressText = itemView.findViewById(R.id.progressText);
            downloadedBadge = itemView.findViewById(R.id.downloadedBadge);
        }
    }
}
