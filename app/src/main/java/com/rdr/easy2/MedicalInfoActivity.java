package com.rdr.easy2;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MedicalInfoActivity extends AppCompatActivity {
    private VolumeOverlayController volumeOverlayController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_info);
        volumeOverlayController = new VolumeOverlayController(this);

        TextView contentView = findViewById(R.id.medical_info_content);
        String medicalInfo = LauncherPreferences.getMedicalInfo(this).trim();
        if (contentView != null) {
            contentView.setText(TextUtils.isEmpty(medicalInfo)
                    ? getString(R.string.medical_info_empty)
                    : medicalInfo);
        }

        findViewById(R.id.close_medical_info_button).setOnClickListener(view -> finish());
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

        View rootView = findViewById(R.id.medical_info_root);
        TextView titleView = findViewById(R.id.medical_info_title_view);
        TextView contentView = findViewById(R.id.medical_info_content);

        if (rootView != null) {
            rootView.setBackgroundColor(palette.getBackgroundColor());
        }
        if (titleView != null) {
            titleView.setTextColor(palette.getHeadingColor());
        }
        if (contentView != null) {
            contentView.setTextColor(palette.getBodyTextColor());
        }
        if (volumeOverlayController != null) {
            volumeOverlayController.applyTheme(palette);
        }
    }
}
