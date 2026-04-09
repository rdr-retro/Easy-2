package com.rdr.easy2;

import android.graphics.Typeface;
import android.inputmethodservice.InputMethodService;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Easy2InputMethodService extends InputMethodService {
    private final List<TextView> characterKeys = new ArrayList<>();

    private LinearLayout keyboardRootView;
    private View handleView;
    private TextView shiftKeyView;
    private TextView deleteKeyView;
    private TextView hideKeyView;
    private TextView spaceKeyView;
    private TextView enterKeyView;
    private TextView voiceKeyView;
    private Easy2KeyboardVoiceGuide voiceGuide;
    private boolean shifted;

    @Override
    public View onCreateInputView() {
        voiceGuide = new Easy2KeyboardVoiceGuide(this, keyboardRootView);
        keyboardRootView = new LinearLayout(this);
        keyboardRootView.setOrientation(LinearLayout.VERTICAL);
        keyboardRootView.setClipToPadding(false);
        keyboardRootView.setPadding(0, 0, 0, 0);
        handleView = null;
        voiceGuide = new Easy2KeyboardVoiceGuide(this, keyboardRootView);

        addCharacterRow(new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"});
        addCharacterRow(new String[]{"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"});
        addCharacterRow(new String[]{"a", "s", "d", "f", "g", "h", "j", "k", "l", "ñ"});

        LinearLayout fourthRow = createRow();
        shiftKeyView = addActionKey(
                fourthRow,
                getString(R.string.keyboard_shift),
                1.35f,
                view -> toggleShift()
        );
        addCharacterKeys(fourthRow, new String[]{"z", "x", "c", "v", "b", "n", "m"}, 1f);
        deleteKeyView = addActionKey(
                fourthRow,
                getString(R.string.keyboard_delete),
                1.6f,
                view -> deleteCharacter()
        );
        keyboardRootView.addView(fourthRow);

        LinearLayout fifthRow = createRow();
        hideKeyView = addActionKey(
                fifthRow,
                getString(R.string.keyboard_hide),
                1.45f,
                view -> {
                    voiceGuide.performKeyFeedback();
                    voiceGuide.speakMessage(R.string.keyboard_spoken_hide);
                    requestHideSelf(0);
                }
        );
        voiceKeyView = addActionKey(
                fifthRow,
                getString(R.string.keyboard_voice_off_label),
                1.25f,
                view -> toggleVoiceGuide()
        );
        addCharacterKey(fifthRow, ",", 0.82f);
        spaceKeyView = addActionKey(
                fifthRow,
                getString(R.string.keyboard_space),
                2.85f,
                view -> handleCharacterPress(" ")
        );
        addCharacterKey(fifthRow, ".", 0.82f);
        enterKeyView = addActionKey(
                fifthRow,
                getString(R.string.keyboard_enter),
                1.35f,
                view -> handleEnter()
        );
        keyboardRootView.addView(fifthRow);

        if (voiceGuide != null) {
            voiceGuide.refreshPreference();
        }
        applyThemePalette();
        updateCharacterKeyLabels();
        return keyboardRootView;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (voiceGuide != null) {
            voiceGuide.refreshPreference();
        }
        applyThemePalette();
        updateCharacterKeyLabels();
    }

    @Override
    public void onDestroy() {
        if (voiceGuide != null) {
            voiceGuide.release();
            voiceGuide = null;
        }
        super.onDestroy();
    }

    private void addCharacterRow(String[] keys) {
        LinearLayout row = createRow();
        addCharacterKeys(row, keys, 1f);
        keyboardRootView.addView(row);
    }

    private void addCharacterKeys(LinearLayout row, String[] keys, float weight) {
        for (String key : keys) {
            addCharacterKey(row, key, weight);
        }
    }

    private void addCharacterKey(LinearLayout row, String value, float weight) {
        TextView keyView = createBaseKey(weight);
        keyView.setTag(value);
        keyView.setOnClickListener(view -> handleCharacterPress((String) view.getTag()));
        row.addView(keyView);
        characterKeys.add(keyView);
    }

    private TextView addActionKey(
            LinearLayout row,
            String label,
            float weight,
            View.OnClickListener listener
    ) {
        TextView keyView = createBaseKey(weight);
        keyView.setText(label);
        keyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        keyView.setLetterSpacing(0.01f);
        keyView.setOnClickListener(listener);
        row.addView(keyView);
        return keyView;
    }

    private LinearLayout createRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        row.setClipToPadding(false);
        return row;
    }

    private TextView createBaseKey(float weight) {
        TextView keyView = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                dpToPx(58),
                weight
        );
        keyView.setLayoutParams(params);
        keyView.setGravity(Gravity.CENTER);
        keyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        keyView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        keyView.setLetterSpacing(0.015f);
        keyView.setIncludeFontPadding(false);
        return keyView;
    }

    private void applyThemePalette() {
        if (keyboardRootView == null) {
            return;
        }

        LauncherThemePalette palette = LauncherThemePalette.fromPreferences(this);
        Easy2KeyboardStyler.stylePanel(keyboardRootView, palette);
        Easy2KeyboardStyler.styleHandle(handleView, palette);

        for (TextView keyView : characterKeys) {
            Easy2KeyboardStyler.styleCharacterKey(keyView, palette);
        }

        Easy2KeyboardStyler.styleSpaceKey(spaceKeyView, palette);
        Easy2KeyboardStyler.styleEnterKey(enterKeyView, palette);
        Easy2KeyboardStyler.styleHideKey(hideKeyView, palette);
        Easy2KeyboardStyler.styleDeleteKey(deleteKeyView);
        Easy2KeyboardStyler.styleShiftKey(shiftKeyView, palette, shifted);
        Easy2KeyboardStyler.styleVoiceKey(
                voiceKeyView,
                palette,
                voiceGuide != null && voiceGuide.isEnabled()
        );
        if (voiceKeyView != null) {
            voiceKeyView.setText(getString(
                    voiceGuide != null && voiceGuide.isEnabled()
                            ? R.string.keyboard_voice_on_label
                            : R.string.keyboard_voice_off_label
            ));
        }
    }

    private void commitText(String text) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null || text == null) {
            return;
        }
        inputConnection.commitText(text, 1);
    }

    private void deleteCharacter() {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null) {
            return;
        }
        inputConnection.deleteSurroundingText(1, 0);
    }

    private void handleEnter() {
        if (voiceGuide != null) {
            voiceGuide.performKeyFeedback();
            voiceGuide.speakMessage(R.string.keyboard_spoken_enter);
        }
        if (sendDefaultEditorAction(true)) {
            return;
        }
        commitText("\n");
    }

    private void toggleShift() {
        shifted = !shifted;
        if (voiceGuide != null) {
            voiceGuide.performKeyFeedback();
            voiceGuide.speakMessage(
                    shifted
                            ? R.string.keyboard_spoken_shift_on
                            : R.string.keyboard_spoken_shift_off
            );
        }
        applyThemePalette();
        updateCharacterKeyLabels();
    }

    private void handleCharacterPress(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        if (isNumericEditor() && !isNumericValue(value)) {
            if (voiceGuide != null) {
                voiceGuide.performKeyFeedback();
                voiceGuide.speakMessage(R.string.keyboard_numbers_only);
            }
            return;
        }

        String textToCommit = shifted && isAlphabeticValue(value)
                ? value.toUpperCase(Locale.getDefault())
                : value.toLowerCase(Locale.getDefault());
        commitText(textToCommit);
        if (voiceGuide != null) {
            voiceGuide.performKeyFeedback();
            voiceGuide.speakTypedValue(textToCommit);
        }
    }

    private void toggleVoiceGuide() {
        if (voiceGuide == null) {
            return;
        }

        voiceGuide.performKeyFeedback();
        boolean enabled = voiceGuide.toggleEnabled();
        if (enabled) {
            voiceGuide.performKeyFeedback();
        }
        applyThemePalette();
    }

    private void updateCharacterKeyLabels() {
        for (TextView keyView : characterKeys) {
            String value = (String) keyView.getTag();
            if (value == null) {
                continue;
            }
            if (isAlphabeticValue(value)) {
                keyView.setText(shifted ? value.toUpperCase() : value.toLowerCase());
            } else {
                keyView.setText(value);
            }
        }
    }

    private boolean isNumericEditor() {
        EditorInfo info = getCurrentInputEditorInfo();
        if (info == null) {
            return false;
        }
        return (info.inputType & EditorInfo.TYPE_CLASS_NUMBER) == EditorInfo.TYPE_CLASS_NUMBER;
    }

    private boolean isAlphabeticValue(String value) {
        return value.length() == 1 && Character.isLetter(value.charAt(0));
    }

    private boolean isNumericValue(String value) {
        return value.length() == 1 && Character.isDigit(value.charAt(0));
    }

    private int dpToPx(int dpValue) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                getResources().getDisplayMetrics()
        ));
    }
}
