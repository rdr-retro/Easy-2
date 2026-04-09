package com.rdr.easy2;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.AlarmClock;
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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AlarmActivity extends AppCompatActivity {
    private VolumeOverlayController volumeOverlayController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        volumeOverlayController = new VolumeOverlayController(this);

        findViewById(R.id.close_alarm_button).setOnClickListener(view -> finish());
        findViewById(R.id.alarm_open_system_button).setOnClickListener(view -> openSystemAlarms());
        findViewById(R.id.alarm_create_button).setOnClickListener(view -> createAlarm());
        findViewById(R.id.alarm_timer_10_button).setOnClickListener(
                view -> startTimer(10, getString(R.string.alarm_timer_label_10))
        );
        findViewById(R.id.alarm_timer_30_button).setOnClickListener(
                view -> startTimer(30, getString(R.string.alarm_timer_label_30))
        );

        updateCurrentTime();
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
    protected void onResume() {
        super.onResume();
        updateCurrentTime();
        enableFullscreen();
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

        View rootView = findViewById(R.id.alarm_root);
        TextView titleView = findViewById(R.id.alarm_title_view);
        TextView subtitleView = findViewById(R.id.alarm_subtitle_view);
        TextView nowView = findViewById(R.id.alarm_now_view);
        TextView openSystemButton = findViewById(R.id.alarm_open_system_button);
        TextView createButton = findViewById(R.id.alarm_create_button);
        TextView timer10Button = findViewById(R.id.alarm_timer_10_button);
        TextView timer30Button = findViewById(R.id.alarm_timer_30_button);

        if (rootView != null) {
            rootView.setBackgroundColor(palette.getBackgroundColor());
        }
        if (titleView != null) {
            titleView.setTextColor(palette.getHeadingColor());
        }
        if (subtitleView != null) {
            subtitleView.setTextColor(palette.getBodyTextColor());
        }
        if (nowView != null) {
            nowView.setTextColor(palette.getHeadingColor());
            nowView.setBackground(createRoundedBackground(
                    palette.getSetupContactFillColor(),
                    24
            ));
        }
        styleActionButton(
                openSystemButton,
                "alarm_open_system",
                palette.getPrimaryColor(),
                palette
        );
        styleActionButton(
                createButton,
                "alarm_create",
                palette.getChipColor(),
                palette
        );
        styleActionButton(
                timer10Button,
                "alarm_timer_10",
                palette.getCircleColor(),
                palette
        );
        styleActionButton(
                timer30Button,
                "alarm_timer_30",
                blend(palette.getPrimaryColor(), palette.getCircleColor(), 0.4f),
                palette
        );

        if (volumeOverlayController != null) {
            volumeOverlayController.applyTheme(palette);
        }
    }

    private void styleActionButton(
            TextView button,
            String stableKey,
            int fallbackColor,
            LauncherThemePalette palette
    ) {
        if (button == null) {
            return;
        }

        int fillColor = ColorblindStyleHelper.resolveSemanticAccentColor(
                stableKey,
                fallbackColor,
                palette
        );
        button.setTextColor(ColorblindStyleHelper.resolveTextColorForBackground(fillColor));
        button.setBackground(ColorblindStyleHelper.createRoundedBackground(
                this,
                fillColor,
                fillColor,
                26,
                ColorblindStyleHelper.isColorblindMode(palette) ? 3 : 0
        ));
    }

    private void updateCurrentTime() {
        TextView nowView = findViewById(R.id.alarm_now_view);
        if (nowView == null) {
            return;
        }

        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        nowView.setText(getString(R.string.alarm_now_label, currentTime));
    }

    private void openSystemAlarms() {
        if (launchExternalIntent(new Intent(AlarmClock.ACTION_SHOW_ALARMS))) {
            return;
        }

        Intent clockIntent = new Intent(Intent.ACTION_MAIN);
        clockIntent.addCategory("android.intent.category.APP_ALARM");
        if (launchExternalIntent(clockIntent)) {
            return;
        }

        showAlarmError();
    }

    private void createAlarm() {
        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
        if (launchExternalIntent(intent)) {
            return;
        }

        showAlarmError();
    }

    private void startTimer(int minutes, String label) {
        Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER);
        intent.putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60);
        intent.putExtra(AlarmClock.EXTRA_MESSAGE, label);
        intent.putExtra(AlarmClock.EXTRA_SKIP_UI, false);
        if (launchExternalIntent(intent)) {
            return;
        }

        showAlarmError();
    }

    private boolean launchExternalIntent(Intent intent) {
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

    private void showAlarmError() {
        Toast.makeText(this, R.string.alarm_open_error, Toast.LENGTH_SHORT).show();
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
