package com.rdr.easy2;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public final class LauncherThemePalette {
    public static final String KEY_GREEN = "green";
    public static final String KEY_BLUE = "blue";
    public static final String KEY_ORANGE = "orange";
    public static final String KEY_CORAL = "coral";
    public static final String KEY_TEAL = "teal";
    public static final String KEY_BLACK = "black";

    private static final List<LauncherThemePalette> OPTIONS = new ArrayList<>();

    static {
        OPTIONS.add(new LauncherThemePalette(
                KEY_GREEN,
                0xFF43A047,
                0xFF6BCF8B,
                0xFF52B86D,
                0xFFF2F7F2,
                0x3343A047,
                0xFFE9F6EC,
                0x5543A047,
                0xFFFFFFFF,
                0xFF43A047,
                0xFF000000,
                0xFF000000,
                0x99000000,
                false
        ));
        OPTIONS.add(new LauncherThemePalette(
                KEY_BLUE,
                0xFF2F80ED,
                0xFF73AEFF,
                0xFF4D95F2,
                0xFFF1F7FF,
                0x332F80ED,
                0xFFE8F1FF,
                0x552F80ED,
                0xFFFFFFFF,
                0xFF2F80ED,
                0xFF000000,
                0xFF000000,
                0x99000000,
                false
        ));
        OPTIONS.add(new LauncherThemePalette(
                KEY_ORANGE,
                0xFFF57C00,
                0xFFFFB45C,
                0xFFFF9B36,
                0xFFFFF6EC,
                0x33F57C00,
                0xFFFFEFD9,
                0x55F57C00,
                0xFFFFFFFF,
                0xFFF57C00,
                0xFF000000,
                0xFF000000,
                0x99000000,
                false
        ));
        OPTIONS.add(new LauncherThemePalette(
                KEY_CORAL,
                0xFFE85D75,
                0xFFF59BA9,
                0xFFEC758A,
                0xFFFFF1F4,
                0x33E85D75,
                0xFFFFE4EA,
                0x55E85D75,
                0xFFFFFFFF,
                0xFFE85D75,
                0xFF000000,
                0xFF000000,
                0x99000000,
                false
        ));
        OPTIONS.add(new LauncherThemePalette(
                KEY_TEAL,
                0xFF00897B,
                0xFF65D0C4,
                0xFF26B3A3,
                0xFFEEF9F7,
                0x3300897B,
                0xFFE0F6F2,
                0x5500897B,
                0xFFFFFFFF,
                0xFF00897B,
                0xFF000000,
                0xFF000000,
                0x99000000,
                false
        ));
        OPTIONS.add(new LauncherThemePalette(
                KEY_BLACK,
                0xFF000000,
                0xFF1B1B1B,
                0xFF2A2A2A,
                0xFF141414,
                0xFF343434,
                0xFF181818,
                0xFF343434,
                0xFF000000,
                0xFFFFFFFF,
                0xFFFFFFFF,
                0xFFFFFFFF,
                0x99FFFFFF,
                true
        ));
    }

    private final String key;
    private final int primaryColor;
    private final int circleColor;
    private final int chipColor;
    private final int setupFieldFillColor;
    private final int setupFieldStrokeColor;
    private final int setupContactFillColor;
    private final int setupContactStrokeColor;
    private final int backgroundColor;
    private final int headingColor;
    private final int bodyTextColor;
    private final int inputTextColor;
    private final int inputHintColor;
    private final boolean darkMode;

    private LauncherThemePalette(
            String key,
            int primaryColor,
            int circleColor,
            int chipColor,
            int setupFieldFillColor,
            int setupFieldStrokeColor,
            int setupContactFillColor,
            int setupContactStrokeColor,
            int backgroundColor,
            int headingColor,
            int bodyTextColor,
            int inputTextColor,
            int inputHintColor,
            boolean darkMode
    ) {
        this.key = key;
        this.primaryColor = primaryColor;
        this.circleColor = circleColor;
        this.chipColor = chipColor;
        this.setupFieldFillColor = setupFieldFillColor;
        this.setupFieldStrokeColor = setupFieldStrokeColor;
        this.setupContactFillColor = setupContactFillColor;
        this.setupContactStrokeColor = setupContactStrokeColor;
        this.backgroundColor = backgroundColor;
        this.headingColor = headingColor;
        this.bodyTextColor = bodyTextColor;
        this.inputTextColor = inputTextColor;
        this.inputHintColor = inputHintColor;
        this.darkMode = darkMode;
    }

    public static LauncherThemePalette fromKey(String key) {
        for (LauncherThemePalette palette : OPTIONS) {
            if (palette.key.equals(key)) {
                return palette;
            }
        }
        return OPTIONS.get(0);
    }

    public static LauncherThemePalette fromPreferences(Context context) {
        return fromKey(LauncherPreferences.getThemeColorKey(context));
    }

    public static List<LauncherThemePalette> getOptions() {
        return new ArrayList<>(OPTIONS);
    }

    public String getKey() {
        return key;
    }

    public int getPrimaryColor() {
        return primaryColor;
    }

    public int getCircleColor() {
        return circleColor;
    }

    public int getChipColor() {
        return chipColor;
    }

    public int getSetupFieldFillColor() {
        return setupFieldFillColor;
    }

    public int getSetupFieldStrokeColor() {
        return setupFieldStrokeColor;
    }

    public int getSetupContactFillColor() {
        return setupContactFillColor;
    }

    public int getSetupContactStrokeColor() {
        return setupContactStrokeColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public int getHeadingColor() {
        return headingColor;
    }

    public int getBodyTextColor() {
        return bodyTextColor;
    }

    public int getInputTextColor() {
        return inputTextColor;
    }

    public int getInputHintColor() {
        return inputHintColor;
    }

    public boolean isDarkMode() {
        return darkMode;
    }
}
