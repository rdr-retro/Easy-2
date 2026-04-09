package com.rdr.easy2;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class UtilitiesActivity extends AppCompatActivity {
    private VolumeOverlayController volumeOverlayController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utilities);
        volumeOverlayController = new VolumeOverlayController(this);
        findViewById(R.id.close_utilities_button).setOnClickListener(view -> finish());
        View.OnClickListener openCalendarListener =
                view -> startActivity(new Intent(this, SeniorCalendarActivity.class));
        View.OnClickListener openNotesListener =
                view -> startActivity(new Intent(this, NotesActivity.class));
        View.OnClickListener openFoodScannerListener =
                view -> startActivity(new Intent(this, FoodScannerActivity.class));
        View.OnClickListener openHearingListener =
                view -> startActivity(new Intent(this, HearingAssistActivity.class));
        View.OnClickListener openAlarmListener =
                view -> startActivity(new Intent(this, AlarmActivity.class));
        findViewById(R.id.utility_calendar_circle).setOnClickListener(openCalendarListener);
        findViewById(R.id.utility_calendar_label).setOnClickListener(openCalendarListener);
        findViewById(R.id.utility_note_circle).setOnClickListener(openNotesListener);
        findViewById(R.id.utility_note_label).setOnClickListener(openNotesListener);
        findViewById(R.id.utility_scan_food_circle).setOnClickListener(openFoodScannerListener);
        findViewById(R.id.utility_scan_food_label).setOnClickListener(openFoodScannerListener);
        findViewById(R.id.utility_hearing_circle).setOnClickListener(openHearingListener);
        findViewById(R.id.utility_hearing_label).setOnClickListener(openHearingListener);
        findViewById(R.id.utility_alarm_circle).setOnClickListener(openAlarmListener);
        findViewById(R.id.utility_alarm_label).setOnClickListener(openAlarmListener);
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
        applyThemePalette();
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

    private void applyThemePalette() {
        LauncherThemePalette palette = LauncherThemePalette.fromPreferences(this);

        View rootView = findViewById(R.id.utilities_root);
        TextView titleView = findViewById(R.id.utilities_title_view);
        FrameLayout calendarCircle = findViewById(R.id.utility_calendar_circle);
        ImageView calendarIcon = findViewById(R.id.utility_calendar_icon);
        TextView calendarLabel = findViewById(R.id.utility_calendar_label);
        FrameLayout noteCircle = findViewById(R.id.utility_note_circle);
        ImageView noteIcon = findViewById(R.id.utility_note_icon);
        TextView noteLabel = findViewById(R.id.utility_note_label);
        FrameLayout scanFoodCircle = findViewById(R.id.utility_scan_food_circle);
        ImageView scanFoodIcon = findViewById(R.id.utility_scan_food_icon);
        TextView scanFoodLabel = findViewById(R.id.utility_scan_food_label);
        FrameLayout hearingCircle = findViewById(R.id.utility_hearing_circle);
        ImageView hearingIcon = findViewById(R.id.utility_hearing_icon);
        TextView hearingLabel = findViewById(R.id.utility_hearing_label);
        FrameLayout alarmCircle = findViewById(R.id.utility_alarm_circle);
        ImageView alarmIcon = findViewById(R.id.utility_alarm_icon);
        TextView alarmLabel = findViewById(R.id.utility_alarm_label);

        if (rootView != null) {
            rootView.setBackgroundColor(palette.getBackgroundColor());
        }
        if (titleView != null) {
            titleView.setTextColor(palette.getHeadingColor());
        }
        applyUtilityStyle("utility_calendar", calendarCircle, calendarIcon, calendarLabel, palette);
        applyUtilityStyle("utility_note", noteCircle, noteIcon, noteLabel, palette);
        applyUtilityStyle("utility_scan_food", scanFoodCircle, scanFoodIcon, scanFoodLabel, palette);
        applyUtilityStyle("utility_hearing", hearingCircle, hearingIcon, hearingLabel, palette);
        applyUtilityStyle("utility_alarm", alarmCircle, alarmIcon, alarmLabel, palette);
        if (volumeOverlayController != null) {
            volumeOverlayController.applyTheme(palette);
        }
    }

    private void applyUtilityStyle(
            String stableKey,
            FrameLayout circleView,
            ImageView iconView,
            TextView labelView,
            LauncherThemePalette palette
    ) {
        int accentColor = ColorblindStyleHelper.resolveAppAccentColor(stableKey, palette);
        boolean colorblindMode = ColorblindStyleHelper.isColorblindMode(palette);

        if (circleView != null) {
            circleView.setBackground(ColorblindStyleHelper.createCircleBackground(
                    this,
                    ColorblindStyleHelper.resolveAppSurfaceColor(stableKey, palette),
                    accentColor,
                    colorblindMode ? 4 : 0
            ));
        }
        ColorblindStyleHelper.applyIconColor(iconView, stableKey, palette);
        if (labelView != null) {
            labelView.setBackground(ColorblindStyleHelper.createRoundedBackground(
                    this,
                    ColorblindStyleHelper.resolveAppLabelColor(stableKey, palette),
                    colorblindMode ? accentColor : Color.TRANSPARENT,
                    18,
                    0
            ));
            labelView.setTextColor(ColorblindStyleHelper.resolveTextColorOnAccent(
                    stableKey,
                    palette
            ));
        }
    }
}
