package com.rdr.easy2;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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

public class Easy2InputMethodService extends InputMethodService {
    private final List<TextView> characterKeys = new ArrayList<>();

    private LinearLayout keyboardRootView;
    private TextView shiftKeyView;
    private TextView deleteKeyView;
    private TextView hideKeyView;
    private TextView spaceKeyView;
    private TextView enterKeyView;
    private boolean shifted;

    @Override
    public View onCreateInputView() {
        keyboardRootView = new LinearLayout(this);
        keyboardRootView.setOrientation(LinearLayout.VERTICAL);
        keyboardRootView.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
        keyboardRootView.setElevation(dpToPx(10));

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
                view -> requestHideSelf(0)
        );
        addCharacterKey(fifthRow, ",", 0.9f);
        spaceKeyView = addActionKey(
                fifthRow,
                getString(R.string.keyboard_space),
                3.4f,
                view -> commitText(" ")
        );
        addCharacterKey(fifthRow, ".", 0.9f);
        enterKeyView = addActionKey(
                fifthRow,
                getString(R.string.keyboard_enter),
                1.45f,
                view -> handleEnter()
        );
        keyboardRootView.addView(fifthRow);

        applyThemePalette();
        updateCharacterKeyLabels();
        return keyboardRootView;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        applyThemePalette();
        updateCharacterKeyLabels();
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
        keyView.setOnClickListener(view -> {
            if (isNumericEditor() && !isNumericValue(value)) {
                return;
            }
            commitText(shifted && isAlphabeticValue(value) ? value.toUpperCase() : value);
        });
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
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return row;
    }

    private TextView createBaseKey(float weight) {
        TextView keyView = new TextView(this);
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

    private void applyThemePalette() {
        if (keyboardRootView == null) {
            return;
        }

        LauncherThemePalette palette = LauncherThemePalette.fromPreferences(this);
        keyboardRootView.setBackground(createRoundedBackground(
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

        styleActionKey(spaceKeyView, palette.getChipColor(), palette.getChipColor());
        styleActionKey(enterKeyView, palette.getPrimaryColor(), palette.getPrimaryColor());
        styleActionKey(hideKeyView, palette.getCircleColor(), palette.getCircleColor());
        styleActionKey(deleteKeyView, 0xFFD32F2F, 0xFFB71C1C);
        styleActionKey(
                shiftKeyView,
                shifted ? palette.getPrimaryColor() : palette.getChipColor(),
                shifted ? palette.getPrimaryColor() : palette.getChipColor()
        );
    }

    private void styleActionKey(TextView keyView, int fillColor, int strokeColor) {
        if (keyView == null) {
            return;
        }
        keyView.setTextColor(Color.WHITE);
        keyView.setBackground(createRoundedBackground(fillColor, strokeColor, 18, 2));
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
        if (sendDefaultEditorAction(true)) {
            return;
        }
        commitText("\n");
    }

    private void toggleShift() {
        shifted = !shifted;
        applyThemePalette();
        updateCharacterKeyLabels();
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

    private int dpToPx(int dpValue) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                getResources().getDisplayMetrics()
        ));
    }
}
