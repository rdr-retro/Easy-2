package com.rdr.easy2;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public final class ShortcutStorage {
    private static final String KEY_SHORTCUTS = "shortcut_components";
    private static final String SHORTCUT_SEPARATOR = "\n";

    private ShortcutStorage() {
    }

    public static List<String> load(Context context) {
        SharedPreferences preferences = LauncherPreferences.getPreferences(context);
        String storedValue = preferences.getString(KEY_SHORTCUTS, "");
        List<String> components = new ArrayList<>();
        if (TextUtils.isEmpty(storedValue)) {
            return components;
        }

        String[] splitComponents = storedValue.split(SHORTCUT_SEPARATOR);
        for (String component : splitComponents) {
            if (!TextUtils.isEmpty(component)) {
                components.add(component);
            }
        }
        return components;
    }

    public static void save(Context context, List<String> componentKeys) {
        LauncherPreferences.getPreferences(context)
                .edit()
                .putString(KEY_SHORTCUTS, TextUtils.join(SHORTCUT_SEPARATOR, componentKeys))
                .apply();
    }
}
