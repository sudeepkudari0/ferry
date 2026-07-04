package com.sudeepkudari.ferry.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.sudeepkudari.ferry.R;

public class FloatingOverlayManager {

    private final Context context;
    private final Context themedContext;
    private final WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams params;

    private TextView logText;
    private ScrollView logScrollView;
    private LinearLayout bodyLayout;
    private MaterialCardView overlayRoot;
    private TextView overlayTitle;
    private ImageView statusIndicator;
    private ObjectAnimator pulseAnimator;

    private boolean isExpanded = false;
    private final Runnable onStopClicked;

    public FloatingOverlayManager(Context context, Runnable onStopClicked) {
        this.context = context;
        this.themedContext = new ContextThemeWrapper(context, R.style.Theme_Ferry);
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.onStopClicked = onStopClicked;
    }

    public void show() {
        if (overlayView != null) return;

        overlayView = LayoutInflater.from(themedContext).inflate(R.layout.overlay_agent_status, null);

        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, // WRAP_CONTENT so it shrinks to a pill
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 20;
        params.y = 100;

        initViews();
        setupDragListener();

        windowManager.addView(overlayView, params);
        
        startPulseAnimation();
    }

    private void initViews() {
        overlayRoot = overlayView.findViewById(R.id.overlayRoot);
        bodyLayout = overlayView.findViewById(R.id.bodyLayout);
        logText = overlayView.findViewById(R.id.logText);
        logScrollView = overlayView.findViewById(R.id.logScrollView);
        View headerLayout = overlayView.findViewById(R.id.headerLayout);
        ImageButton btnStop = overlayView.findViewById(R.id.btnStop);
        overlayTitle = overlayView.findViewById(R.id.overlayTitle);
        statusIndicator = overlayView.findViewById(R.id.statusIndicator);

        btnStop.setOnClickListener(v -> {
            if (onStopClicked != null) onStopClicked.run();
        });

        // Toggle expand/collapse on tap
        headerLayout.setOnClickListener(v -> toggleExpanded());
    }

    private void toggleExpanded() {
        isExpanded = !isExpanded;
        bodyLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        
        // When expanded, take full width. When collapsed, wrap content to form a pill.
        if (isExpanded) {
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            overlayRoot.setCardBackgroundColor(android.graphics.Color.parseColor("#F8FFFFFF"));
        } else {
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            overlayRoot.setCardBackgroundColor(android.graphics.Color.parseColor("#D9FFFFFF")); // More translucent when collapsed
        }
        windowManager.updateViewLayout(overlayView, params);
    }

    private void startPulseAnimation() {
        pulseAnimator = ObjectAnimator.ofFloat(statusIndicator, "alpha", 1f, 0.2f, 1f);
        pulseAnimator.setDuration(1200);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.start();
    }

    private void setupDragListener() {
        View dragHandle = overlayView.findViewById(R.id.headerLayout);
        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        return false; // Return false so onClick still fires if it's just a tap
                    case MotionEvent.ACTION_MOVE:
                        float diffX = event.getRawX() - initialTouchX;
                        float diffY = event.getRawY() - initialTouchY;
                        if (Math.abs(diffX) > 10 || Math.abs(diffY) > 10) {
                            isDragging = true;
                            params.x = initialX + (int) diffX;
                            params.y = initialY + (int) diffY;
                            windowManager.updateViewLayout(overlayView, params);
                        }
                        return isDragging;
                    case MotionEvent.ACTION_UP:
                        return isDragging; // If we dragged, consume the event so onClick doesn't fire
                }
                return false;
            }
        });
    }

    public void appendLog(String message) {
        if (overlayView == null || logText == null) return;
        
        overlayView.post(() -> {
            // Update Title if it's a step
            if (message.startsWith("Step ")) {
                overlayTitle.setText(message.split("->")[0].trim());
            } else if (message.startsWith("Task complete")) {
                overlayTitle.setText("Completed");
                if (pulseAnimator != null) pulseAnimator.cancel();
                statusIndicator.setAlpha(1f);
                statusIndicator.setColorFilter(android.graphics.Color.parseColor("#059669")); // Green
            } else if (message.startsWith("Task stopped")) {
                overlayTitle.setText("Failed");
                if (pulseAnimator != null) pulseAnimator.cancel();
                statusIndicator.setAlpha(1f);
                statusIndicator.setColorFilter(android.graphics.Color.parseColor("#DC2626")); // Red
            }
            
            // Append log
            String current = logText.getText().toString();
            if (current.length() > 2000) {
                current = current.substring(current.length() - 2000);
            }
            logText.setText(current + "\n" + message);
            
            logScrollView.post(() -> logScrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    public void destroy() {
        if (pulseAnimator != null) pulseAnimator.cancel();
        if (overlayView != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
        }
    }
}
