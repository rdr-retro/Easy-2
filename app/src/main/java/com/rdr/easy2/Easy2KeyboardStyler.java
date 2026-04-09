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
        int fillColor = blend(
                palette.getSetupContactFillColor(),
                Color.WHITE,
                palette.isDarkMode() ? 0.08f : 0.36f
        );

        panel.setElevation(0f);
        panel.setBackground(createSolidBackground(context, fillColor, 0));
    }

    public static void styleHandle(View handleView, LauncherThemePalette palette) {
        if (handleView == null || palette == null) {
            return;
        }

        handleView.setBackgroundColor(Color.TRANSPARENT);
    }

    public static void styleCharacterKey(TextView keyView, LauncherThemePalette palette) {
        if (keyView == null || palette == null) {
            return;
        }

        int fillColor = palette.isDarkMode()
                ? blend(palette.getSetupFieldFillColor(), Color.WHITE, 0.08f)
                : blend(palette.getBackgroundColor(), Color.WHITE, 0.7f);
        int rippleColor = withAlpha(palette.getPrimaryColor(), palette.isDarkMode() ? 80 : 44);

        styleKey(keyView, palette.getInputTextColor(), fillColor, rippleColor);
    }

    public static void styleSpaceKey(TextView keyView, LauncherThemePalette palette) {
        if (keyView == null || palette == null) {
            return;
        }

        int fillColor = palette.isDarkMode()
                ? blend(palette.getSetupContactFillColor(), Color.WHITE, 0.14f)
                : blend(palette.getSetupContactFillColor(), Color.WHITE, 0.34f);
        int rippleColor = withAlpha(palette.getPrimaryColor(), palette.isDarkMode() ? 88 : 52);

        styleKey(keyView, palette.getBodyTextColor(), fillColor, rippleColor);
    }

    public static void styleEnterKey(TextView keyView, LauncherThemePalette palette) {
        if (keyView == null || palette == null) {
            return;
        }

        int fillColor = palette.isDarkMode()
                ? blend(palette.getPrimaryColor(), Color.WHITE, 0.08f)
                : palette.getPrimaryColor();
        int rippleColor = withAlpha(Color.WHITE, 56);

        styleKey(keyView, Color.WHITE, fillColor, rippleColor);
    }

    public static void styleHideKey(TextView keyView, LauncherThemePalette palette) {
        if (keyView == null || palette == null) {
            return;
        }

        int fillColor = blend(
                palette.getCircleColor(),
                palette.getSetupContactFillColor(),
                palette.isDarkMode() ? 0.2f : 0.42f
        );
        int rippleColor = withAlpha(palette.getPrimaryColor(), palette.isDarkMode() ? 78 : 44);

        styleKey(
                keyView,
                getReadableTextColor(fillColor),
                fillColor,
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
                0xFFD8443E,
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

        int fillColor = blend(
                palette.getChipColor(),
                palette.getSetupContactFillColor(),
                palette.isDarkMode() ? 0.28f : 0.46f
        );
        int rippleColor = withAlpha(Color.WHITE, palette.isDarkMode() ? 56 : 46);

        styleKey(
                keyView,
                getReadableTextColor(fillColor),
                fillColor,
                rippleColor
        );
    }

    private static void styleKey(
            TextView keyView,
            int textColor,
            int fillColor,
            int rippleColor
    ) {
        Context context = keyView.getContext();
        keyView.setTextColor(textColor);
        keyView.setElevation(0f);
        keyView.setBackground(createRippleBackground(
                context,
                fillColor,
                0,
                rippleColor
        ));
    }

    private static RippleDrawable createRippleBackground(
            Context context,
            int fillColor,
            int radiusDp,
            int rippleColor
    ) {
        GradientDrawable content = createSolidBackground(context, fillColor, radiusDp);
        GradientDrawable mask = createSolidBackground(context, Color.WHITE, radiusDp);
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask);
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
