package com.example.mobileagent.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import android.view.ContextThemeWrapper;

import com.example.mobileagent.R;
import com.google.android.material.card.MaterialCardView;

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
    
    private boolean isMinimized = false;
    private final Runnable onStopClicked;

    public FloatingOverlayManager(Context context, Runnable onStopClicked) {
        // Service contexts don't carry the Activity's Material theme, so Material
        // components (MaterialCardView, ?selectableItemBackgroundBorderless, etc.)
        // crash on inflate. Wrapping with ContextThemeWrapper fixes this.
        this.context = context;
        this.themedContext = new ContextThemeWrapper(context, R.style.Theme_MobileAgent);
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
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        initViews();
        setupDragListener();

        windowManager.addView(overlayView, params);
    }

    private void initViews() {
        overlayRoot = overlayView.findViewById(R.id.overlayRoot);
        bodyLayout = overlayView.findViewById(R.id.bodyLayout);
        logText = overlayView.findViewById(R.id.logText);
        logScrollView = overlayView.findViewById(R.id.logScrollView);
        View headerLayout = overlayView.findViewById(R.id.headerLayout);
        ImageButton btnMinimize = overlayView.findViewById(R.id.btnMinimize);
        ImageButton btnStop = overlayView.findViewById(R.id.btnStop);
        SeekBar opacitySlider = overlayView.findViewById(R.id.opacitySlider);

        // Stop button
        btnStop.setOnClickListener(v -> {
            if (onStopClicked != null) {
                onStopClicked.run();
            }
        });

        // Minimize toggle
        btnMinimize.setOnClickListener(v -> {
            isMinimized = !isMinimized;
            bodyLayout.setVisibility(isMinimized ? View.GONE : View.VISIBLE);
            btnMinimize.setImageResource(isMinimized ? android.R.drawable.arrow_down_float : android.R.drawable.arrow_up_float);
        });

        // Double tap header to minimize/expand
        headerLayout.setOnClickListener(v -> btnMinimize.performClick());

        // Opacity slider
        opacitySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Map 0-100 to 0-255 alpha
                int alpha = (int) ((progress / 100f) * 255);
                String hexAlpha = Integer.toHexString(alpha);
                if (hexAlpha.length() == 1) hexAlpha = "0" + hexAlpha;
                overlayRoot.setCardBackgroundColor(Color.parseColor("#" + hexAlpha + "000000"));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupDragListener() {
        View dragHandle = overlayView.findViewById(R.id.headerLayout);
        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayView, params);
                        return true;
                }
                return false;
            }
        });
    }

    public void appendLog(String message) {
        if (overlayView == null || logText == null) return;
        
        // Ensure we run on UI thread if called from background
        overlayView.post(() -> {
            String current = logText.getText().toString();
            // keep last ~2000 chars to avoid memory issues
            if (current.length() > 2000) {
                current = current.substring(current.length() - 2000);
            }
            logText.setText(current + "\n" + message);
            
            // Auto-scroll to bottom
            logScrollView.post(() -> logScrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    public void destroy() {
        if (overlayView != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
        }
    }
}
