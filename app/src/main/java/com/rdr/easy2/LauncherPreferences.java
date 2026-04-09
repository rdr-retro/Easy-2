package com.rdr.easy2;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LauncherPreferences {
    public static final String USER_MODE_CLIENT = "client";
    public static final String USER_MODE_ADMIN = "admin";

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
    private static final String KEY_USER_MODE = "user_mode";
    private static final String KEY_REMOTE_SERVER_URL = "remote_server_url";
    private static final String KEY_REMOTE_AUTH_TOKEN = "remote_auth_token";
    private static final String KEY_REMOTE_CLIENT_ID = "remote_client_id";
    private static final String KEY_REMOTE_LAST_SYNC_AT = "remote_last_sync_at";
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

    public static String getUserMode(Context context) {
        return getPreferences(context).getString(KEY_USER_MODE, USER_MODE_CLIENT);
    }

    public static boolean isAdminMode(Context context) {
        return USER_MODE_ADMIN.equals(getUserMode(context));
    }

    public static boolean isClientMode(Context context) {
        return !isAdminMode(context);
    }

    public static String getRemoteServerUrl(Context context) {
        return sanitizeServerUrl(
                getPreferences(context).getString(KEY_REMOTE_SERVER_URL, "")
        );
    }

    public static boolean hasRemoteServerUrl(Context context) {
        return !TextUtils.isEmpty(getRemoteServerUrl(context));
    }

    public static String getRemoteClientId(Context context) {
        return getPreferences(context).getString(KEY_REMOTE_CLIENT_ID, "");
    }

    public static long getRemoteLastSyncAt(Context context) {
        return getPreferences(context).getLong(KEY_REMOTE_LAST_SYNC_AT, 0L);
    }

    public static String ensureRemoteAuthToken(Context context) {
        SharedPreferences preferences = getPreferences(context);
        String existingToken = preferences.getString(KEY_REMOTE_AUTH_TOKEN, "");
        if (!TextUtils.isEmpty(existingToken)) {
            return existingToken;
        }

        String newToken = UUID.randomUUID().toString();
        preferences.edit()
                .putString(KEY_REMOTE_AUTH_TOKEN, newToken)
                .apply();
        return newToken;
    }

    public static void saveRemoteRegistration(Context context, String clientId) {
        getPreferences(context).edit()
                .putString(KEY_REMOTE_CLIENT_ID, clientId)
                .putLong(KEY_REMOTE_LAST_SYNC_AT, System.currentTimeMillis())
                .apply();
    }

    public static void markRemoteSync(Context context) {
        getPreferences(context).edit()
                .putLong(KEY_REMOTE_LAST_SYNC_AT, System.currentTimeMillis())
                .apply();
    }

    public static void clearRemoteRegistration(Context context) {
        getPreferences(context).edit()
                .remove(KEY_REMOTE_CLIENT_ID)
                .remove(KEY_REMOTE_LAST_SYNC_AT)
                .apply();
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
            String userMode,
            String remoteServerUrl,
            List<PinnedContact> pinnedContacts
    ) {
        List<String> lookupKeys = new ArrayList<>();
        List<String> displayNames = new ArrayList<>();
        String sanitizedUserMode = sanitizeUserMode(userMode);
        String normalizedServerUrl = sanitizeServerUrl(remoteServerUrl);
        String previousServerUrl = getRemoteServerUrl(context);
        String previousUserMode = getUserMode(context);

        for (PinnedContact pinnedContact : pinnedContacts) {
            lookupKeys.add(pinnedContact.getLookupKey());
            displayNames.add(pinnedContact.getDisplayName());
        }

        SharedPreferences.Editor editor = getPreferences(context).edit()
                .putBoolean(KEY_SETUP_COMPLETE, true)
                .putString(KEY_FIRST_NAME, firstName.trim())
                .putString(KEY_LAST_NAME, lastName.trim())
                .putString(KEY_AGE, age.trim())
                .putString(
                        KEY_THEME_COLOR,
                        LauncherThemePalette.fromKey(themeColorKey).getKey()
                )
                .putString(KEY_MEDICAL_INFO, medicalInfo.trim())
                .putString(KEY_USER_MODE, sanitizedUserMode)
                .putString(KEY_REMOTE_SERVER_URL, normalizedServerUrl)
                .putString(KEY_PINNED_CONTACT_KEYS, TextUtils.join(SEPARATOR, lookupKeys))
                .putString(KEY_PINNED_CONTACT_NAMES, TextUtils.join(SEPARATOR, displayNames));

        if (TextUtils.isEmpty(normalizedServerUrl)
                || USER_MODE_ADMIN.equals(sanitizedUserMode)
                || !normalizedServerUrl.equals(previousServerUrl)
                || !sanitizedUserMode.equals(previousUserMode)) {
            editor.remove(KEY_REMOTE_CLIENT_ID);
            editor.remove(KEY_REMOTE_LAST_SYNC_AT);
        }

        editor.apply();
    }

    public static String sanitizeServerUrl(String rawUrl) {
        if (TextUtils.isEmpty(rawUrl)) {
            return "";
        }

        String normalizedUrl = rawUrl.trim();
        if (TextUtils.isEmpty(normalizedUrl)) {
            return "";
        }

        if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
            normalizedUrl = "http://" + normalizedUrl;
        }

        while (normalizedUrl.endsWith("/")) {
            normalizedUrl = normalizedUrl.substring(0, normalizedUrl.length() - 1);
        }

        return normalizedUrl;
    }

    private static String sanitizeUserMode(String userMode) {
        return USER_MODE_ADMIN.equals(userMode) ? USER_MODE_ADMIN : USER_MODE_CLIENT;
    }
}
