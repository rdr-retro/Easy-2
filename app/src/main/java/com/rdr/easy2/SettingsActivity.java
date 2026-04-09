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

public class SettingsActivity extends AppCompatActivity {
    private static final int REQUEST_CONTACTS_PERMISSION = 3101;

    private EditText firstNameInput;
    private EditText lastNameInput;
    private EditText ageInput;
    private EditText medicalInfoInput;
    private EditText serverUrlInput;
    private View settingsRootView;
    private View profileCardView;
    private View themeCardView;
    private View medicalCardView;
    private View contactsCardView;
    private View accessibilityCardView;
    private View supportCardView;
    private View clientContactsContainer;
    private ScrollView settingsScrollView;
    private TextView titleView;
    private TextView subtitleView;
    private TextView categoryPersonalView;
    private TextView categoryHealthView;
    private TextView categoryAccessibilityView;
    private TextView categorySystemView;
    private TextView profileTitleView;
    private TextView themeTitleView;
    private TextView medicalTitleView;
    private TextView contactsTitleView;
    private TextView accessibilityTitleView;
    private TextView accessibilityDetailView;
    private TextView supportTitleView;
    private TextView contactsHelperView;
    private TextView roleTitleView;
    private TextView roleDetailView;
    private TextView modeClientButton;
    private TextView modeAdminButton;
    private TextView serverHelperView;
    private TextView adminHelperView;
    private Button closeButton;
    private Button saveButton;
    private Button organizeShortcutsButton;
    private Button phoneSettingsButton;
    private Button keyboardSettingsButton;
    private Button accessibilitySettingsButton;
    private Button displaySettingsButton;
    private Button hearingAssistButton;
    private final TextView[] contactSlots = new TextView[4];
    private final TextView[] colorOptions = new TextView[6];
    private final TextView[] colorLabels = new TextView[6];
    private final List<PinnedContact> selectedContacts = new ArrayList<>();

    private int pendingContactIndex = -1;
    private String selectedThemeKey = LauncherThemePalette.KEY_GREEN;
    private String selectedUserMode = LauncherPreferences.USER_MODE_CLIENT;

