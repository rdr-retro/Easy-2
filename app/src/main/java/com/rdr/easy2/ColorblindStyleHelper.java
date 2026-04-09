package com.rdr.easy2;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.widget.ImageView;

public final class ColorblindStyleHelper {
    private static final int DEFAULT_PRIMARY = 0xFF43A047;
    private static final int DEFAULT_PHONE_APP_TEXT_DARK = 0xFF111111;
    private static final float[][] COLORBLIND_HUE_WINDOWS = new float[][]{
            {12f, 38f},
            {42f, 74f},
            {182f, 212f},
            {216f, 248f},
            {266f, 326f}
    };

    private ColorblindStyleHelper() {
    }

    public static boolean isColorblindMode(LauncherThemePalette palette) {
        return palette != null && LauncherThemePalette.KEY_COLORBLIND.equals(palette.getKey());
    }

    public static int resolveAppAccentColor(String stableKey, LauncherThemePalette palette) {
        if (isColorblindMode(palette)) {
            return createUniqueColorblindAccentColor(stableKey, palette);
        }
        return createUniquePhoneAppAccentColor(stableKey, palette);
    }

    public static int resolveAppSurfaceColor(String stableKey, LauncherThemePalette palette) {
        if (isColorblindMode(palette)) {
            return blendColors(resolveAppAccentColor(stableKey, palette), Color.WHITE, 0.78f);
        }
        return blendColors(
                resolveAppAccentColor(stableKey, palette),
                resolvePhoneAppBlendBaseColor(palette),
                palette != null && palette.isDarkMode() ? 0.58f : 0.78f
        );
    }

    public static int resolveAppLabelColor(String stableKey, LauncherThemePalette palette) {
        if (isColorblindMode(palette)) {
            return resolveAppAccentColor(stableKey, palette);
        }
        return blendColors(
                resolveAppAccentColor(stableKey, palette),
                palette != null && palette.isDarkMode() ? Color.BLACK : Color.WHITE,
                palette != null && palette.isDarkMode() ? 0.16f : 0.08f
        );
    }

    public static int resolveAppCardColor(String stableKey, LauncherThemePalette palette) {
        if (isColorblindMode(palette)) {
            return blendColors(resolveAppAccentColor(stableKey, palette), Color.WHITE, 0.92f);
        }
        return blendColors(
                resolveAppAccentColor(stableKey, palette),
                resolvePhoneAppBlendBaseColor(palette),
                palette != null && palette.isDarkMode() ? 0.82f : 0.9f
        );
    }

    public static int resolveAppStrokeColor(String stableKey, LauncherThemePalette palette) {
        if (isColorblindMode(palette)) {
            return resolveAppAccentColor(stableKey, palette);
        }
        return blendColors(
                resolveAppAccentColor(stableKey, palette),
                resolvePhoneAppBlendBaseColor(palette),
                palette != null && palette.isDarkMode() ? 0.28f : 0.18f
        );
    }

    public static int resolveTextColorOnAccent(String stableKey, LauncherThemePalette palette) {
        return isDarkColor(resolveAppLabelColor(stableKey, palette))
                ? Color.WHITE
                : DEFAULT_PHONE_APP_TEXT_DARK;
    }

    public static void applyIconColor(ImageView imageView, String stableKey, LauncherThemePalette palette) {
        if (imageView == null) {
            return;
        }
        if (isColorblindMode(palette)) {
            imageView.setColorFilter(resolveAppAccentColor(stableKey, palette));
        } else {
            imageView.clearColorFilter();
        }
    }

    public static int resolveSemanticAccentColor(
            String stableKey,
            int fallbackColor,
            LauncherThemePalette palette
    ) {
        if (!isColorblindMode(palette)) {
            return fallbackColor;
        }
        return resolveAppAccentColor(stableKey, palette);
    }

    public static int resolveTextColorForBackground(int backgroundColor) {
        return isDarkColor(backgroundColor) ? Color.WHITE : DEFAULT_PHONE_APP_TEXT_DARK;
    }

