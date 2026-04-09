package com.rdr.easy2;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class NotesActivity extends AppCompatActivity {
    private VolumeOverlayController volumeOverlayController;
    private LauncherThemePalette palette;

    private View rootView;
    private FrameLayout topBarView;
    private TextView addButtonView;
    private TextView titleView;
    private TextView closeAppButtonView;
    private TextView emptyStateView;
    private TextView savedNoteView;
    private TextView deleteButtonView;
    private LinearLayout editorContainer;
    private EditText noteEditorView;
    private TextView saveButtonView;
    private TextView closeButtonView;
    private Easy2KeyboardController keyboardController;

    private boolean editorVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);
        volumeOverlayController = new VolumeOverlayController(this);

        bindViews();
        addButtonView.setOnClickListener(view -> openEditor());
        closeAppButtonView.setOnClickListener(view -> finish());
        savedNoteView.setOnClickListener(view -> openEditor());
        saveButtonView.setOnClickListener(view -> saveNote());
        closeButtonView.setOnClickListener(view -> closeEditor());
        deleteButtonView.setOnClickListener(view -> deleteNote());

        applyThemePalette();
        refreshNoteState();
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

    @Override
    public void onBackPressed() {
        if (keyboardController != null && keyboardController.handleBackPressed()) {
            return;
        }
        if (editorVisible) {
            closeEditor();
            return;
        }
        super.onBackPressed();
    }

    private void bindViews() {
        rootView = findViewById(R.id.notes_root);
        topBarView = findViewById(R.id.notes_top_bar);
        addButtonView = findViewById(R.id.notes_add_button);
        titleView = findViewById(R.id.notes_title_view);
        closeAppButtonView = findViewById(R.id.notes_app_close_button);
        emptyStateView = findViewById(R.id.notes_empty_state);
        savedNoteView = findViewById(R.id.notes_saved_note);
        deleteButtonView = findViewById(R.id.notes_delete_button);
        editorContainer = findViewById(R.id.notes_editor_container);
        noteEditorView = findViewById(R.id.notes_editor);
        saveButtonView = findViewById(R.id.notes_save_button);
        closeButtonView = findViewById(R.id.notes_close_button);
        ScrollView notesScrollView = findViewById(R.id.notes_scroll_view);
        LinearLayout keyboardContainer = findViewById(R.id.notes_keyboard_container);
        keyboardController = new Easy2KeyboardController(this, keyboardContainer, notesScrollView);
        keyboardController.attach(noteEditorView);
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
        palette = LauncherThemePalette.fromPreferences(this);

        rootView.setBackgroundColor(palette.getBackgroundColor());
        topBarView.setBackground(createRoundedBackground(
                palette.getSetupContactFillColor(),
                palette.getSetupContactStrokeColor(),
                28,
                2
        ));
        titleView.setTextColor(palette.getHeadingColor());
        emptyStateView.setTextColor(palette.getBodyTextColor());
        savedNoteView.setTextColor(0xFF4A3A14);
        noteEditorView.setTextColor(0xFF4A3A14);
        noteEditorView.setHintTextColor(0x99725621);

        addButtonView.setTextColor(Color.WHITE);
        addButtonView.setBackground(createCircleBackground(palette.getPrimaryColor()));
        closeAppButtonView.setTextColor(Color.WHITE);
        closeAppButtonView.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_close_button));
        saveButtonView.setTextColor(Color.WHITE);
        saveButtonView.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_save_button));
        closeButtonView.setTextColor(Color.WHITE);
        closeButtonView.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_close_button));
        deleteButtonView.setTextColor(Color.WHITE);
        deleteButtonView.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_close_button));
        if (keyboardController != null) {
            keyboardController.applyTheme(palette);
        }

        if (volumeOverlayController != null) {
            volumeOverlayController.applyTheme(palette);
        }
    }

    private void openEditor() {
        editorVisible = true;
        noteEditorView.setText(LauncherPreferences.getNoteText(this));
        noteEditorView.setSelection(noteEditorView.getText().length());
        noteEditorView.requestFocus();
        refreshNoteState();
    }

    private void closeEditor() {
        editorVisible = false;
        if (keyboardController != null) {
            keyboardController.clearFocusAndHide();
        }
        hideKeyboard();
        refreshNoteState();
    }

    private void saveNote() {
        Editable noteText = noteEditorView.getText();
        String text = noteText == null ? "" : noteText.toString();
        LauncherPreferences.saveNoteText(this, text);
        editorVisible = false;
        if (keyboardController != null) {
            keyboardController.clearFocusAndHide();
        }
        hideKeyboard();
        refreshNoteState();
    }

    private void deleteNote() {
        LauncherPreferences.saveNoteText(this, "");
        editorVisible = false;
        if (keyboardController != null) {
            keyboardController.clearFocusAndHide();
        }
        hideKeyboard();
        refreshNoteState();
    }

    private void refreshNoteState() {
        String savedNote = LauncherPreferences.getNoteText(this);
        boolean hasSavedNote = !TextUtils.isEmpty(savedNote.trim());

        editorContainer.setVisibility(editorVisible ? View.VISIBLE : View.GONE);
        savedNoteView.setVisibility(!editorVisible && hasSavedNote ? View.VISIBLE : View.GONE);
        deleteButtonView.setVisibility(!editorVisible && hasSavedNote ? View.VISIBLE : View.GONE);
        emptyStateView.setVisibility(!editorVisible && !hasSavedNote ? View.VISIBLE : View.GONE);
        closeAppButtonView.setVisibility(editorVisible ? View.GONE : View.VISIBLE);

        if (hasSavedNote) {
            savedNoteView.setText(savedNote);
        } else {
            savedNoteView.setText("");
        }
    }

    private void hideKeyboard() {
        View currentFocus = getCurrentFocus();
        if (currentFocus == null) {
            currentFocus = noteEditorView;
        }

        InputMethodManager inputMethodManager = getSystemService(InputMethodManager.class);
        if (inputMethodManager != null && currentFocus != null) {
            inputMethodManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
        noteEditorView.clearFocus();
    }

    private GradientDrawable createRoundedBackground(
            int fillColor,
            int strokeColor,
            int cornerRadiusDp,
            int strokeWidthDp
    ) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dpToPx(cornerRadiusDp));
        if (strokeWidthDp > 0) {
            drawable.setStroke(dpToPx(strokeWidthDp), strokeColor);
        }
        return drawable;
    }

    private GradientDrawable createCircleBackground(int fillColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(fillColor);
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
