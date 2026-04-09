package com.rdr.easy2;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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

public final class Easy2KeyboardController {
    private final Activity activity;
    private final LinearLayout keyboardContainer;
    private final View insetTarget;
    private final int originalInsetBottom;
    private final int visibleInsetBottom;
    private final List<EditText> attachedInputs = new ArrayList<>();
    private final List<TextView> characterKeys = new ArrayList<>();

    private TextView shiftKeyView;
    private TextView hideKeyView;
    private TextView deleteKeyView;
    private TextView spaceKeyView;
    private TextView enterKeyView;
    private EditText currentInput;
    private boolean shifted;

    public Easy2KeyboardController(Activity activity, LinearLayout keyboardContainer, View insetTarget) {
        this.activity = activity;
        this.keyboardContainer = keyboardContainer;
        this.insetTarget = insetTarget;
        this.originalInsetBottom = insetTarget.getPaddingBottom();
        this.visibleInsetBottom = dpToPx(318);
        buildKeyboard();
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
        keyboardContainer.setBackground(createRoundedBackground(
                palette.getSetupContactFillColor(),
                palette.getSetupContactStrokeColor(),
                24,
                2
        ));

        for (TextView keyView : characterKeys) {
            keyView.setTextColor(palette.getInputTextColor());
            keyView.setBackground(createRoundedBackground(
                    palette.getSetupFieldFillColor(),
                    palette.getSetupFieldStrokeColor(),
                    18,
                    2
            ));
        }

        if (spaceKeyView != null) {
            styleActionKey(spaceKeyView, palette.getChipColor(), palette.getChipColor());
        }
        if (enterKeyView != null) {
            styleActionKey(enterKeyView, palette.getPrimaryColor(), palette.getPrimaryColor());
        }
        if (hideKeyView != null) {
            styleActionKey(hideKeyView, palette.getCircleColor(), palette.getCircleColor());
        }
        if (deleteKeyView != null) {
            styleActionKey(deleteKeyView, 0xFFD32F2F, 0xFFB71C1C);
        }
        updateShiftKeyTheme(palette);
        updateCharacterKeyLabels();
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
        keyboardContainer.setOrientation(LinearLayout.VERTICAL);
        keyboardContainer.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
        keyboardContainer.setElevation(dpToPx(10));

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
                view -> clearFocusAndHide()
        );
        addCharacterKey(fifthRow, ",", 0.9f);
        spaceKeyView = addActionKey(
                fifthRow,
                activity.getString(R.string.keyboard_space),
                3.4f,
                view -> insertCharacter(" ")
        );
        addCharacterKey(fifthRow, ".", 0.9f);
        enterKeyView = addActionKey(
                fifthRow,
                activity.getString(R.string.keyboard_enter),
                1.45f,
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
        keyView.setOnClickListener(view -> insertCharacter((String) view.getTag()));
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
        return row;
    }

    private TextView createBaseKey(float weight) {
        TextView keyView = new TextView(activity);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                dpToPx(54),
                weight
        );
        int margin = dpToPx(3);
        params.setMargins(margin, margin, margin, margin);
        keyView.setLayoutParams(params);
        keyView.setGravity(Gravity.CENTER);
        keyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        keyView.setTypeface(Typeface.DEFAULT_BOLD);
        keyView.setIncludeFontPadding(false);
        return keyView;
    }

    private void showFor(EditText editText) {
        currentInput = editText;
        hideSystemKeyboard();
        keyboardContainer.setVisibility(View.VISIBLE);
        applyInset();
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

    private void insertCharacter(String value) {
        if (currentInput == null || value == null || value.isEmpty()) {
            return;
        }

        if (isNumericField(currentInput) && !isNumericValue(value)) {
            return;
        }

        String textToInsert = shifted && isAlphabeticValue(value)
                ? value.toUpperCase()
                : value.toLowerCase();
        replaceSelection(textToInsert);
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
            return;
        }

        if (start > 0) {
            editable.delete(start - 1, start);
        }
    }

    private void handleEnter() {
        if (currentInput == null) {
            return;
        }

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
        updateCharacterKeyLabels();
        if (shiftKeyView != null && shiftKeyView.getBackground() instanceof GradientDrawable) {
            shiftKeyView.setSelected(shifted);
        }
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
        if (shiftKeyView == null) {
            return;
        }

        int fill = shifted ? palette.getPrimaryColor() : palette.getChipColor();
        int stroke = shifted ? palette.getPrimaryColor() : palette.getChipColor();
        styleActionKey(shiftKeyView, fill, stroke);
    }

    private void styleActionKey(TextView keyView, int fillColor, int strokeColor) {
        keyView.setTextColor(Color.WHITE);
        keyView.setBackground(createRoundedBackground(fillColor, strokeColor, 18, 2));
    }

    private GradientDrawable createRoundedBackground(
            int fillColor,
            int strokeColor,
            int radiusDp,
            int strokeWidthDp
    ) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dpToPx(radiusDp));
        if (strokeWidthDp > 0) {
            drawable.setStroke(dpToPx(strokeWidthDp), strokeColor);
        }
        return drawable;
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

    private void applyInset() {
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