    public static int resolvePhoneAppAccentColor(String stableKey, LauncherThemePalette palette) {
        if (isColorblindMode(palette)) {
            return resolveAppAccentColor(stableKey, palette);
        }
        return createUniquePhoneAppAccentColor(stableKey, palette);
    }

    public static int resolvePhoneAppSurfaceColor(String stableKey, LauncherThemePalette palette) {
        if (isColorblindMode(palette)) {
            return resolveAppSurfaceColor(stableKey, palette);
        }
        return blendColors(
                resolvePhoneAppAccentColor(stableKey, palette),
                resolvePhoneAppBlendBaseColor(palette),
                palette != null && palette.isDarkMode() ? 0.72f : 0.8f
        );
    }

    public static int resolvePhoneAppLabelColor(String stableKey, LauncherThemePalette palette) {
        if (isColorblindMode(palette)) {
            return resolveAppLabelColor(stableKey, palette);
        }
        return blendColors(
                resolvePhoneAppAccentColor(stableKey, palette),
                palette != null && palette.isDarkMode() ? Color.BLACK : Color.WHITE,
                palette != null && palette.isDarkMode() ? 0.14f : 0.08f
        );
    }

    public static int resolvePhoneAppCardColor(String stableKey, LauncherThemePalette palette) {
        if (isColorblindMode(palette)) {
            return resolveAppCardColor(stableKey, palette);
        }
        return blendColors(
                resolvePhoneAppAccentColor(stableKey, palette),
                resolvePhoneAppBlendBaseColor(palette),
                palette != null && palette.isDarkMode() ? 0.84f : 0.9f
        );
    }

    public static int resolvePhoneAppStrokeColor(String stableKey, LauncherThemePalette palette) {
        if (isColorblindMode(palette)) {
            return resolveAppStrokeColor(stableKey, palette);
        }
        return blendColors(
                resolvePhoneAppAccentColor(stableKey, palette),
                resolvePhoneAppBlendBaseColor(palette),
                palette != null && palette.isDarkMode() ? 0.28f : 0.22f
        );
    }

    public static int resolvePhoneAppTextColor(String stableKey, LauncherThemePalette palette) {
        int backgroundColor = isColorblindMode(palette)
                ? resolveAppLabelColor(stableKey, palette)
                : resolvePhoneAppLabelColor(stableKey, palette);
        return isDarkColor(backgroundColor) ? Color.WHITE : DEFAULT_PHONE_APP_TEXT_DARK;
    }

