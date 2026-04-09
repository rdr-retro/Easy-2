package com.rdr.easy2;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

public final class Easy2KeyboardStyler {
    private Easy2KeyboardStyler() {
    }

    public static void stylePanel(View panel, LauncherThemePalette palette) {
        if (panel == null || palette == null) {
            return;
        }

        Context context = panel.getContext();
        int startColor = blend(
                palette.getSetupContactFillColor(),
                Color.WHITE,
                palette.isDarkMode() ? 0.06f : 0.42f
        );
        int endColor = blend(
                palette.getSetupContactFillColor(),
                palette.getPrimaryColor(),
                palette.isDarkMode() ? 0.16f : 0.08f
        );
        int strokeColor = palette.isDarkMode()
                ? withAlpha(Color.WHITE, 42)
                : withAlpha(palette.getPrimaryColor(), 58);

        panel.setElevation(dpToPx(context, 20));
        panel.setBackground(createGradientBackground(
                context,
                startColor,
                endColor,
                strokeColor,
                28,
                2
        ));
    }

    public static void styleHandle(View handleView, LauncherThemePalette palette) {
        if (handleView == null || palette == null) {
            return;
        }

        int fillColor = palette.isDarkMode()
                ? withAlpha(Color.WHITE, 72)
                : withAlpha(palette.getPrimaryColor(), 78);
        handleView.setBackground(createSolidBackground(handleView.getContext(), fillColor, 999));
    }

    public static void styleCharacterKey(TextView keyView, LauncherThemePalette palette) {
        if (keyView == null || palette == null) {
            return;
        }

        int startColor = palette.isDarkMode()
                ? blend(palette.getSetupFieldFillColor(), Color.WHITE, 0.06f)
                : Color.WHITE;
        int endColor = blend(
                palette.getSetupFieldFillColor(),
                palette.getSetupContactFillColor(),
                palette.isDarkMode() ? 0.18f : 0.26f
        );
        int strokeColor = palette.isDarkMode()
                ? withAlpha(Color.WHITE, 28)
                : withAlpha(palette.getPrimaryColor(), 36);
        int rippleColor = withAlpha(palette.getPrimaryColor(), palette.isDarkMode() ? 80 : 44);

        styleKey(keyView, palette.getInputTextColor(), startColor, endColor, strokeColor, rippleColor);
    }

    public static void styleSpaceKey(TextView keyView, LauncherThemePalette palette) {
        if (keyView == null || palette == null) {
            return;
        }

        int startColor = palette.isDarkMode()
                ? blend(palette.getSetupContactFillColor(), Color.WHITE, 0.08f)
                : Color.WHITE;
        int endColor = blend(
                palette.getSetupContactFillColor(),
                palette.getSetupFieldFillColor(),
                palette.isDarkMode() ? 0.26f : 0.46f
        );
        int strokeColor = palette.isDarkMode()
                ? withAlpha(Color.WHITE, 32)
                : withAlpha(palette.getPrimaryColor(), 42);
        int rippleColor = withAlpha(palette.getPrimaryColor(), palette.isDarkMode() ? 88 : 52);

        styleKey(keyView, palette.getBodyTextColor(), startColor, endColor, strokeColor, rippleColor);
    }

    public static void styleEnterKey(TextView keyView, LauncherThemePalette palette) {
        if (keyView == null || palette == null) {
            return;
        }

        int startColor = blend(palette.getPrimaryColor(), Color.WHITE, 0.16f);
        int endColor = blend(
                palette.getPrimaryColor(),
                Color.BLACK,
                palette.isDarkMode() ? 0.22f : 0.08f
        );
        int strokeColor = blend(palette.getPrimaryColor(), Color.BLACK, 0.22f);
        int rippleColor = withAlpha(Color.WHITE, 56);

        styleKey(keyView, Color.WHITE, startColor, endColor, strokeColor, rippleColor);
    }

    public static void styleHideKey(TextView keyView, LauncherThemePalette palette) {
        if (keyView == null || palette == null) {
            return;
        }

        int startColor = blend(
                palette.getCircleColor(),
                Color.WHITE,
                palette.isDarkMode() ? 0.05f : 0.18f
        );
        int endColor = blend(
                palette.getCircleColor(),
                palette.getSetupContactFillColor(),
                palette.isDarkMode() ? 0.12f : 0.28f
        );
        int strokeColor = palette.isDarkMode()
                ? withAlpha(Color.WHITE, 36)
                : withAlpha(palette.getCircleColor(), 86);
        int rippleColor = withAlpha(palette.getPrimaryColor(), palette.isDarkMode() ? 78 : 44);

        styleKey(
                keyView,
                getReadableTextColor(endColor),
                startColor,
                endColor,
                strokeColor,
                rippleColor
        );
    }

