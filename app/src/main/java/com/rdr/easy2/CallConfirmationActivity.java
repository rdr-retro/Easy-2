package com.rdr.easy2;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class CallConfirmationActivity extends AppCompatActivity {
    private static final String EXTRA_CONTACT_NAME = "extra_contact_name";
    private static final String EXTRA_PHONE_NUMBER = "extra_phone_number";

    private View rootView;
    private View cardView;
    private TextView titleView;
    private TextView nameView;
    private TextView numberView;
    private TextView noButton;
    private TextView yesButton;
    private VolumeOverlayController volumeOverlayController;

    public static Intent createIntent(
            android.content.Context context,
            String contactName,
            String phoneNumber
    ) {
        Intent intent = new Intent(context, CallConfirmationActivity.class);
        intent.putExtra(EXTRA_CONTACT_NAME, contactName);
        intent.putExtra(EXTRA_PHONE_NUMBER, phoneNumber);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_confirmation);

        rootView = findViewById(R.id.call_confirmation_root);
        cardView = findViewById(R.id.call_confirmation_card);
        titleView = findViewById(R.id.call_confirmation_title);
        nameView = findViewById(R.id.call_confirmation_name);
        numberView = findViewById(R.id.call_confirmation_number);
        noButton = findViewById(R.id.call_confirmation_no_button);
        yesButton = findViewById(R.id.call_confirmation_yes_button);
        volumeOverlayController = new VolumeOverlayController(this);

        bindContent();
        applyThemePalette();

        if (noButton != null) {
            noButton.setOnClickListener(view -> finish());
        }
        if (yesButton != null) {
            yesButton.setOnClickListener(view -> confirmCall());
        }

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

    private void bindContent() {
        Intent intent = getIntent();
        String contactName = intent != null ? intent.getStringExtra(EXTRA_CONTACT_NAME) : "";
        String phoneNumber = intent != null ? intent.getStringExtra(EXTRA_PHONE_NUMBER) : "";

        if (nameView != null) {
            nameView.setText(TextUtils.isEmpty(contactName)
                    ? getString(R.string.call_confirmation_unknown_contact)
                    : contactName);
        }
        if (numberView != null) {
            numberView.setText(phoneNumber);
        }
    }

    private void confirmCall() {
        Intent intent = getIntent();
        String phoneNumber = intent != null ? intent.getStringExtra(EXTRA_PHONE_NUMBER) : "";
        if (TextUtils.isEmpty(phoneNumber)) {
            finish();
            return;
        }
        startActivity(DialerActivity.createAutoCallIntent(this, phoneNumber));
        finish();
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

        if (rootView != null) {
            rootView.setBackgroundColor(palette.getBackgroundColor());
        }
        if (cardView != null) {
            cardView.setBackground(createRoundedBackground(
                    palette.getSetupFieldFillColor(),
                    palette.getSetupFieldStrokeColor(),
                    30,
                    2
            ));
        }
        if (titleView != null) {
            titleView.setTextColor(palette.getHeadingColor());
        }
        if (nameView != null) {
            nameView.setTextColor(palette.getHeadingColor());
        }
        if (numberView != null) {
            numberView.setTextColor(palette.getBodyTextColor());
        }
        styleButton(noButton, 0xFFD84343);
        styleButton(yesButton, 0xFF2EAD4E);

        if (volumeOverlayController != null) {
            volumeOverlayController.applyTheme(palette);
        }
    }

    private void styleButton(TextView button, int fillColor) {
        if (button == null) {
            return;
        }
        button.setBackground(createRoundedBackground(fillColor, fillColor, 26, 0));
        button.setTextColor(Color.WHITE);
    }

    private GradientDrawable createRoundedBackground(
            int fillColor,
            int strokeColor,
            int radiusDp,
            int strokeWidthDp
    ) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dpToPx(radiusDp));
        drawable.setColor(fillColor);
        if (strokeWidthDp > 0) {
            drawable.setStroke(Math.round(dpToPx(strokeWidthDp)), strokeColor);
        }
        return drawable;
    }

    private float dpToPx(int dpValue) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                getResources().getDisplayMetrics()
        );
    }
}
