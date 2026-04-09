package com.rdr.easy2;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class SetupActivity extends AppCompatActivity {
    private static final int REQUEST_CONTACTS_PERMISSION = 3001;
    private static final int TOTAL_STEPS = 4;
    private static final int STEP_BASIC = 0;
    private static final int STEP_THEME = 1;
    private static final int STEP_MEDICAL = 2;
    private static final int STEP_CONTACTS = 3;

    private EditText firstNameInput;
    private EditText lastNameInput;
    private EditText ageInput;
    private EditText medicalInfoInput;
    private View setupRootView;
    private View setupCardView;
    private ScrollView setupScrollView;
    private TextView titleView;
    private TextView subtitleView;
    private TextView stepCounterView;
    private TextView cardTitleView;
    private TextView cardDetailView;
    private TextView optionalChipView;
    private TextView contactsHelperView;
    private TextView supportTitleView;
    private Button backButton;
    private Button saveButton;
    private Button organizeShortcutsButton;
    private Button phoneSettingsButton;
    private Button keyboardSettingsButton;
    private final View[] progressSegments = new View[TOTAL_STEPS];
    private final View[] stepSections = new View[TOTAL_STEPS];
    private final TextView[] contactSlots = new TextView[4];
    private final TextView[] colorOptions = new TextView[6];
    private final TextView[] colorLabels = new TextView[6];
    private final List<PinnedContact> selectedContacts = new ArrayList<>();

    private int pendingContactIndex = -1;
    private int currentStep = STEP_BASIC;
    private String selectedThemeKey = LauncherThemePalette.KEY_GREEN;
    private boolean setupAlreadyComplete;

    private ActivityResultLauncher<Intent> contactPickerLauncher;
    private Easy2KeyboardController keyboardController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        setupAlreadyComplete = LauncherPreferences.isSetupComplete(this);

        setupRootView = findViewById(R.id.setup_root);
        setupCardView = findViewById(R.id.setup_card_view);
        setupScrollView = findViewById(R.id.setup_scroll_view);
        titleView = findViewById(R.id.setup_title_view);
        subtitleView = findViewById(R.id.setup_subtitle_view);
        stepCounterView = findViewById(R.id.setup_step_view);
        cardTitleView = findViewById(R.id.setup_card_title_view);
        cardDetailView = findViewById(R.id.setup_card_detail_view);
        optionalChipView = findViewById(R.id.setup_optional_chip_view);
        contactsHelperView = findViewById(R.id.setup_contacts_helper_view);
        supportTitleView = findViewById(R.id.setup_support_title_view);
        firstNameInput = findViewById(R.id.input_first_name);
        lastNameInput = findViewById(R.id.input_last_name);
        ageInput = findViewById(R.id.input_age);
        medicalInfoInput = findViewById(R.id.input_medical_info);
        backButton = findViewById(R.id.setup_back_button);
        saveButton = findViewById(R.id.save_setup_button);
        organizeShortcutsButton = findViewById(R.id.organize_shortcuts_button);
        phoneSettingsButton = findViewById(R.id.open_phone_settings_button);
        keyboardSettingsButton = findViewById(R.id.open_keyboard_settings_button);

        progressSegments[0] = findViewById(R.id.setup_progress_0);
        progressSegments[1] = findViewById(R.id.setup_progress_1);
        progressSegments[2] = findViewById(R.id.setup_progress_2);
        progressSegments[3] = findViewById(R.id.setup_progress_3);

        stepSections[STEP_BASIC] = findViewById(R.id.setup_step_basic_section);
        stepSections[STEP_THEME] = findViewById(R.id.setup_step_theme_section);
        stepSections[STEP_MEDICAL] = findViewById(R.id.setup_step_medical_section);
        stepSections[STEP_CONTACTS] = findViewById(R.id.setup_step_contacts_section);

        contactSlots[0] = findViewById(R.id.contact_slot_0);
        contactSlots[1] = findViewById(R.id.contact_slot_1);
        contactSlots[2] = findViewById(R.id.contact_slot_2);
        contactSlots[3] = findViewById(R.id.contact_slot_3);

        colorOptions[0] = findViewById(R.id.color_option_green);
        colorOptions[1] = findViewById(R.id.color_option_blue);
        colorOptions[2] = findViewById(R.id.color_option_orange);
        colorOptions[3] = findViewById(R.id.color_option_coral);
        colorOptions[4] = findViewById(R.id.color_option_teal);
        colorOptions[5] = findViewById(R.id.color_option_black);

        colorLabels[0] = findViewById(R.id.color_label_green);
        colorLabels[1] = findViewById(R.id.color_label_blue);
        colorLabels[2] = findViewById(R.id.color_label_orange);
        colorLabels[3] = findViewById(R.id.color_label_coral);
        colorLabels[4] = findViewById(R.id.color_label_teal);
        colorLabels[5] = findViewById(R.id.color_label_black);

        LinearLayout keyboardContainer = findViewById(R.id.setup_keyboard_container);
        keyboardController = new Easy2KeyboardController(this, keyboardContainer, setupScrollView);
        keyboardController.attach(firstNameInput);
        keyboardController.attach(lastNameInput);
        keyboardController.attach(ageInput);
        keyboardController.attach(medicalInfoInput);

        for (int i = 0; i < contactSlots.length; i++) {
            selectedContacts.add(null);
        }

        contactPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handlePickedContact
        );

        titleView.setText(setupAlreadyComplete ? R.string.setup_edit_title : R.string.setup_title);
        subtitleView.setText(
                setupAlreadyComplete
                        ? R.string.setup_edit_subtitle
                        : R.string.setup_subtitle
        );

        bindContactSlots();
        bindColorOptions();
        preloadExistingValues();
        showStep(STEP_BASIC);

        saveButton.setOnClickListener(view -> handlePrimaryAction());
        backButton.setOnClickListener(view -> handleBackAction());
        if (organizeShortcutsButton != null) {
            organizeShortcutsButton.setOnClickListener(view -> {
                clearTextInputFocus();
                startActivity(new Intent(this, ShortcutOrganizerActivity.class));
            });
        }
        if (phoneSettingsButton != null) {
            phoneSettingsButton.setOnClickListener(view -> {
                clearTextInputFocus();
                openPhoneSettings();
            });
        }
        if (keyboardSettingsButton != null) {
            keyboardSettingsButton.setOnClickListener(view -> {
                clearTextInputFocus();
                openKeyboardSettings();
            });
        }
    }

    private void bindContactSlots() {
        for (int i = 0; i < contactSlots.length; i++) {
            final int index = i;
            contactSlots[i].setOnClickListener(view -> openContactSelector(index));
            updateContactSlotText(i);
        }
    }

    private void bindColorOptions() {
        bindColorOption(0, LauncherThemePalette.KEY_GREEN, R.string.theme_green);
        bindColorOption(1, LauncherThemePalette.KEY_BLUE, R.string.theme_blue);
        bindColorOption(2, LauncherThemePalette.KEY_ORANGE, R.string.theme_orange);
        bindColorOption(3, LauncherThemePalette.KEY_CORAL, R.string.theme_coral);
        bindColorOption(4, LauncherThemePalette.KEY_TEAL, R.string.theme_teal);
        bindColorOption(5, LauncherThemePalette.KEY_BLACK, R.string.theme_black);
    }

    private void bindColorOption(int index, String themeKey, int descriptionRes) {
        TextView colorOption = colorOptions[index];
        if (colorOption == null) {
            return;
        }

        colorOption.setContentDescription(getString(descriptionRes));
        colorOption.setOnClickListener(view -> {
            selectedThemeKey = themeKey;
            clearTextInputFocus();
            applySelectedTheme();
        });
    }

    private void preloadExistingValues() {
        firstNameInput.setText(LauncherPreferences.getFirstName(this));
        lastNameInput.setText(LauncherPreferences.getLastName(this));
        ageInput.setText(LauncherPreferences.getAge(this));
        medicalInfoInput.setText(LauncherPreferences.getMedicalInfo(this));
        selectedThemeKey = LauncherPreferences.getThemeColorKey(this);

        List<PinnedContact> pinnedContacts = LauncherPreferences.getPinnedContacts(this);
        for (int i = 0; i < pinnedContacts.size() && i < selectedContacts.size(); i++) {
            selectedContacts.set(i, pinnedContacts.get(i));
            updateContactSlotText(i);
        }
    }

    private void showStep(int step) {
        currentStep = Math.max(STEP_BASIC, Math.min(step, TOTAL_STEPS - 1));
        clearTextInputFocus();

        for (int i = 0; i < stepSections.length; i++) {
            if (stepSections[i] != null) {
                stepSections[i].setVisibility(i == currentStep ? View.VISIBLE : View.GONE);
            }
        }

        optionalChipView.setVisibility(currentStep == STEP_MEDICAL ? View.VISIBLE : View.GONE);
        cardTitleView.setText(getStepTitleRes(currentStep));
        cardDetailView.setText(getStepDetailRes(currentStep));
        stepCounterView.setText(
                getString(R.string.setup_step_counter, currentStep + 1, TOTAL_STEPS)
        );

        backButton.setVisibility(currentStep == STEP_BASIC ? View.INVISIBLE : View.VISIBLE);
        saveButton.setText(
                currentStep == STEP_CONTACTS
                        ? (setupAlreadyComplete
                            ? R.string.setup_save_changes
                            : R.string.setup_save)
                        : R.string.setup_next
        );

        applySelectedTheme();
        scrollToTop();
    }

    private int getStepTitleRes(int step) {
        switch (step) {
            case STEP_THEME:
                return R.string.setup_step_theme_title;
            case STEP_MEDICAL:
                return R.string.setup_step_medical_title;
            case STEP_CONTACTS:
                return R.string.setup_step_contacts_title;
            case STEP_BASIC:
            default:
                return R.string.setup_step_basic_title;
        }
    }

    private int getStepDetailRes(int step) {
        switch (step) {
            case STEP_THEME:
                return R.string.setup_step_theme_detail;
            case STEP_MEDICAL:
                return R.string.setup_step_medical_detail;
            case STEP_CONTACTS:
                return R.string.setup_step_contacts_detail;
            case STEP_BASIC:
            default:
                return R.string.setup_step_basic_detail;
        }
    }

    private void handlePrimaryAction() {
        switch (currentStep) {
            case STEP_BASIC:
                if (validateBasicInfo(true)) {
                    showStep(STEP_THEME);
                }
                break;
            case STEP_THEME:
                showStep(STEP_MEDICAL);
                break;
            case STEP_MEDICAL:
                showStep(STEP_CONTACTS);
                break;
            case STEP_CONTACTS:
            default:
                saveSetup();
                break;
        }
    }

    private void handleBackAction() {
        if (currentStep > STEP_BASIC) {
            showStep(currentStep - 1);
            return;
        }
        onBackPressed();
    }

    private boolean validateBasicInfo(boolean showErrors) {
        String firstName = firstNameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();
        String age = ageInput.getText().toString().trim();

        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName)) {
            if (showErrors) {
                Toast.makeText(this, R.string.setup_validation_name, Toast.LENGTH_SHORT).show();
                focusInput(TextUtils.isEmpty(firstName) ? firstNameInput : lastNameInput);
            }
            return false;
        }

        if (TextUtils.isEmpty(age)) {
            if (showErrors) {
                Toast.makeText(this, R.string.setup_validation_age, Toast.LENGTH_SHORT).show();
                focusInput(ageInput);
            }
            return false;
        }

        try {
            int numericAge = Integer.parseInt(age);
            if (numericAge <= 0 || numericAge > 120) {
                if (showErrors) {
                    Toast.makeText(this, R.string.setup_validation_age, Toast.LENGTH_SHORT).show();
                    focusInput(ageInput);
                }
                return false;
            }
        } catch (NumberFormatException exception) {
            if (showErrors) {
                Toast.makeText(this, R.string.setup_validation_age, Toast.LENGTH_SHORT).show();
                focusInput(ageInput);
            }
            return false;
        }

        return true;
    }

    private boolean validateContacts(boolean showErrors) {
        for (PinnedContact selectedContact : selectedContacts) {
            if (selectedContact == null) {
                if (showErrors) {
                    Toast.makeText(this, R.string.setup_validation_contacts, Toast.LENGTH_SHORT)
                            .show();
                }
                return false;
            }
        }
        return true;
    }

    private void applySelectedTheme() {
        LauncherThemePalette palette = LauncherThemePalette.fromKey(selectedThemeKey);

        if (setupRootView != null) {
            setupRootView.setBackgroundColor(palette.getBackgroundColor());
        }
        if (setupCardView != null) {
            setupCardView.setBackground(createRoundedDrawable(
                    palette.getSetupFieldFillColor(),
                    palette.getSetupFieldStrokeColor(),
                    28,
                    2
            ));
        }
        if (titleView != null) {
            titleView.setTextColor(palette.getHeadingColor());
        }
        if (subtitleView != null) {
            subtitleView.setTextColor(palette.getBodyTextColor());
        }
        if (stepCounterView != null) {
            stepCounterView.setTextColor(palette.getPrimaryColor());
        }
        if (cardTitleView != null) {
            cardTitleView.setTextColor(palette.getHeadingColor());
        }
        if (cardDetailView != null) {
            cardDetailView.setTextColor(palette.getBodyTextColor());
        }
        if (optionalChipView != null) {
            optionalChipView.setTextColor(0xFFFFFFFF);
            optionalChipView.setBackground(createRoundedDrawable(
                    palette.getPrimaryColor(),
                    palette.getPrimaryColor(),
                    999,
                    0
            ));
        }
        if (contactsHelperView != null) {
            contactsHelperView.setTextColor(palette.getBodyTextColor());
        }
        if (supportTitleView != null) {
            supportTitleView.setTextColor(palette.getHeadingColor());
        }
        if (saveButton != null) {
            saveButton.setBackgroundTintList(ColorStateList.valueOf(palette.getPrimaryColor()));
            saveButton.setTextColor(0xFFFFFFFF);
        }
        if (backButton != null) {
            int backFillColor = palette.isDarkMode()
                    ? palette.getCircleColor()
                    : palette.getSetupContactFillColor();
            backButton.setBackground(createRoundedDrawable(
                    backFillColor,
                    palette.getSetupContactStrokeColor(),
                    22,
                    2
            ));
            backButton.setTextColor(palette.getBodyTextColor());
        }

        applyActionButtonTheme(organizeShortcutsButton, palette);
        applyActionButtonTheme(phoneSettingsButton, palette);
        applyActionButtonTheme(keyboardSettingsButton, palette);

        applyTextFieldTheme(firstNameInput, palette);
        applyTextFieldTheme(lastNameInput, palette);
        applyTextFieldTheme(ageInput, palette);
        applyTextFieldTheme(medicalInfoInput, palette);
        if (keyboardController != null) {
            keyboardController.applyTheme(palette);
        }

        for (TextView contactSlot : contactSlots) {
            applyContactSlotTheme(contactSlot, palette);
        }

        updateProgressIndicator(palette);
        updateColorOptionSelection();
    }

    private void applyActionButtonTheme(Button button, LauncherThemePalette palette) {
        if (button == null) {
            return;
        }

        int actionColor = palette.isDarkMode()
                ? palette.getCircleColor()
                : palette.getChipColor();
        button.setBackgroundTintList(ColorStateList.valueOf(actionColor));
        button.setTextColor(0xFFFFFFFF);
    }

    private void updateProgressIndicator(LauncherThemePalette palette) {
        int inactiveColor = palette.isDarkMode() ? 0xFF303030 : 0x1A000000;
        for (int i = 0; i < progressSegments.length; i++) {
            View segment = progressSegments[i];
            if (segment == null) {
                continue;
            }
            int fillColor = i <= currentStep ? palette.getPrimaryColor() : inactiveColor;
            segment.setBackground(createRoundedDrawable(fillColor, fillColor, 999, 0));
        }
    }

    private void applyTextFieldTheme(EditText editText, LauncherThemePalette palette) {
        if (editText == null) {
            return;
        }
        editText.setBackground(createRoundedDrawable(
                palette.getSetupFieldFillColor(),
                palette.getSetupFieldStrokeColor(),
                18,
                2
        ));
        editText.setTextColor(palette.getInputTextColor());
        editText.setHintTextColor(palette.getInputHintColor());
    }

    private void applyContactSlotTheme(TextView textView, LauncherThemePalette palette) {
        if (textView == null) {
            return;
        }
        textView.setBackground(createRoundedDrawable(
                palette.getSetupContactFillColor(),
                palette.getSetupContactStrokeColor(),
                20,
                2
        ));
        textView.setTextColor(palette.getBodyTextColor());
    }

    private void updateColorOptionSelection() {
        List<LauncherThemePalette> themeOptions = LauncherThemePalette.getOptions();
        LauncherThemePalette selectedPalette = LauncherThemePalette.fromKey(selectedThemeKey);

        for (int i = 0; i < colorOptions.length; i++) {
            TextView colorOption = colorOptions[i];
            if (colorOption == null || i >= themeOptions.size()) {
                continue;
            }

            LauncherThemePalette palette = themeOptions.get(i);
            boolean selected = palette.getKey().equals(selectedThemeKey);
            colorOption.setBackground(createCircleDrawable(palette.getPrimaryColor(), selected));
            colorOption.setText(selected ? "\u2713" : "");
            colorOption.setAlpha(selected ? 1f : 0.85f);

            if (i < colorLabels.length && colorLabels[i] != null) {
                colorLabels[i].setTextColor(selectedPalette.getBodyTextColor());
                colorLabels[i].setAlpha(selected ? 1f : 0.74f);
                colorLabels[i].setTypeface(
                        colorLabels[i].getTypeface(),
                        selected ? Typeface.BOLD : Typeface.NORMAL
                );
            }
        }
    }

    private GradientDrawable createRoundedDrawable(
            int fillColor,
            int strokeColor,
            int radiusDp,
            int strokeWidthDp
    ) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dpToPx(radiusDp));
        drawable.setColor(fillColor);
        drawable.setStroke(Math.round(dpToPx(strokeWidthDp)), strokeColor);
        return drawable;
    }

    private GradientDrawable createCircleDrawable(int fillColor, boolean selected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(fillColor);

        int strokeColor;
        if (selected) {
            strokeColor = isColorDark(fillColor) ? 0xFFFFFFFF : 0xFF000000;
        } else {
            strokeColor = isColorDark(fillColor) ? 0x66FFFFFF : 0x22000000;
        }

        drawable.setStroke(
                Math.round(dpToPx(selected ? 3 : 1)),
                strokeColor
        );
        return drawable;
    }

    private boolean isColorDark(int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255d;
        return luminance < 0.5d;
    }

    private float dpToPx(int dpValue) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                getResources().getDisplayMetrics()
        );
    }

    private void scrollToTop() {
        if (setupScrollView != null) {
            setupScrollView.post(() -> setupScrollView.smoothScrollTo(0, 0));
        }
    }

    private void focusInput(EditText editText) {
        if (editText == null) {
            return;
        }
        editText.requestFocus();
        editText.setSelection(editText.getText().length());
    }

    private void openContactSelector(int index) {
        clearTextInputFocus();
        if (!hasContactsPermission()) {
            pendingContactIndex = index;
            requestPermissions(
                    new String[]{Manifest.permission.READ_CONTACTS},
                    REQUEST_CONTACTS_PERMISSION
            );
            return;
        }

        pendingContactIndex = index;
        Intent pickContactIntent = new Intent(
                Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI
        );
        contactPickerLauncher.launch(pickContactIntent);
    }

    private void handlePickedContact(ActivityResult result) {
        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
            pendingContactIndex = -1;
            return;
        }

        Uri contactUri = result.getData().getData();
        if (contactUri == null
                || pendingContactIndex < 0
                || pendingContactIndex >= selectedContacts.size()) {
            pendingContactIndex = -1;
            return;
        }

        PinnedContact pinnedContact = queryPinnedContact(contactUri);
        if (pinnedContact == null) {
            Toast.makeText(this, R.string.setup_contact_error, Toast.LENGTH_SHORT).show();
            pendingContactIndex = -1;
            return;
        }

        for (int i = 0; i < selectedContacts.size(); i++) {
            PinnedContact existingContact = selectedContacts.get(i);
            if (existingContact == null || i == pendingContactIndex) {
                continue;
            }
            if (existingContact.getLookupKey().equals(pinnedContact.getLookupKey())) {
                Toast.makeText(this, R.string.setup_contact_duplicate, Toast.LENGTH_SHORT).show();
                pendingContactIndex = -1;
                return;
            }
        }

        selectedContacts.set(pendingContactIndex, pinnedContact);
        updateContactSlotText(pendingContactIndex);
        pendingContactIndex = -1;
    }

    private PinnedContact queryPinnedContact(Uri contactUri) {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                contactUri,
                new String[]{
                        ContactsContract.Contacts.LOOKUP_KEY,
                        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
                },
                null,
                null,
                null
        );

        if (cursor == null) {
            return null;
        }

        try {
            if (!cursor.moveToFirst()) {
                return null;
            }

            int lookupIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY);
            int displayNameIndex =
                    cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY);

            String lookupKey = cursor.getString(lookupIndex);
            String displayName = cursor.getString(displayNameIndex);
            if (TextUtils.isEmpty(lookupKey) || TextUtils.isEmpty(displayName)) {
                return null;
            }

            return new PinnedContact(lookupKey, displayName);
        } finally {
            cursor.close();
        }
    }

    private void updateContactSlotText(int index) {
        if (index < 0 || index >= contactSlots.length) {
            return;
        }

        PinnedContact pinnedContact = selectedContacts.get(index);
        if (pinnedContact == null) {
            contactSlots[index].setText(getString(R.string.setup_contact_slot, index + 1));
        } else {
            contactSlots[index].setText(pinnedContact.getDisplayName());
        }
    }

    private void saveSetup() {
        clearTextInputFocus();
        if (!validateBasicInfo(false)) {
            showStep(STEP_BASIC);
            validateBasicInfo(true);
            return;
        }
        if (!validateContacts(false)) {
            showStep(STEP_CONTACTS);
            Toast.makeText(this, R.string.setup_validation_contacts, Toast.LENGTH_SHORT).show();
            return;
        }

        String firstName = firstNameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();
        String age = ageInput.getText().toString().trim();
        String medicalInfo = medicalInfoInput.getText().toString().trim();

        List<PinnedContact> pinnedContacts = new ArrayList<>();
        for (PinnedContact selectedContact : selectedContacts) {
            if (selectedContact != null) {
                pinnedContacts.add(selectedContact);
            }
        }

        LauncherPreferences.saveSetup(
                this,
                firstName,
                lastName,
                age,
                selectedThemeKey,
                medicalInfo,
                pinnedContacts
        );

        Intent launcherIntent = new Intent(this, MainActivity.class);
        launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(launcherIntent);
        finish();
    }

    private void openPhoneSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (Exception exception) {
            Toast.makeText(this, R.string.phone_settings_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void openKeyboardSettings() {
        try {
            startActivity(new Intent(this, KeyboardActivationActivity.class));
        } catch (Exception exception) {
            Toast.makeText(this, R.string.keyboard_settings_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void clearTextInputFocus() {
        if (keyboardController != null) {
            keyboardController.clearFocusAndHide();
        }
        if (firstNameInput != null) {
            firstNameInput.clearFocus();
        }
        if (lastNameInput != null) {
            lastNameInput.clearFocus();
        }
        if (ageInput != null) {
            ageInput.clearFocus();
        }
        if (medicalInfoInput != null) {
            medicalInfoInput.clearFocus();
        }
    }

    @Override
    public void onBackPressed() {
        if (currentStep > STEP_BASIC) {
            showStep(currentStep - 1);
            return;
        }
        clearTextInputFocus();
        super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CONTACTS_PERMISSION) {
            return;
        }

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (pendingContactIndex >= 0) {
                openContactSelector(pendingContactIndex);
            }
        } else {
            Toast.makeText(this, R.string.contacts_permission_needed, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasContactsPermission() {
        return checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }
}