    public static void styleDeleteKey(TextView keyView) {
        if (keyView == null) {
            return;
        }

        styleKey(
                keyView,
                Color.WHITE,
                0xFFF06E68,
                0xFFD8443E,
                0xFFAE2F2A,
                withAlpha(Color.WHITE, 52)
        );
    }

    public static void styleShiftKey(
            TextView keyView,
            LauncherThemePalette palette,
            boolean shifted
    ) {
        if (keyView == null || palette == null) {
            return;
        }

        if (shifted) {
            styleEnterKey(keyView, palette);
            return;
        }

        int startColor = blend(
                palette.getChipColor(),
                Color.WHITE,
                palette.isDarkMode() ? 0.05f : 0.14f
        );
        int endColor = blend(
                palette.getChipColor(),
                palette.getPrimaryColor(),
                palette.isDarkMode() ? 0.18f : 0.08f
        );
        int strokeColor = palette.isDarkMode()
                ? withAlpha(Color.WHITE, 26)
                : withAlpha(palette.getChipColor(), 110);
        int rippleColor = withAlpha(Color.WHITE, palette.isDarkMode() ? 56 : 46);

        styleKey(
                keyView,
                getReadableTextColor(endColor),
                startColor,
                endColor,
                strokeColor,
                rippleColor
        );
    }

    private static void styleKey(
            TextView keyView,
            int textColor,
            int startColor,
            int endColor,
            int strokeColor,
            int rippleColor
    ) {
        Context context = keyView.getContext();
        keyView.setTextColor(textColor);
        keyView.setElevation(dpToPx(context, 2));
        keyView.setBackground(createRippleBackground(
                context,
                startColor,
                endColor,
                strokeColor,
                20,
                1,
                rippleColor
        ));
    }

    private static RippleDrawable createRippleBackground(
            Context context,
            int startColor,
            int endColor,
            int strokeColor,
            int radiusDp,
            int strokeWidthDp,
            int rippleColor
    ) {
        GradientDrawable content = createGradientBackground(
                context,
                startColor,
                endColor,
                strokeColor,
                radiusDp,
                strokeWidthDp
        );
        GradientDrawable mask = createSolidBackground(context, Color.WHITE, radiusDp);
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask);
    }

    private static GradientDrawable createGradientBackground(
            Context context,
            int startColor,
            int endColor,
            int strokeColor,
            int radiusDp,
            int strokeWidthDp
    ) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{startColor, endColor}
        );
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dpToPx(context, radiusDp));
        if (strokeWidthDp > 0) {
            drawable.setStroke(dpToPx(context, strokeWidthDp), strokeColor);
        }
        return drawable;
    }

    private static GradientDrawable createSolidBackground(Context context, int fillColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dpToPx(context, radiusDp));
        return drawable;
    }

    private static int getReadableTextColor(int backgroundColor) {
        double luminance = (
                (0.299 * Color.red(backgroundColor))
                        + (0.587 * Color.green(backgroundColor))
                        + (0.114 * Color.blue(backgroundColor))
        ) / 255d;
        return luminance < 0.56d ? Color.WHITE : Color.BLACK;
    }

    private static int blend(int baseColor, int targetColor, float ratio) {
        float clampedRatio = Math.max(0f, Math.min(1f, ratio));
        float inverseRatio = 1f - clampedRatio;
        return Color.argb(
                Math.round((Color.alpha(baseColor) * inverseRatio) + (Color.alpha(targetColor) * clampedRatio)),
                Math.round((Color.red(baseColor) * inverseRatio) + (Color.red(targetColor) * clampedRatio)),
                Math.round((Color.green(baseColor) * inverseRatio) + (Color.green(targetColor) * clampedRatio)),
                Math.round((Color.blue(baseColor) * inverseRatio) + (Color.blue(targetColor) * clampedRatio))
        );
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(
                Math.max(0, Math.min(255, alpha)),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );
    }

    private static int dpToPx(Context context, int dpValue) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                context.getResources().getDisplayMetrics()
        ));
    }
}
