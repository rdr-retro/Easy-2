package com.rdr.easy2;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class HearingAssistActivity extends AppCompatActivity {
    private static final String SOUND_AMPLIFIER_PACKAGE =
            "com.google.android.accessibility.soundamplifier";

    private VolumeOverlayController volumeOverlayController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hearing_assist);
        volumeOverlayController = new VolumeOverlayController(this);

        findViewById(R.id.close_hearing_button).setOnClickListener(view -> finish());
        findViewById(R.id.hearing_amplifier_button).setOnClickListener(
                view -> openSoundAmplifier()
        );
        findViewById(R.id.hearing_sound_button).setOnClickListener(
                view -> openExternal(new Intent(Settings.ACTION_SOUND_SETTINGS))
        );
        findViewById(R.id.hearing_devices_button).setOnClickListener(
                view -> openHearingDevices()
        );
        findViewById(R.id.hearing_accessibility_button).setOnClickListener(
                view -> openExternal(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        );

        applyThemePalette();
        enableFullscreen();
    }

    @Override
    protected void onDestroy() {
        if (volumeOverlayController != null) {
            volumeOverlayController.release();
        }
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableFullscreen();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (volumeOverlayController != null && volumeOverlayController.handleKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void applyThemePalette() {
        LauncherThemePalette palette = LauncherThemePalette.fromPreferences(this);

        View rootView = findViewById(R.id.hearing_root);
        TextView titleView = findViewById(R.id.hearing_title_view);
        TextView subtitleView = findViewById(R.id.hearing_subtitle_view);
        TextView amplifierButton = findViewById(R.id.hearing_amplifier_button);
        TextView soundButton = findViewById(R.id.hearing_sound_button);
        TextView devicesButton = findViewById(R.id.hearing_devices_button);
        TextView accessibilityButton = findViewById(R.id.hearing_accessibility_button);

        if (rootView != null) {
            rootView.setBackgroundColor(palette.getBackgroundColor());
        }
        if (titleView != null) {
            titleView.setTextColor(palette.getHeadingColor());
        }
        if (subtitleView != null) {
            subtitleView.setTextColor(palette.getBodyTextColor());
        }

        styleActionButton(amplifierButton, palette.getPrimaryColor());
        styleActionButton(soundButton, palette.getChipColor());
        styleActionButton(devicesButton, palette.getCircleColor());
        styleActionButton(accessibilityButton, blend(palette.getPrimaryColor(), palette.getCircleColor(), 0.42f));

        if (volumeOverlayController != null) {
            volumeOverlayController.applyTheme(palette);
        }
    }

    private void styleActionButton(TextView button, int fillColor) {
        if (button == null) {
            return;
        }

        button.setTextColor(Color.WHITE);
        button.setBackground(createRoundedBackground(fillColor, 26));
    }

    private void openSoundAmplifier() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(SOUND_AMPLIFIER_PACKAGE);
        if (launchIntent != null && launchExternal(launchIntent)) {
            return;
        }

        openExternal(new Intent(Settings.ACTION_SOUND_SETTINGS));
    }

    private void openHearingDevices() {
        if (launchExternal(new Intent("android.settings.HEARING_DEVICES_SETTINGS"))) {
            return;
        }

        if (launchExternal(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS))) {
            return;
        }

        showError();
    }

    private void openExternal(Intent intent) {
        if (launchExternal(intent)) {
            return;
        }

        showError();
    }

    private boolean launchExternal(Intent intent) {
        try {
            if (intent.resolveActivity(getPackageManager()) == null) {
                return false;
            }
            startActivity(intent);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void showError() {
        Toast.makeText(this, R.string.hearing_open_error, Toast.LENGTH_SHORT).show();
    }

    private void enableFullscreen() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());

        if (controller == null) {
            return;
        }

        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
        controller.hide(WindowInsetsCompat.Type.systemBars());
    }

    private GradientDrawable createRoundedBackground(int fillColor, int cornerRadiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dpToPx(cornerRadiusDp));
        drawable.setColor(fillColor);
        return drawable;
    }

    private int blend(int baseColor, int targetColor, float ratio) {
        float clampedRatio = Math.max(0f, Math.min(1f, ratio));
        float inverseRatio = 1f - clampedRatio;
        return Color.argb(
                Math.round((Color.alpha(baseColor) * inverseRatio) + (Color.alpha(targetColor) * clampedRatio)),
                Math.round((Color.red(baseColor) * inverseRatio) + (Color.red(targetColor) * clampedRatio)),
                Math.round((Color.green(baseColor) * inverseRatio) + (Color.green(targetColor) * clampedRatio)),
                Math.round((Color.blue(baseColor) * inverseRatio) + (Color.blue(targetColor) * clampedRatio))
        );
    }

    private float dpToPx(int dpValue) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                getResources().getDisplayMetrics()
        );
    }
}
