package com.rdr.easy2;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class KeyboardActivationActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyboard_activation);

        TextView titleView = findViewById(R.id.keyboard_activation_title);
        TextView detailView = findViewById(R.id.keyboard_activation_detail);
        TextView settingsButton = findViewById(R.id.keyboard_activation_settings_button);
        TextView pickerButton = findViewById(R.id.keyboard_activation_picker_button);
        TextView closeButton = findViewById(R.id.keyboard_activation_close_button);

        LauncherThemePalette palette = LauncherThemePalette.fromPreferences(this);
        findViewById(R.id.keyboard_activation_root).setBackgroundColor(palette.getBackgroundColor());
        titleView.setTextColor(palette.getHeadingColor());
        detailView.setTextColor(palette.getBodyTextColor());
        settingsButton.setTextColor(Color.WHITE);
        settingsButton.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_save_button));
        pickerButton.setTextColor(Color.WHITE);
        pickerButton.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_save_button));
        closeButton.setTextColor(Color.WHITE);
        closeButton.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_close_button));

        settingsButton.setOnClickListener(view -> openKeyboardSettings());
        pickerButton.setOnClickListener(view -> showKeyboardPicker());
        closeButton.setOnClickListener(view -> finish());
    }

    private void openKeyboardSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
        } catch (Exception exception) {
            Toast.makeText(this, R.string.keyboard_settings_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void showKeyboardPicker() {
        try {
            InputMethodManager inputMethodManager = getSystemService(InputMethodManager.class);
            if (inputMethodManager != null) {
                inputMethodManager.showInputMethodPicker();
                return;
            }
        } catch (Exception exception) {
            // Fall through to toast below.
        }
        Toast.makeText(this, R.string.keyboard_picker_unavailable, Toast.LENGTH_SHORT).show();
    }
}
