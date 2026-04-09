package com.rdr.easy2;

import android.app.Activity;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Easy2KeyboardController {
    private final Activity activity;
    private final LinearLayout keyboardContainer;
    private final View insetTarget;
    private final int originalInsetBottom;
    private final List<EditText> attachedInputs = new ArrayList<>();
    private final List<TextView> characterKeys = new ArrayList<>();
    private final Easy2KeyboardVoiceGuide voiceGuide;

    private View handleView;
    private TextView shiftKeyView;
    private TextView hideKeyView;
    private TextView deleteKeyView;
    private TextView spaceKeyView;
    private TextView enterKeyView;
    private TextView voiceKeyView;
    private EditText currentInput;
    private boolean shifted;
    private int visibleInsetBottom;

    public Easy2KeyboardController(Activity activity, LinearLayout keyboardContainer, View insetTarget) {
        this.activity = activity;
        this.keyboardContainer = keyboardContainer;
        this.insetTarget = insetTarget;
        this.originalInsetBottom = insetTarget.getPaddingBottom();
        this.voiceGuide = new Easy2KeyboardVoiceGuide(activity, keyboardContainer);
        buildKeyboard();
        refreshVoiceGuide();
        hide();
    }

    public void attach(EditText editText) {
        if (editText == null || attachedInputs.contains(editText)) {
            return;
        }

        attachedInputs.add(editText);
        editText.setShowSoftInputOnFocus(false);
        editText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                showFor(editText);
            } else {
                keyboardContainer.post(this::hideIfNoInputHasFocus);
            }
        });
        editText.setOnClickListener(view -> showFor(editText));
    }

    public void applyTheme(LauncherThemePalette palette) {
        Easy2KeyboardStyler.stylePanel(keyboardContainer, palette);
        Easy2KeyboardStyler.styleHandle(handleView, palette);

        for (TextView keyView : characterKeys) {
            Easy2KeyboardStyler.styleCharacterKey(keyView, palette);
        }

        Easy2KeyboardStyler.styleSpaceKey(spaceKeyView, palette);
        Easy2KeyboardStyler.styleEnterKey(enterKeyView, palette);
        Easy2KeyboardStyler.styleHideKey(hideKeyView, palette);
        Easy2KeyboardStyler.styleDeleteKey(deleteKeyView);
        updateShiftKeyTheme(palette);
        updateVoiceKeyTheme(palette);
        updateCharacterKeyLabels();
    }

    public void refreshVoiceGuide() {
        voiceGuide.refreshPreference();
        updateVoiceKeyTheme(LauncherThemePalette.fromPreferences(activity));
    }

    public void release() {
        voiceGuide.release();
    }

    public void hide() {
        keyboardContainer.setVisibility(View.GONE);
        restoreInset();
    }

    public void clearFocusAndHide() {
        clearAttachedInputFocus();
        hideSystemKeyboard();
        hide();
    }

    public boolean handleBackPressed() {
        if (!isVisible() && !hasFocusedInput()) {
            return false;
        }

        clearFocusAndHide();
        return true;
    }

    private void buildKeyboard() {
        keyboardContainer.removeAllViews();
        characterKeys.clear();
        keyboardContainer.setOrientation(LinearLayout.VERTICAL);
        keyboardContainer.setClipToPadding(false);
        keyboardContainer.setPadding(0, 0, 0, 0);
        handleView = null;

        addCharacterRow(new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"});
        addCharacterRow(new String[]{"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"});
        addCharacterRow(new String[]{"a", "s", "d", "f", "g", "h", "j", "k", "l", "ñ"});

        LinearLayout fourthRow = createRow();
        shiftKeyView = addActionKey(
                fourthRow,
                activity.getString(R.string.keyboard_shift),
                1.35f,
                view -> toggleShift()
        );
        addCharacterKeys(fourthRow, new String[]{"z", "x", "c", "v", "b", "n", "m"}, 1f);
        deleteKeyView = addActionKey(
                fourthRow,
                activity.getString(R.string.keyboard_delete),
                1.6f,
                view -> deleteCharacter()
        );
        keyboardContainer.addView(fourthRow);

        LinearLayout fifthRow = createRow();
        hideKeyView = addActionKey(
                fifthRow,
                activity.getString(R.string.keyboard_hide),
                1.45f,
                view -> {
                    voiceGuide.performKeyFeedback();
                    voiceGuide.speakMessage(R.string.keyboard_spoken_hide);
                    clearFocusAndHide();
                }
        );
        voiceKeyView = addActionKey(
                fifthRow,
                activity.getString(R.string.keyboard_voice_off_label),
                1.25f,
                view -> toggleVoiceGuide()
        );
        addCharacterKey(fifthRow, ",", 0.82f);
        spaceKeyView = addActionKey(
                fifthRow,
                activity.getString(R.string.keyboard_space),
                2.85f,
                view -> handleCharacterPress(" ")
        );
        addCharacterKey(fifthRow, ".", 0.82f);
        enterKeyView = addActionKey(
                fifthRow,
                activity.getString(R.string.keyboard_enter),
                1.35f,
                view -> handleEnter()
        );
        keyboardContainer.addView(fifthRow);
    }

    private void addCharacterRow(String[] keys) {
        LinearLayout row = createRow();
        addCharacterKeys(row, keys, 1f);
        keyboardContainer.addView(row);
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
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        row.setClipToPadding(false);
        return row;
    }

    private TextView createBaseKey(float weight) {
        TextView keyView = new TextView(activity);
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

    private void showFor(EditText editText) {
        currentInput = editText;
        refreshVoiceGuide();
        hideSystemKeyboard();
        keyboardContainer.setVisibility(View.VISIBLE);
        keyboardContainer.post(this::updateVisibleInset);
        voiceGuide.announceInput(editText);
    }

    private void hideIfNoInputHasFocus() {
        for (EditText editText : attachedInputs) {
            if (editText.hasFocus()) {
                currentInput = editText;
                return;
            }
        }
        currentInput = null;
        hide();
    }

    private void clearAttachedInputFocus() {
        for (EditText editText : attachedInputs) {
            editText.clearFocus();
        }
        currentInput = null;
    }

    private boolean hasFocusedInput() {
        for (EditText editText : attachedInputs) {
            if (editText.hasFocus()) {
                return true;
            }
        }
        return false;
    }

    private void handleCharacterPress(String value) {
        if (currentInput == null || value == null || value.isEmpty()) {
            return;
        }

        if (isNumericField(currentInput) && !isNumericValue(value)) {
            voiceGuide.performKeyFeedback();
            voiceGuide.speakMessage(R.string.keyboard_numbers_only);
            return;
        }

        String textToInsert = shifted && isAlphabeticValue(value)
                ? value.toUpperCase(Locale.getDefault())
                : value.toLowerCase(Locale.getDefault());
        replaceSelection(textToInsert);
        voiceGuide.performKeyFeedback();
        voiceGuide.speakTypedValue(textToInsert);
    }

    private void deleteCharacter() {
        if (currentInput == null) {
            return;
        }

        Editable editable = currentInput.getText();
        if (editable == null) {
            return;
        }

        int start = Math.max(currentInput.getSelectionStart(), 0);
        int end = Math.max(currentInput.getSelectionEnd(), 0);

        if (start != end) {
            editable.delete(Math.min(start, end), Math.max(start, end));
            voiceGuide.performKeyFeedback();
            voiceGuide.speakMessage(R.string.keyboard_spoken_delete);
            return;
        }

        if (start > 0) {
            editable.delete(start - 1, start);
            voiceGuide.performKeyFeedback();
            voiceGuide.speakMessage(R.string.keyboard_spoken_delete);
        }
    }

    private void handleEnter() {
        if (currentInput == null) {
            return;
        }

        voiceGuide.performKeyFeedback();
        voiceGuide.speakMessage(R.string.keyboard_spoken_enter);
        if (isNumericField(currentInput) || !isMultilineField(currentInput)) {
            moveToNextInput();
            return;
        }

        replaceSelection("\n");
    }

    private void moveToNextInput() {
        if (currentInput == null) {
            return;
        }

        int currentIndex = attachedInputs.indexOf(currentInput);
        if (currentIndex >= 0 && currentIndex < attachedInputs.size() - 1) {
            attachedInputs.get(currentIndex + 1).requestFocus();
            return;
        }

        currentInput.clearFocus();
        hide();
    }

    private void replaceSelection(String insertedText) {
        Editable editable = currentInput.getText();
        if (editable == null) {
            return;
        }

        int start = Math.max(currentInput.getSelectionStart(), 0);
        int end = Math.max(currentInput.getSelectionEnd(), 0);
        int min = Math.min(start, end);
        int max = Math.max(start, end);
        editable.replace(min, max, insertedText);
    }

    private void toggleShift() {
        shifted = !shifted;
        voiceGuide.performKeyFeedback();
        voiceGuide.speakMessage(
                shifted
                        ? R.string.keyboard_spoken_shift_on
                        : R.string.keyboard_spoken_shift_off
        );
        updateCharacterKeyLabels();
        updateShiftKeyTheme(LauncherThemePalette.fromPreferences(activity));
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

    private void updateShiftKeyTheme(LauncherThemePalette palette) {
        Easy2KeyboardStyler.styleShiftKey(shiftKeyView, palette, shifted);
    }

    private void updateVoiceKeyTheme(LauncherThemePalette palette) {
        if (voiceKeyView == null || palette == null) {
            return;
        }

        boolean voiceEnabled = voiceGuide.isEnabled();
        voiceKeyView.setText(activity.getString(
                voiceEnabled
                        ? R.string.keyboard_voice_on_label
                        : R.string.keyboard_voice_off_label
        ));
        Easy2KeyboardStyler.styleVoiceKey(voiceKeyView, palette, voiceEnabled);
    }

    private void toggleVoiceGuide() {
        voiceGuide.performKeyFeedback();
        boolean enabled = voiceGuide.toggleEnabled();
        if (enabled) {
            voiceGuide.performKeyFeedback();
        }
        updateVoiceKeyTheme(LauncherThemePalette.fromPreferences(activity));
    }

    private boolean isNumericField(EditText editText) {
        return (editText.getInputType() & InputType.TYPE_CLASS_NUMBER) == InputType.TYPE_CLASS_NUMBER;
    }

    private boolean isMultilineField(EditText editText) {
        return (editText.getInputType() & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0;
    }

    private boolean isAlphabeticValue(String value) {
        return value.length() == 1 && Character.isLetter(value.charAt(0));
    }

    private boolean isNumericValue(String value) {
        return value.length() == 1 && Character.isDigit(value.charAt(0));
    }

    private void updateVisibleInset() {
        visibleInsetBottom = keyboardContainer.getHeight();
        if (visibleInsetBottom <= 0) {
            visibleInsetBottom = dpToPx(290);
        }
        insetTarget.setPadding(
                insetTarget.getPaddingLeft(),
                insetTarget.getPaddingTop(),
                insetTarget.getPaddingRight(),
                visibleInsetBottom
        );
    }

    private void restoreInset() {
        insetTarget.setPadding(
                insetTarget.getPaddingLeft(),
                insetTarget.getPaddingTop(),
                insetTarget.getPaddingRight(),
                originalInsetBottom
        );
    }

    private void hideSystemKeyboard() {
        InputMethodManager inputMethodManager = activity.getSystemService(InputMethodManager.class);
        View focusedView = activity.getCurrentFocus();
        if (inputMethodManager != null && focusedView != null) {
            inputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        }
    }

    private boolean isVisible() {
        return keyboardContainer.getVisibility() == View.VISIBLE;
    }

    private int dpToPx(int dpValue) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                activity.getResources().getDisplayMetrics()
        ));
    }
}
