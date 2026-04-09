package com.rdr.easy2;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public final class LauncherPreferences {
    private static final String PREFS_NAME = "launcher_preferences";
    private static final String KEY_SETUP_COMPLETE = "setup_complete";
    private static final String KEY_FIRST_NAME = "profile_first_name";
    private static final String KEY_LAST_NAME = "profile_last_name";
    private static final String KEY_AGE = "profile_age";
    private static final String KEY_THEME_COLOR = "theme_color";
    private static final String KEY_MEDICAL_INFO = "medical_info";
    private static final String KEY_NOTE_TEXT = "note_text";
    private static final String KEY_PINNED_CONTACT_KEYS = "pinned_contact_keys";
    private static final String KEY_PINNED_CONTACT_NAMES = "pinned_contact_names";
    private static final String SEPARATOR = "\n";

    private LauncherPreferences() {
    }

    public static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isSetupComplete(Context context) {
        return getPreferences(context).getBoolean(KEY_SETUP_COMPLETE, false);
    }

    public static String getFirstName(Context context) {
        return getPreferences(context).getString(KEY_FIRST_NAME, "");
    }

    public static String getLastName(Context context) {
        return getPreferences(context).getString(KEY_LAST_NAME, "");
    }

    public static String getAge(Context context) {
        return getPreferences(context).getString(KEY_AGE, "");
    }

    public static String getThemeColorKey(Context context) {
        return getPreferences(context).getString(
                KEY_THEME_COLOR,
                LauncherThemePalette.KEY_GREEN
        );
    }

    public static String getMedicalInfo(Context context) {
        return getPreferences(context).getString(KEY_MEDICAL_INFO, "");
    }

    public static String getNoteText(Context context) {
        return getPreferences(context).getString(KEY_NOTE_TEXT, "");
    }

    public static void saveNoteText(Context context, String noteText) {
        getPreferences(context).edit()
                .putString(KEY_NOTE_TEXT, noteText)
                .apply();
    }

    public static String getDisplayName(Context context) {
        String firstName = getFirstName(context).trim();
        String lastName = getLastName(context).trim();
        if (TextUtils.isEmpty(firstName)) {
            return "";
        }
        if (TextUtils.isEmpty(lastName)) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    public static List<PinnedContact> getPinnedContacts(Context context) {
        SharedPreferences preferences = getPreferences(context);
        String keys = preferences.getString(KEY_PINNED_CONTACT_KEYS, "");
        String names = preferences.getString(KEY_PINNED_CONTACT_NAMES, "");

        List<PinnedContact> contacts = new ArrayList<>();
        if (TextUtils.isEmpty(keys)) {
            return contacts;
        }

        String[] keyParts = keys.split(SEPARATOR);
        String[] nameParts = names.split(SEPARATOR);

        for (int i = 0; i < keyParts.length; i++) {
            String lookupKey = keyParts[i].trim();
            if (TextUtils.isEmpty(lookupKey)) {
                continue;
            }

            String displayName = i < nameParts.length ? nameParts[i].trim() : "";
            contacts.add(new PinnedContact(lookupKey, displayName));
        }

        return contacts;
    }

    public static List<String> getPinnedContactLookupKeys(Context context) {
        List<String> lookupKeys = new ArrayList<>();
        for (PinnedContact contact : getPinnedContacts(context)) {
            lookupKeys.add(contact.getLookupKey());
        }
        return lookupKeys;
    }

    public static void saveSetup(
            Context context,
            String firstName,
            String lastName,
            String age,
            String themeColorKey,
            String medicalInfo,
            List<PinnedContact> pinnedContacts
    ) {
        List<String> lookupKeys = new ArrayList<>();
        List<String> displayNames = new ArrayList<>();

        for (PinnedContact pinnedContact : pinnedContacts) {
            lookupKeys.add(pinnedContact.getLookupKey());
            displayNames.add(pinnedContact.getDisplayName());
        }

        getPreferences(context).edit()
                .putBoolean(KEY_SETUP_COMPLETE, true)
                .putString(KEY_FIRST_NAME, firstName.trim())
                .putString(KEY_LAST_NAME, lastName.trim())
                .putString(KEY_AGE, age.trim())
                .putString(
                        KEY_THEME_COLOR,
                        LauncherThemePalette.fromKey(themeColorKey).getKey()
                )
                .putString(KEY_MEDICAL_INFO, medicalInfo.trim())
                .putString(KEY_PINNED_CONTACT_KEYS, TextUtils.join(SEPARATOR, lookupKeys))
                .putString(KEY_PINNED_CONTACT_NAMES, TextUtils.join(SEPARATOR, displayNames))
                .apply();
    }
}
