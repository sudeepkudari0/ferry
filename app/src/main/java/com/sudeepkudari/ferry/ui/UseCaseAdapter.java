package com.sudeepkudari.ferry.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sudeepkudari.ferry.R;
import com.sudeepkudari.ferry.model.UseCase;

import java.util.ArrayList;
import java.util.List;

public class UseCaseAdapter extends RecyclerView.Adapter<UseCaseAdapter.UseCaseViewHolder> {

    private List<UseCase> useCases = new ArrayList<>();
    private final OnUseCaseClickListener listener;

    public interface OnUseCaseClickListener {
        void onUseCaseClick(UseCase useCase);
    }

    public UseCaseAdapter(OnUseCaseClickListener listener) {
        this.listener = listener;
    }

    public void setUseCases(List<UseCase> useCases) {
        this.useCases = useCases;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UseCaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_use_case, parent, false);
        return new UseCaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UseCaseViewHolder holder, int position) {
        UseCase useCase = useCases.get(position);
        holder.titleTextView.setText(useCase.getTitle());
        holder.subtitleTextView.setText(useCase.getSubtitle());
        holder.iconImageView.setImageResource(useCase.getIconResId());
        holder.itemView.setOnClickListener(v -> listener.onUseCaseClick(useCase));
    }

    @Override
    public int getItemCount() {
        return useCases.size();
    }

    static class UseCaseViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImageView;
        TextView titleTextView;
        TextView subtitleTextView;

        public UseCaseViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.iconImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            subtitleTextView = itemView.findViewById(R.id.subtitleTextView);
        }
    }
}
