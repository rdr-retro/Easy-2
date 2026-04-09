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

public class Easy2InputMethodService extends InputMethodService {
    private final List<TextView> characterKeys = new ArrayList<>();

    private LinearLayout keyboardRootView;
    private View handleView;
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
        keyboardRootView.setClipToPadding(false);
        keyboardRootView.setPadding(0, 0, 0, 0);
        handleView = null;

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

    private int dpToPx(int dpValue) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                getResources().getDisplayMetrics()
        ));
    }
}