    public static GradientDrawable createCircleBackground(
            Context context,
            int fillColor,
            int strokeColor,
            int strokeWidthDp
    ) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(fillColor);
        if (strokeWidthDp > 0) {
            drawable.setStroke(dpToPx(context, strokeWidthDp), strokeColor);
        }
        return drawable;
    }

    public static GradientDrawable createRoundedBackground(
            Context context,
            int fillColor,
            int strokeColor,
            int cornerRadiusDp,
            int strokeWidthDp
    ) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dpToPx(context, cornerRadiusDp));
        drawable.setColor(fillColor);
        if (strokeWidthDp > 0) {
            drawable.setStroke(dpToPx(context, strokeWidthDp), strokeColor);
        }
        return drawable;
    }

    private static int blendColors(int fromColor, int toColor, float toRatio) {
        float fromRatio = 1f - toRatio;
        int red = Math.round(Color.red(fromColor) * fromRatio + Color.red(toColor) * toRatio);
        int green = Math.round(
                Color.green(fromColor) * fromRatio + Color.green(toColor) * toRatio
        );
        int blue = Math.round(Color.blue(fromColor) * fromRatio + Color.blue(toColor) * toRatio);
        return Color.rgb(red, green, blue);
    }

    private static int createUniquePhoneAppAccentColor(String stableKey, LauncherThemePalette palette) {
        long colorSeed = stableColorSeed(stableKey);
        int baseColor = palette != null ? palette.getPrimaryColor() : DEFAULT_PRIMARY;
        float[] hsv = new float[3];
        Color.colorToHSV(baseColor, hsv);

        float hue = wrapHue(hsv[0] + (normalizedSeedSegment(colorSeed, 0) * 360f));
        float saturationFloor = palette != null && palette.isDarkMode() ? 0.5f : 0.54f;
        float saturation = clamp(
                Math.max(hsv[1], saturationFloor)
                        + (normalizedSeedSegment(colorSeed, 12) - 0.5f) * 0.22f,
                saturationFloor,
                0.82f
        );
        float valueCenter = palette != null && palette.isDarkMode() ? 0.82f : 0.76f;
        float value = clamp(
                valueCenter + (normalizedSeedSegment(colorSeed, 24) - 0.5f) * 0.18f,
                palette != null && palette.isDarkMode() ? 0.72f : 0.64f,
                0.92f
        );
        return Color.HSVToColor(new float[]{hue, saturation, value});
    }

    private static int createUniqueColorblindAccentColor(
            String stableKey,
            LauncherThemePalette palette
    ) {
        long colorSeed = stableColorSeed(stableKey);
        float windowPosition = normalizedSeedSegment(colorSeed, 0);
        int windowIndex = Math.min(
                (int) (windowPosition * COLORBLIND_HUE_WINDOWS.length),
                COLORBLIND_HUE_WINDOWS.length - 1
        );
        float[] hueWindow = COLORBLIND_HUE_WINDOWS[windowIndex];
        float hue = hueWindow[0]
                + ((hueWindow[1] - hueWindow[0]) * normalizedSeedSegment(colorSeed, 12));
        float saturation = clamp(
                0.74f + (normalizedSeedSegment(colorSeed, 24) - 0.5f) * 0.18f,
                0.66f,
                0.9f
        );
        float valueCenter = palette != null && palette.isDarkMode() ? 0.84f : 0.76f;
        float value = clamp(
                valueCenter + (normalizedSeedSegment(colorSeed, 36) - 0.5f) * 0.2f,
                palette != null && palette.isDarkMode() ? 0.72f : 0.66f,
                0.94f
        );
        return Color.HSVToColor(new float[]{hue, saturation, value});
    }

    private static int resolvePhoneAppBlendBaseColor(LauncherThemePalette palette) {
        return palette != null ? palette.getBackgroundColor() : Color.WHITE;
    }

    private static boolean isDarkColor(int color) {
        double red = normalizeChannel(Color.red(color));
        double green = normalizeChannel(Color.green(color));
        double blue = normalizeChannel(Color.blue(color));
        double luminance = (0.2126 * red) + (0.7152 * green) + (0.0722 * blue);
        return luminance < 0.55;
    }

    private static double normalizeChannel(int value) {
        double channel = value / 255.0;
        return channel <= 0.03928
                ? channel / 12.92
                : Math.pow((channel + 0.055) / 1.055, 2.4);
    }

    private static long stableColorSeed(String stableKey) {
        long hash = 0xcbf29ce484222325L;
        String normalizedKey = stableKey != null ? stableKey : "";
        for (int index = 0; index < normalizedKey.length(); index++) {
            hash ^= normalizedKey.charAt(index);
            hash *= 0x100000001b3L;
        }
        hash ^= normalizedKey.length();
        hash *= 0x100000001b3L;
        return hash;
    }

    private static float normalizedSeedSegment(long colorSeed, int shift) {
        return ((colorSeed >>> shift) & 0x3FFL) / 1023f;
    }

    private static float clamp(float value, float minValue, float maxValue) {
        return Math.max(minValue, Math.min(maxValue, value));
    }

    private static float wrapHue(float hue) {
        float wrappedHue = hue % 360f;
        return wrappedHue < 0f ? wrappedHue + 360f : wrappedHue;
    }

    private static int dpToPx(Context context, int dpValue) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                context.getResources().getDisplayMetrics()
        ));
    }
}
