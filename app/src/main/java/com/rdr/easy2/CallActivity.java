package com.rdr.easy2;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.VideoProfile;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class CallActivity extends AppCompatActivity implements CallRepository.Listener {
    private View rootView;
    private View callerCardView;
    private TextView titleView;
    private TextView callerNameView;
    private TextView callStateView;
    private TextView answerButton;
    private TextView muteButton;
    private TextView speakerButton;
    private TextView endCallButton;
    private VolumeOverlayController volumeOverlayController;

    public static android.content.Intent createIntent(android.content.Context context) {
        return new android.content.Intent(context, CallActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        rootView = findViewById(R.id.call_root);
        callerCardView = findViewById(R.id.call_caller_card);
        titleView = findViewById(R.id.call_title_view);
        callerNameView = findViewById(R.id.call_caller_name);
        callStateView = findViewById(R.id.call_state_view);
        answerButton = findViewById(R.id.call_answer_button);
        muteButton = findViewById(R.id.call_mute_button);
        speakerButton = findViewById(R.id.call_speaker_button);
        endCallButton = findViewById(R.id.call_end_button);
        volumeOverlayController = new VolumeOverlayController(this);

        if (answerButton != null) {
            answerButton.setOnClickListener(view -> answerCurrentCall());
        }
        if (muteButton != null) {
            muteButton.setOnClickListener(view -> toggleMute());
        }
        if (speakerButton != null) {
            speakerButton.setOnClickListener(view -> toggleSpeaker());
        }
        if (endCallButton != null) {
            endCallButton.setOnClickListener(view -> endCurrentCall());
        }

        applyThemePalette();
        enableFullscreen();
    }

    @Override
    protected void onStart() {
        super.onStart();
        CallRepository.registerListener(this);
        renderCurrentCall(CallRepository.getCurrentCall());
    }

    @Override
    protected void onStop() {
        CallRepository.unregisterListener(this);
        super.onStop();
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

    @Override
    public void onCallUpdated(Call call) {
        runOnUiThread(() -> renderCurrentCall(call));
    }

    @Override
    public void onCallCleared() {
        runOnUiThread(this::finish);
    }

    private void renderCurrentCall(Call call) {
        if (call == null) {
            finish();
            return;
        }

        if (callerNameView != null) {
            callerNameView.setText(resolveDisplayName(call));
        }
        if (callStateView != null) {
            callStateView.setText(resolveCallState(call.getState()));
        }
        if (answerButton != null) {
            answerButton.setVisibility(call.getState() == Call.STATE_RINGING ? View.VISIBLE : View.GONE);
        }
        if (muteButton != null) {
            updateMuteButton();
        }
        if (speakerButton != null) {
            updateSpeakerButton();
        }
    }

    private String resolveDisplayName(Call call) {
        Call.Details details = call.getDetails();
        if (details == null) {
            return getString(R.string.call_unknown_number);
        }

        CharSequence callerName = details.getCallerDisplayName();
        if (!TextUtils.isEmpty(callerName)) {
            return callerName.toString();
        }

        Uri handle = details.getHandle();
        if (handle != null && !TextUtils.isEmpty(handle.getSchemeSpecificPart())) {
            String phoneNumber = handle.getSchemeSpecificPart();
            String contactName = findContactNameForNumber(phoneNumber);
            if (!TextUtils.isEmpty(contactName)) {
                return contactName;
            }
            return phoneNumber;
        }

        return getString(R.string.call_unknown_number);
    }

    private String findContactNameForNumber(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return null;
        }

        Cursor cursor = null;
        try {
            Uri lookupUri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(phoneNumber)
            );
            cursor = getContentResolver().query(
                    lookupUri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                    null,
                    null,
                    null
            );

            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }

            int nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME);
            return cursor.getString(nameIndex);
        } catch (SecurityException ignored) {
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String resolveCallState(int state) {
        switch (state) {
            case Call.STATE_NEW:
            case Call.STATE_CONNECTING:
            case Call.STATE_SELECT_PHONE_ACCOUNT:
                return getString(R.string.call_state_connecting);
            case Call.STATE_DIALING:
                return getString(R.string.call_state_dialing);
            case Call.STATE_RINGING:
                return getString(R.string.call_state_incoming);
            case Call.STATE_ACTIVE:
                return getString(R.string.call_state_active);
            case Call.STATE_HOLDING:
                return getString(R.string.call_state_on_hold);
            case Call.STATE_DISCONNECTED:
                return getString(R.string.call_state_finished);
            default:
                return getString(R.string.call_state_connecting);
        }
    }

    private void answerCurrentCall() {
        Call currentCall = CallRepository.getCurrentCall();
        if (currentCall == null) {
            return;
        }
        currentCall.answer(VideoProfile.STATE_AUDIO_ONLY);
    }

    private void endCurrentCall() {
        Call currentCall = CallRepository.getCurrentCall();
        if (currentCall == null) {
            finish();
            return;
        }

        int state = currentCall.getState();
        if (state == Call.STATE_RINGING) {
            currentCall.reject(false, null);
        } else {
            currentCall.disconnect();
        }
    }

    private void toggleMute() {
        Easy2InCallService service = Easy2InCallService.getInstance();
        if (service == null) {
            return;
        }
        CallAudioState state = Easy2InCallService.getCurrentAudioState();
        boolean muted = state != null && state.isMuted();
        service.setMuted(!muted);
        updateMuteButton();
    }

    private void toggleSpeaker() {
        Easy2InCallService service = Easy2InCallService.getInstance();
        if (service == null) {
            return;
        }
        CallAudioState state = Easy2InCallService.getCurrentAudioState();
        int currentRoute = state != null ? state.getRoute() : CallAudioState.ROUTE_EARPIECE;
        int nextRoute = (currentRoute & CallAudioState.ROUTE_SPEAKER) != 0
                ? CallAudioState.ROUTE_EARPIECE
                : CallAudioState.ROUTE_SPEAKER;
        service.setAudioRoute(nextRoute);
        updateSpeakerButton();
    }

    private void updateMuteButton() {
        if (muteButton == null) {
            return;
        }
        CallAudioState state = Easy2InCallService.getCurrentAudioState();
        boolean muted = state != null && state.isMuted();
        muteButton.setText(muted
                ? getString(R.string.call_unmute)
                : getString(R.string.call_mute));
        muteButton.setAlpha(muted ? 1f : 0.9f);
    }

    private void updateSpeakerButton() {
        if (speakerButton == null) {
            return;
        }
        CallAudioState state = Easy2InCallService.getCurrentAudioState();
        boolean speakerOn = state != null
                && (state.getRoute() & CallAudioState.ROUTE_SPEAKER) != 0;
        speakerButton.setText(speakerOn
                ? getString(R.string.call_speaker_off)
                : getString(R.string.call_speaker_on));
        speakerButton.setAlpha(speakerOn ? 1f : 0.9f);
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
        if (callerCardView != null) {
            callerCardView.setBackground(createRoundedBackground(
                    palette.getSetupFieldFillColor(),
                    palette.getSetupFieldStrokeColor(),
                    28,
                    2
            ));
        }
        if (titleView != null) {
            titleView.setTextColor(palette.getHeadingColor());
        }
        if (callerNameView != null) {
            callerNameView.setTextColor(palette.getHeadingColor());
        }
        if (callStateView != null) {
            callStateView.setTextColor(palette.getBodyTextColor());
        }

        stylePillButton(answerButton, 0xFF2EAD4E);
        stylePillButton(muteButton, palette.getChipColor());
        stylePillButton(speakerButton, palette.getChipColor());
        stylePillButton(endCallButton, 0xFFD84343);

        if (volumeOverlayController != null) {
            volumeOverlayController.applyTheme(palette);
        }
    }

    private void stylePillButton(TextView button, int fillColor) {
        if (button == null) {
            return;
        }
        button.setBackground(createRoundedBackground(fillColor, fillColor, 24, 0));
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
