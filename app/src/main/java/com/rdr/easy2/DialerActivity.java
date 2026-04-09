package com.rdr.easy2;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.app.role.RoleManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class DialerActivity extends AppCompatActivity {
    private static final String EXTRA_PHONE_NUMBER = "extra_phone_number";
    private static final String EXTRA_AUTO_CALL = "extra_auto_call";
    private static final String EMERGENCY_NUMBER = "112";

    private View rootView;
    private View displayCard;
    private TextView titleView;
    private TextView numberView;
    private TextView deleteButton;
    private TextView callButton;
    private VolumeOverlayController volumeOverlayController;
    private ActivityResultLauncher<Intent> dialerRoleLauncher;
    private boolean autoCallRequested;

    public static Intent createEmergencyIntent(Context context) {
        Intent intent = new Intent(context, DialerActivity.class);
        intent.putExtra(EXTRA_PHONE_NUMBER, EMERGENCY_NUMBER);
        return intent;
    }

    public static Intent createAutoCallIntent(Context context, String phoneNumber) {
        Intent intent = new Intent(context, DialerActivity.class);
        intent.putExtra(EXTRA_PHONE_NUMBER, phoneNumber);
        intent.putExtra(EXTRA_AUTO_CALL, true);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialer);

        rootView = findViewById(R.id.dialer_root);
        displayCard = findViewById(R.id.dialer_display_card);
        titleView = findViewById(R.id.dialer_title_view);
        numberView = findViewById(R.id.dialer_number_view);
        deleteButton = findViewById(R.id.dialer_delete_button);
        callButton = findViewById(R.id.dialer_call_button);
        volumeOverlayController = new VolumeOverlayController(this);

        dialerRoleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK
                            && DialerRoleHelper.isDialerRoleHeld(this)) {
                        placeCurrentCall();
                    } else {
                        Toast.makeText(
                                this,
                                R.string.dialer_role_needed,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        bindDigitButton(R.id.dialer_digit_1, "1");
        bindDigitButton(R.id.dialer_digit_2, "2");
        bindDigitButton(R.id.dialer_digit_3, "3");
        bindDigitButton(R.id.dialer_digit_4, "4");
        bindDigitButton(R.id.dialer_digit_5, "5");
        bindDigitButton(R.id.dialer_digit_6, "6");
        bindDigitButton(R.id.dialer_digit_7, "7");
        bindDigitButton(R.id.dialer_digit_8, "8");
        bindDigitButton(R.id.dialer_digit_9, "9");
        bindDigitButton(R.id.dialer_digit_star, "*");
        bindDigitButton(R.id.dialer_digit_0, "0");
        bindDigitButton(R.id.dialer_digit_hash, "#");

        if (deleteButton != null) {
            deleteButton.setOnClickListener(view -> deleteLastDigit());
        }
        if (callButton != null) {
            callButton.setOnClickListener(view -> startCallFlow());
        }
        findViewById(R.id.close_dialer_button).setOnClickListener(view -> finish());

        applyIntent(getIntent());
        applyThemePalette();
        enableFullscreen();

        if (autoCallRequested && numberView != null && !TextUtils.isEmpty(numberView.getText())) {
            numberView.post(this::startCallFlow);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyIntent(intent);
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

    private void applyIntent(Intent intent) {
        autoCallRequested = intent != null && intent.getBooleanExtra(EXTRA_AUTO_CALL, false);
        String phoneNumber = "";
        if (intent != null) {
            phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER);
            if (TextUtils.isEmpty(phoneNumber) && intent.getData() != null) {
                phoneNumber = intent.getData().getSchemeSpecificPart();
            }
        }

        if (TextUtils.isEmpty(phoneNumber)) {
            phoneNumber = "";
        }
        if (numberView != null) {
            numberView.setText(phoneNumber);
        }
    }

    private void bindDigitButton(int viewId, String symbol) {
        TextView digitButton = findViewById(viewId);
        if (digitButton == null) {
            return;
        }
        digitButton.setOnClickListener(view -> appendDigit(symbol));
    }

    private void appendDigit(String symbol) {
        if (numberView == null) {
            return;
        }
        String currentValue = numberView.getText().toString();
        numberView.setText(currentValue + symbol);
    }

    private void deleteLastDigit() {
        if (numberView == null) {
            return;
        }
        String currentValue = numberView.getText().toString();
        if (currentValue.isEmpty()) {
            return;
        }
        numberView.setText(currentValue.substring(0, currentValue.length() - 1));
    }

    private void startCallFlow() {
        String currentNumber = numberView != null
                ? numberView.getText().toString().trim()
                : "";
        if (TextUtils.isEmpty(currentNumber)) {
            Toast.makeText(this, R.string.dialer_empty_number, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!DialerRoleHelper.isDialerRoleHeld(this)) {
            requestDialerRole();
            return;
        }

        placeCurrentCall();
    }

    private void placeCurrentCall() {
        String currentNumber = numberView != null
                ? numberView.getText().toString().trim()
                : "";
        if (TextUtils.isEmpty(currentNumber)) {
            return;
        }

        android.telecom.TelecomManager telecomManager = getSystemService(android.telecom.TelecomManager.class);
        if (telecomManager == null) {
            Toast.makeText(this, R.string.dialer_call_error, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            telecomManager.placeCall(Uri.fromParts("tel", currentNumber, null), new Bundle());
        } catch (SecurityException | IllegalArgumentException exception) {
            Toast.makeText(this, R.string.dialer_call_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void requestDialerRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(this, R.string.dialer_role_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        RoleManager roleManager = getSystemService(RoleManager.class);
        if (roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
            Toast.makeText(this, R.string.dialer_role_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        dialerRoleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER));
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
        if (titleView != null) {
            titleView.setTextColor(palette.getHeadingColor());
        }
        if (numberView != null) {
            numberView.setTextColor(palette.getHeadingColor());
        }
        if (displayCard != null) {
            displayCard.setBackground(createRoundedBackground(
                    palette.getSetupFieldFillColor(),
                    palette.getSetupFieldStrokeColor(),
                    28,
                    2
            ));
        }

        styleDialerButton(findViewById(R.id.dialer_digit_1), palette.getCircleColor());
        styleDialerButton(findViewById(R.id.dialer_digit_2), palette.getCircleColor());
        styleDialerButton(findViewById(R.id.dialer_digit_3), palette.getCircleColor());
        styleDialerButton(findViewById(R.id.dialer_digit_4), palette.getCircleColor());
        styleDialerButton(findViewById(R.id.dialer_digit_5), palette.getCircleColor());
        styleDialerButton(findViewById(R.id.dialer_digit_6), palette.getCircleColor());
        styleDialerButton(findViewById(R.id.dialer_digit_7), palette.getCircleColor());
        styleDialerButton(findViewById(R.id.dialer_digit_8), palette.getCircleColor());
        styleDialerButton(findViewById(R.id.dialer_digit_9), palette.getCircleColor());
        styleDialerButton(findViewById(R.id.dialer_digit_star), palette.getCircleColor());
        styleDialerButton(findViewById(R.id.dialer_digit_0), palette.getCircleColor());
        styleDialerButton(findViewById(R.id.dialer_digit_hash), palette.getCircleColor());
        styleActionButton(deleteButton, "dialer_delete", palette.getChipColor(), palette);

        if (callButton != null) {
            int callButtonFill = ColorblindStyleHelper.resolveSemanticAccentColor(
                    "dialer_call",
                    0xFF2EAD4E,
                    palette
            );
            callButton.setBackground(createRoundedBackground(
                    callButtonFill,
                    callButtonFill,
                    28,
                    ColorblindStyleHelper.isColorblindMode(palette) ? 3 : 0
            ));
            callButton.setTextColor(
                    ColorblindStyleHelper.resolveTextColorForBackground(callButtonFill)
            );
        }

        if (volumeOverlayController != null) {
            volumeOverlayController.applyTheme(palette);
        }
    }

    private void styleDialerButton(View buttonView, int fillColor) {
        if (!(buttonView instanceof TextView)) {
            return;
        }
        TextView button = (TextView) buttonView;
        button.setBackground(createCircleBackground(fillColor));
        button.setTextColor(Color.WHITE);
    }

    private void styleActionButton(
            View buttonView,
            String stableKey,
            int fallbackColor,
            LauncherThemePalette palette
    ) {
        if (!(buttonView instanceof TextView)) {
            return;
        }
        TextView button = (TextView) buttonView;
        int fillColor = ColorblindStyleHelper.resolveSemanticAccentColor(
                stableKey,
                fallbackColor,
                palette
        );
        button.setBackground(createRoundedBackground(
                fillColor,
                fillColor,
                28,
                ColorblindStyleHelper.isColorblindMode(palette) ? 3 : 0
        ));
        button.setTextColor(ColorblindStyleHelper.resolveTextColorForBackground(fillColor));
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

    private GradientDrawable createCircleBackground(int fillColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(fillColor);
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