    private ActivityResultLauncher<Intent> contactPickerLauncher;
    private Easy2KeyboardController keyboardController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!LauncherPreferences.isSetupComplete(this)) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_settings);

        settingsRootView = findViewById(R.id.settings_root);
        settingsScrollView = findViewById(R.id.settings_scroll_view);
        profileCardView = findViewById(R.id.settings_profile_card);
        themeCardView = findViewById(R.id.settings_theme_card);
        medicalCardView = findViewById(R.id.settings_medical_card);
        contactsCardView = findViewById(R.id.settings_contacts_card);
        accessibilityCardView = findViewById(R.id.settings_accessibility_card);
        supportCardView = findViewById(R.id.settings_support_card);
        titleView = findViewById(R.id.settings_title_view);
        subtitleView = findViewById(R.id.settings_subtitle_view);
        categoryPersonalView = findViewById(R.id.settings_category_personal_view);
        categoryHealthView = findViewById(R.id.settings_category_health_view);
        categoryAccessibilityView = findViewById(R.id.settings_category_accessibility_view);
        categorySystemView = findViewById(R.id.settings_category_system_view);
        profileTitleView = findViewById(R.id.settings_profile_title_view);
        themeTitleView = findViewById(R.id.settings_theme_title_view);
        medicalTitleView = findViewById(R.id.settings_medical_title_view);
        contactsTitleView = findViewById(R.id.settings_contacts_title_view);
        accessibilityTitleView = findViewById(R.id.settings_accessibility_title_view);
        accessibilityDetailView = findViewById(R.id.settings_accessibility_detail_view);
        supportTitleView = findViewById(R.id.settings_support_title_view);
        contactsHelperView = findViewById(R.id.setup_contacts_helper_view);
        roleTitleView = findViewById(R.id.setup_role_title_view);
        roleDetailView = findViewById(R.id.setup_role_detail_view);
        modeClientButton = findViewById(R.id.setup_mode_client_button);
        modeAdminButton = findViewById(R.id.setup_mode_admin_button);
        serverHelperView = findViewById(R.id.setup_server_helper_view);
        adminHelperView = findViewById(R.id.setup_admin_helper_view);
        firstNameInput = findViewById(R.id.input_first_name);
        lastNameInput = findViewById(R.id.input_last_name);
        ageInput = findViewById(R.id.input_age);
        medicalInfoInput = findViewById(R.id.input_medical_info);
        serverUrlInput = findViewById(R.id.input_server_url);
        closeButton = findViewById(R.id.settings_close_button);
        saveButton = findViewById(R.id.settings_save_button);
        organizeShortcutsButton = findViewById(R.id.organize_shortcuts_button);
        phoneSettingsButton = findViewById(R.id.open_phone_settings_button);
        keyboardSettingsButton = findViewById(R.id.open_keyboard_settings_button);
        accessibilitySettingsButton = findViewById(R.id.open_accessibility_settings_button);
        displaySettingsButton = findViewById(R.id.open_display_settings_button);
        hearingAssistButton = findViewById(R.id.open_hearing_assist_button);
        clientContactsContainer = findViewById(R.id.setup_client_contacts_container);

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

        LinearLayout keyboardContainer = findViewById(R.id.settings_keyboard_container);
        keyboardController = new Easy2KeyboardController(this, keyboardContainer, settingsScrollView);
        keyboardController.attach(firstNameInput);
        keyboardController.attach(lastNameInput);
        keyboardController.attach(ageInput);
        keyboardController.attach(medicalInfoInput);
        keyboardController.attach(serverUrlInput);

        for (int i = 0; i < contactSlots.length; i++) {
            selectedContacts.add(null);
        }

        contactPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handlePickedContact
        );

        bindContactSlots();
        bindColorOptions();
        bindModeButtons();
        preloadExistingValues();
        applySelectedTheme();

        if (closeButton != null) {
            closeButton.setOnClickListener(view -> {
                clearTextInputFocus();
                finish();
            });
        }
        if (saveButton != null) {
            saveButton.setOnClickListener(view -> saveSettings());
        }
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
        if (accessibilitySettingsButton != null) {
            accessibilitySettingsButton.setOnClickListener(view -> {
                clearTextInputFocus();
                openAccessibilitySettings();
            });
        }
        if (displaySettingsButton != null) {
            displaySettingsButton.setOnClickListener(view -> {
                clearTextInputFocus();
                openDisplaySettings();
            });
        }
        if (hearingAssistButton != null) {
            hearingAssistButton.setOnClickListener(view -> {
                clearTextInputFocus();
                startActivity(new Intent(this, HearingAssistActivity.class));
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

    private void bindModeButtons() {
        if (modeClientButton != null) {
            modeClientButton.setOnClickListener(view -> {
                selectedUserMode = LauncherPreferences.USER_MODE_CLIENT;
                clearTextInputFocus();
                refreshRoleUi();
                applySelectedTheme();
            });
        }

        if (modeAdminButton != null) {
            modeAdminButton.setOnClickListener(view -> {
                selectedUserMode = LauncherPreferences.USER_MODE_ADMIN;
                clearTextInputFocus();
                refreshRoleUi();
                applySelectedTheme();
            });
        }
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
        serverUrlInput.setText(LauncherPreferences.getRemoteServerUrl(this));
        selectedThemeKey = LauncherPreferences.getThemeColorKey(this);
        selectedUserMode = LauncherPreferences.getUserMode(this);

        List<PinnedContact> pinnedContacts = LauncherPreferences.getPinnedContacts(this);
        for (int i = 0; i < pinnedContacts.size() && i < selectedContacts.size(); i++) {
            selectedContacts.set(i, pinnedContacts.get(i));
            updateContactSlotText(i);
        }

        refreshRoleUi();
    }

    private void refreshRoleUi() {
        boolean adminMode = LauncherPreferences.USER_MODE_ADMIN.equals(selectedUserMode);

        if (clientContactsContainer != null) {
            clientContactsContainer.setVisibility(adminMode ? View.GONE : View.VISIBLE);
        }
        if (adminHelperView != null) {
            adminHelperView.setVisibility(adminMode ? View.VISIBLE : View.GONE);
        }
    }

    private boolean validateBasicInfo(boolean showErrors) {
        boolean adminMode = LauncherPreferences.USER_MODE_ADMIN.equals(selectedUserMode);
        String firstName = firstNameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();
        String age = ageInput.getText().toString().trim();
        String serverUrl = LauncherPreferences.sanitizeServerUrl(
                serverUrlInput.getText().toString()
        );

        if (!adminMode && (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName))) {
            if (showErrors) {
                Toast.makeText(this, R.string.setup_validation_name, Toast.LENGTH_SHORT).show();
                focusInput(TextUtils.isEmpty(firstName) ? firstNameInput : lastNameInput);
            }
            return false;
        }

        if (adminMode && TextUtils.isEmpty(serverUrl)) {
            if (showErrors) {
                Toast.makeText(this, R.string.setup_validation_server_admin, Toast.LENGTH_SHORT)
                        .show();
                focusInput(serverUrlInput);
            }
            return false;
        }

        if (adminMode) {
            return true;
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
        if (LauncherPreferences.USER_MODE_ADMIN.equals(selectedUserMode)) {
            return true;
        }

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

        if (settingsRootView != null) {
            settingsRootView.setBackgroundColor(palette.getBackgroundColor());
        }
        applySectionCardTheme(profileCardView, palette);
        applySectionCardTheme(themeCardView, palette);
        applySectionCardTheme(medicalCardView, palette);
        applySectionCardTheme(contactsCardView, palette);
        applySectionCardTheme(accessibilityCardView, palette);
        applySectionCardTheme(supportCardView, palette);

        if (titleView != null) {
            titleView.setTextColor(palette.getHeadingColor());
        }
        if (subtitleView != null) {
            subtitleView.setTextColor(palette.getBodyTextColor());
        }
        if (categoryPersonalView != null) {
            categoryPersonalView.setTextColor(palette.getPrimaryColor());
        }
        if (categoryHealthView != null) {
            categoryHealthView.setTextColor(palette.getPrimaryColor());
        }
        if (categoryAccessibilityView != null) {
            categoryAccessibilityView.setTextColor(palette.getPrimaryColor());
        }
        if (categorySystemView != null) {
            categorySystemView.setTextColor(palette.getPrimaryColor());
        }
        if (profileTitleView != null) {
            profileTitleView.setTextColor(palette.getHeadingColor());
        }
        if (themeTitleView != null) {
            themeTitleView.setTextColor(palette.getHeadingColor());
        }
        if (medicalTitleView != null) {
            medicalTitleView.setTextColor(palette.getHeadingColor());
        }
        if (contactsTitleView != null) {
            contactsTitleView.setTextColor(palette.getHeadingColor());
        }
        if (accessibilityTitleView != null) {
            accessibilityTitleView.setTextColor(palette.getHeadingColor());
        }
        if (accessibilityDetailView != null) {
            accessibilityDetailView.setTextColor(palette.getBodyTextColor());
        }
        if (supportTitleView != null) {
            supportTitleView.setTextColor(palette.getHeadingColor());
        }
        if (contactsHelperView != null) {
            contactsHelperView.setTextColor(palette.getBodyTextColor());
        }
        if (roleTitleView != null) {
            roleTitleView.setTextColor(palette.getHeadingColor());
        }
        if (roleDetailView != null) {
            roleDetailView.setTextColor(palette.getBodyTextColor());
        }
        if (serverHelperView != null) {
            serverHelperView.setTextColor(palette.getBodyTextColor());
        }
        if (adminHelperView != null) {
            adminHelperView.setTextColor(palette.getBodyTextColor());
        }
        if (saveButton != null) {
            saveButton.setBackgroundTintList(ColorStateList.valueOf(palette.getPrimaryColor()));
            saveButton.setTextColor(0xFFFFFFFF);
        }
        if (closeButton != null) {
            int closeFillColor = palette.isDarkMode()
                    ? palette.getCircleColor()
                    : palette.getSetupContactFillColor();
            closeButton.setBackground(createRoundedDrawable(
                    closeFillColor,
                    palette.getSetupContactStrokeColor(),
                    22,
                    2
            ));
            closeButton.setTextColor(palette.getBodyTextColor());
        }

        applyActionButtonTheme(organizeShortcutsButton, palette);
        applyActionButtonTheme(phoneSettingsButton, palette);
        applyActionButtonTheme(keyboardSettingsButton, palette);
        applyActionButtonTheme(accessibilitySettingsButton, palette);
        applyActionButtonTheme(displaySettingsButton, palette);
        applyActionButtonTheme(hearingAssistButton, palette);

        applyTextFieldTheme(firstNameInput, palette);
        applyTextFieldTheme(lastNameInput, palette);
        applyTextFieldTheme(ageInput, palette);
        applyTextFieldTheme(medicalInfoInput, palette);
        applyTextFieldTheme(serverUrlInput, palette);
        if (keyboardController != null) {
            keyboardController.applyTheme(palette);
        }

        applyModeButtonTheme(
                modeClientButton,
                palette,
                !LauncherPreferences.USER_MODE_ADMIN.equals(selectedUserMode)
        );
        applyModeButtonTheme(
                modeAdminButton,
                palette,
                LauncherPreferences.USER_MODE_ADMIN.equals(selectedUserMode)
        );

        for (TextView contactSlot : contactSlots) {
            applyContactSlotTheme(contactSlot, palette);
        }

        updateColorOptionSelection();
    }

    private void applySectionCardTheme(View cardView, LauncherThemePalette palette) {
        if (cardView == null) {
            return;
        }
        cardView.setBackground(createRoundedDrawable(
                palette.getSetupFieldFillColor(),
                palette.getSetupFieldStrokeColor(),
                28,
                2
        ));
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

    private void applyModeButtonTheme(
            TextView button,
            LauncherThemePalette palette,
            boolean selected
    ) {
        if (button == null) {
            return;
        }

        int fillColor = selected
                ? palette.getPrimaryColor()
                : palette.getSetupContactFillColor();
        int strokeColor = selected
                ? palette.getPrimaryColor()
                : palette.getSetupContactStrokeColor();
        button.setBackground(createRoundedDrawable(fillColor, strokeColor, 18, selected ? 0 : 2));
        button.setTextColor(selected ? 0xFFFFFFFF : palette.getBodyTextColor());
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

    private void saveSettings() {
        clearTextInputFocus();
        if (!validateBasicInfo(true)) {
            return;
        }
        if (!validateContacts(true)) {
            return;
        }

        String firstName = firstNameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();
        String age = ageInput.getText().toString().trim();
        String medicalInfo = medicalInfoInput.getText().toString().trim();
        String serverUrl = LauncherPreferences.sanitizeServerUrl(
                serverUrlInput.getText().toString()
        );

        String previousServerUrl = LauncherPreferences.getRemoteServerUrl(this);
        String previousUserMode = LauncherPreferences.getUserMode(this);
        String previousClientId = LauncherPreferences.getRemoteClientId(this);

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
                selectedUserMode,
                serverUrl,
                pinnedContacts
        );

        boolean shouldRegisterClient =
                LauncherPreferences.USER_MODE_CLIENT.equals(selectedUserMode)
                        && !TextUtils.isEmpty(serverUrl)
                        && (TextUtils.isEmpty(previousClientId)
                            || !serverUrl.equals(previousServerUrl)
                            || !LauncherPreferences.USER_MODE_CLIENT.equals(previousUserMode));

        if (shouldRegisterClient) {
            Toast.makeText(this, R.string.setup_registration_started, Toast.LENGTH_SHORT).show();
            registerClientInBackground(serverUrl, firstName, lastName, age);
        }

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

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception exception) {
            Toast.makeText(this, R.string.settings_accessibility_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void openDisplaySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
        } catch (Exception exception) {
            Toast.makeText(this, R.string.settings_accessibility_error, Toast.LENGTH_SHORT).show();
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
        if (serverUrlInput != null) {
            serverUrlInput.clearFocus();
        }
    }

    @Override
    public void onBackPressed() {
        if (keyboardController != null && keyboardController.handleBackPressed()) {
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

    private void registerClientInBackground(
            String serverUrl,
            String firstName,
            String lastName,
            String age
    ) {
        new Thread(() -> {
            try {
                String authToken = LauncherPreferences.ensureRemoteAuthToken(this);
                StringBuilder displayNameBuilder = new StringBuilder();
                if (!TextUtils.isEmpty(firstName)) {
                    displayNameBuilder.append(firstName.trim());
                }
                if (!TextUtils.isEmpty(lastName)) {
                    if (displayNameBuilder.length() > 0) {
                        displayNameBuilder.append(' ');
                    }
                    displayNameBuilder.append(lastName.trim());
                }

                String displayName = displayNameBuilder.length() > 0
                        ? displayNameBuilder.toString()
                        : getString(
                                R.string.remote_default_client_name,
                                authToken.substring(0, Math.min(6, authToken.length()))
                        );

                RemoteServerClient.RegistrationResult result = RemoteServerClient.registerClient(
                        serverUrl,
                        authToken,
                        DeviceIdentity.getDisplayName(this),
                        displayName,
                        age
                );
                if (!TextUtils.isEmpty(result.clientId)) {
                    LauncherPreferences.saveRemoteRegistration(this, result.clientId);
                }
                runOnUiThread(() -> Toast.makeText(
                        this,
                        R.string.setup_registration_success,
                        Toast.LENGTH_SHORT
                ).show());
            } catch (Exception exception) {
                runOnUiThread(() -> Toast.makeText(
                        this,
                        R.string.setup_registration_failed,
                        Toast.LENGTH_SHORT
                ).show());
            }
        }).start();
    }
}
