package com.sudeepkudari.ferry.ui;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sudeepkudari.ferry.R;
import com.sudeepkudari.ferry.data.StepEntity;

import java.util.ArrayList;
import java.util.List;

public class StepHistoryAdapter extends RecyclerView.Adapter<StepHistoryAdapter.StepViewHolder> {

    private List<StepEntity> steps = new ArrayList<>();

    public void setSteps(List<StepEntity> steps) {
        this.steps = steps;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_step_history, parent, false);
        return new StepViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StepViewHolder holder, int position) {
        StepEntity step = steps.get(position);
        holder.bind(step, position == steps.size() - 1);
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    static class StepViewHolder extends RecyclerView.ViewHolder {
        private final TextView actionTypeText;
        private final TextView reasoningText;
        private final TextView timeText;
        private final View timelineLine;

        public StepViewHolder(@NonNull View itemView) {
            super(itemView);
            actionTypeText = itemView.findViewById(R.id.stepActionType);
            reasoningText = itemView.findViewById(R.id.stepReasoning);
            timeText = itemView.findViewById(R.id.stepTime);
            timelineLine = itemView.findViewById(R.id.timelineLine);
        }

        public void bind(StepEntity step, boolean isLast) {
            actionTypeText.setText("Step " + step.stepNumber + ": " + step.actionType);
            reasoningText.setText(step.reasoning);
            timeText.setText(DateFormat.format("hh:mm:ss a", step.timestamp));
            
            // Hide the timeline line for the last item so it doesn't run off the screen
            timelineLine.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);
        }
    }
}
