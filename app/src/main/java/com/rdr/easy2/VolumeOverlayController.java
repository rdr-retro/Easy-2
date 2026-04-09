package com.rdr.easy2;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class VolumeOverlayController {
    private static final int TARGET_STREAM = AudioManager.STREAM_MUSIC;
    private static final long HIDE_DELAY_MS = 1500L;

    private final Activity activity;
    private final AudioManager audioManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final View overlayContainer;
    private final View overlayPill;
    private final View progressTrack;
    private final View progressFill;
    private final TextView volumeValueView;
    private final TextView volumeLabelView;
    private final ImageView volumeIconView;

    private final Runnable hideRunnable = this::hideNow;

    public VolumeOverlayController(Activity activity) {
        this.activity = activity;
        this.audioManager = activity.getSystemService(AudioManager.class);
        overlayContainer = activity.findViewById(R.id.volume_overlay_container);
        overlayPill = activity.findViewById(R.id.volume_overlay_pill);
        progressTrack = activity.findViewById(R.id.volume_overlay_track);
        progressFill = activity.findViewById(R.id.volume_overlay_fill);
        volumeValueView = activity.findViewById(R.id.volume_overlay_value);
        volumeLabelView = activity.findViewById(R.id.volume_overlay_label);
        volumeIconView = activity.findViewById(R.id.volume_overlay_icon);

        hideNow();
    }

    public boolean handleKeyEvent(KeyEvent event) {
        if (event == null) {
            return false;
        }

        int keyCode = event.getKeyCode();
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP
                && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int direction = keyCode == KeyEvent.KEYCODE_VOLUME_UP
                    ? AudioManager.ADJUST_RAISE
                    : AudioManager.ADJUST_LOWER;
            adjustVolume(direction);
            return true;
        }

        return event.getAction() == KeyEvent.ACTION_UP;
    }

    public void applyTheme(LauncherThemePalette palette) {
        if (palette == null) {
            return;
        }

        if (progressFill != null) {
            progressFill.setBackground(createRoundedBackground(palette.getPrimaryColor(), 18));
        }
        if (volumeIconView != null) {
            volumeIconView.setImageTintList(ColorStateList.valueOf(palette.getPrimaryColor()));
        }
        if (volumeValueView != null) {
            volumeValueView.setTextColor(palette.getPrimaryColor());
        }
        if (volumeLabelView != null) {
            volumeLabelView.setTextColor(palette.getPrimaryColor());
        }
        if (overlayPill != null) {
            overlayPill.setBackground(createRoundedBackground(0xF7FFFFFF, 56));
        }
    }

    public void release() {
        handler.removeCallbacks(hideRunnable);
    }

    private void adjustVolume(int direction) {
        if (audioManager == null) {
            return;
        }

        audioManager.adjustStreamVolume(
                TARGET_STREAM,
                direction,
                AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
        );
        showCurrentVolume();
    }

    private void showCurrentVolume() {
        if (audioManager == null) {
            return;
        }

        int maxVolume = Math.max(1, audioManager.getStreamMaxVolume(TARGET_STREAM));
        int currentVolume = audioManager.getStreamVolume(TARGET_STREAM);
        int percentage = Math.round((currentVolume * 100f) / maxVolume);

        if (volumeValueView != null) {
            volumeValueView.setText(activity.getString(R.string.volume_overlay_percent, percentage));
        }

        updateProgress(percentage);

        if (overlayContainer != null) {
            overlayContainer.setAlpha(1f);
            overlayContainer.setVisibility(View.VISIBLE);
        }

        handler.removeCallbacks(hideRunnable);
        handler.postDelayed(hideRunnable, HIDE_DELAY_MS);
    }

    private void updateProgress(int percentage) {
        if (progressTrack == null || progressFill == null) {
            return;
        }

        progressTrack.post(() -> {
            int trackWidth = progressTrack.getWidth();
            if (trackWidth <= 0) {
                return;
            }

            int width = Math.round(trackWidth * (percentage / 100f));
            if (percentage > 0) {
                width = Math.max(width, dpToPx(18));
            }

            FrameLayout.LayoutParams layoutParams =
                    (FrameLayout.LayoutParams) progressFill.getLayoutParams();
            layoutParams.width = width;
            progressFill.setLayoutParams(layoutParams);
        });
    }

    private void hideNow() {
        if (overlayContainer != null) {
            overlayContainer.setVisibility(View.GONE);
        }
    }

    private GradientDrawable createRoundedBackground(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dpToPx(radiusDp));
        drawable.setColor(color);
        return drawable;
    }

    private int dpToPx(int dpValue) {
        return Math.round(
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        dpValue,
                        activity.getResources().getDisplayMetrics()
                )
        );
    }
}
