package com.rdr.easy2;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.Locale;

public final class DeviceIdentity {
    private DeviceIdentity() {
    }

    public static String getDisplayName(Context context) {
        String configuredName = sanitize(readConfiguredDeviceName(context));
        String manufacturer = prettifyBrand(sanitize(Build.MANUFACTURER));
        String brand = prettifyBrand(sanitize(Build.BRAND));
        String model = sanitize(Build.MODEL);
        String hardwareLabel = buildHardwareLabel(manufacturer, brand, model);

        if (isProbablyEmulator()) {
            if (!TextUtils.isEmpty(configuredName) && !isGenericValue(configuredName)) {
                return "Emulador - " + configuredName;
            }
            if (!TextUtils.isEmpty(hardwareLabel) && !isGenericValue(hardwareLabel)) {
                return "Emulador - " + hardwareLabel;
            }
            return "Emulador Android";
        }

        if (!TextUtils.isEmpty(configuredName) && !isGenericValue(configuredName)) {
            if (!TextUtils.isEmpty(hardwareLabel)
                    && !containsIgnoreCase(configuredName, manufacturer)
                    && !containsIgnoreCase(configuredName, model)) {
                return configuredName + " (" + hardwareLabel + ")";
            }
            return configuredName;
        }

        if (!TextUtils.isEmpty(hardwareLabel) && !isGenericValue(hardwareLabel)) {
            return hardwareLabel;
        }

        return "Android";
    }

    private static String readConfiguredDeviceName(Context context) {
        if (context == null) {
            return "";
        }

        try {
            String value = Settings.Global.getString(context.getContentResolver(), "device_name");
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        } catch (Exception ignored) {
        }

        return "";
    }

    private static String buildHardwareLabel(String manufacturer, String brand, String model) {
        String selectedBrand = !TextUtils.isEmpty(manufacturer) ? manufacturer : brand;
        if (TextUtils.isEmpty(selectedBrand) || isGenericValue(selectedBrand)) {
            selectedBrand = "";
        }

        if (TextUtils.isEmpty(model)) {
            return selectedBrand;
        }

        if (!TextUtils.isEmpty(selectedBrand)
                && model.toLowerCase(Locale.ROOT).startsWith(selectedBrand.toLowerCase(Locale.ROOT))) {
            return model;
        }

        if (TextUtils.isEmpty(selectedBrand)) {
            return model;
        }

        return selectedBrand + " " + model;
    }

    private static boolean isProbablyEmulator() {
        return containsAny(Build.FINGERPRINT, "generic", "emulator", "virtual")
                || containsAny(Build.MODEL, "sdk_gphone", "emulator", "android sdk built for x86")
                || containsAny(Build.HARDWARE, "goldfish", "ranchu", "vbox86", "nox")
                || containsAny(Build.PRODUCT, "sdk", "emulator", "simulator", "genymotion")
                || containsAny(Build.MANUFACTURER, "genymotion")
                || containsAny(Build.BRAND, "generic")
                || containsAny(Build.DEVICE, "generic", "emulator");
    }

    private static boolean isGenericValue(String value) {
        return containsAny(value, "generic", "unknown", "sdk", "emulator", "simulator");
    }

    private static boolean containsAny(String value, String... parts) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }

        String normalized = value.toLowerCase(Locale.ROOT);
        for (String part : parts) {
            if (!TextUtils.isEmpty(part) && normalized.contains(part.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(String text, String fragment) {
        if (TextUtils.isEmpty(text) || TextUtils.isEmpty(fragment)) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).contains(fragment.toLowerCase(Locale.ROOT));
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String prettifyBrand(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }

        String[] parts = value.toLowerCase(Locale.ROOT).split("[\\s_-]+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (TextUtils.isEmpty(part)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }
}
