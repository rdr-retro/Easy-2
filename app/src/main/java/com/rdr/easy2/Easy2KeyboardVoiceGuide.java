package com.rdr.easy2;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.EditText;

import java.util.Locale;

public final class Easy2KeyboardVoiceGuide {
    private static final String UTTERANCE_ID = "easy2_keyboard_voice";

    private final Context appContext;
    private final View feedbackView;

    private TextToSpeech textToSpeech;
    private boolean textToSpeechReady;
    private boolean enabled;

    public Easy2KeyboardVoiceGuide(Context context, View feedbackView) {
        this.appContext = context.getApplicationContext();
        this.feedbackView = feedbackView;
        this.enabled = LauncherPreferences.isKeyboardVoiceGuideEnabled(appContext);
        this.textToSpeech = new TextToSpeech(appContext, status -> {
            textToSpeechReady = status == TextToSpeech.SUCCESS;
            if (!textToSpeechReady || textToSpeech == null) {
                return;
            }

            Locale preferredLocale = Locale.getDefault();
            int result = textToSpeech.setLanguage(preferredLocale);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech.setLanguage(Locale.forLanguageTag("es-ES"));
            }
            textToSpeech.setSpeechRate(0.92f);
            textToSpeech.setPitch(1f);
        });
    }

    public void refreshPreference() {
        enabled = LauncherPreferences.isKeyboardVoiceGuideEnabled(appContext);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean toggleEnabled() {
        boolean newState = !enabled;
        LauncherPreferences.setKeyboardVoiceGuideEnabled(appContext, newState);
        enabled = newState;
        speakNow(appContext.getString(
                newState
                        ? R.string.keyboard_voice_enabled
                        : R.string.keyboard_voice_disabled
        ), true);
        return newState;
    }

    public void announceInput(EditText editText) {
        if (editText == null) {
            return;
        }

        CharSequence label = editText.getHint();
        if (TextUtils.isEmpty(label)) {
            label = editText.getContentDescription();
        }
        if (TextUtils.isEmpty(label)) {
            return;
        }

        speakNow(label.toString(), false);
    }

    public void speakTypedValue(String value) {
        speakNow(resolveSpokenValue(value), false);
    }

    public void speakMessage(int resId) {
        speakNow(appContext.getString(resId), false);
    }

    public void performKeyFeedback() {
        if (!enabled || feedbackView == null) {
            return;
        }

        feedbackView.performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        );
    }

    public void release() {
        textToSpeechReady = false;
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }

    private void speakNow(String message, boolean force) {
        if ((!enabled && !force) || !textToSpeechReady || TextUtils.isEmpty(message)) {
            return;
        }

        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID);
    }

    private String resolveSpokenValue(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }

        if (" ".equals(value)) {
            return appContext.getString(R.string.keyboard_spoken_space);
        }
        if (",".equals(value)) {
            return appContext.getString(R.string.keyboard_spoken_comma);
        }
        if (".".equals(value)) {
            return appContext.getString(R.string.keyboard_spoken_period);
        }

        return value;
    }
}
