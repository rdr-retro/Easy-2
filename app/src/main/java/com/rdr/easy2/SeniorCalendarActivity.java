package com.rdr.easy2;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.Locale;

public class SeniorCalendarActivity extends AppCompatActivity {
    private static final int CALENDAR_WEEKS = 6;

    private VolumeOverlayController volumeOverlayController;
    private Locale locale;
    private LauncherThemePalette palette;
    private YearMonth displayedMonth;

    private TextView titleView;
    private TextView todayView;
    private TextView monthView;
    private TextView previousMonthButton;
    private TextView nextMonthButton;
    private LinearLayout weekdaysRow;
    private LinearLayout calendarWeeksContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_senior_calendar);
        volumeOverlayController = new VolumeOverlayController(this);
        locale = resolveLocale();
        displayedMonth = YearMonth.now();

        bindViews();
        findViewById(R.id.close_calendar_button).setOnClickListener(view -> finish());
        previousMonthButton.setOnClickListener(view -> {
            displayedMonth = displayedMonth.minusMonths(1);
            renderCalendar();
        });
        nextMonthButton.setOnClickListener(view -> {
            displayedMonth = displayedMonth.plusMonths(1);
            renderCalendar();
        });

        applyThemePalette();
        renderCalendar();
        enableFullscreen();
    }

    @Override
    protected void onDestroy() {
        if (volumeOverlayController != null) {
            volumeOverlayController.release();
        }
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableFullscreen();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (volumeOverlayController != null && volumeOverlayController.handleKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void bindViews() {
        titleView = findViewById(R.id.senior_calendar_title_view);
        todayView = findViewById(R.id.senior_calendar_today_view);
        monthView = findViewById(R.id.senior_calendar_month_view);
        previousMonthButton = findViewById(R.id.senior_calendar_previous_button);
        nextMonthButton = findViewById(R.id.senior_calendar_next_button);
        weekdaysRow = findViewById(R.id.senior_calendar_weekdays_row);
        calendarWeeksContainer = findViewById(R.id.senior_calendar_weeks_container);
    }

    private void enableFullscreen() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());

        if (controller == null) {
            return;
        }

        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
        controller.hide(WindowInsetsCompat.Type.systemBars());
    }

    private void applyThemePalette() {
        palette = LauncherThemePalette.fromPreferences(this);

        View rootView = findViewById(R.id.senior_calendar_root);
        if (rootView != null) {
            rootView.setBackgroundColor(palette.getBackgroundColor());
        }
        if (titleView != null) {
            titleView.setTextColor(palette.getHeadingColor());
        }
        if (todayView != null) {
            todayView.setTextColor(palette.getBodyTextColor());
            todayView.setBackground(null);
        }
        if (monthView != null) {
            monthView.setTextColor(palette.getHeadingColor());
        }
        styleNavigationButton(previousMonthButton);
        styleNavigationButton(nextMonthButton);

        if (volumeOverlayController != null) {
            volumeOverlayController.applyTheme(palette);
        }
    }

    private void styleNavigationButton(TextView button) {
        if (button == null) {
            return;
        }

        button.setTextColor(Color.WHITE);
        button.setBackground(createRoundedBackground(palette.getChipColor(), Color.TRANSPARENT, 28, 0));
    }

    private void renderCalendar() {
        if (palette == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        todayView.setText(getString(
                R.string.senior_calendar_today_format,
                capitalize(today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)))
        ));
        monthView.setText(capitalize(displayedMonth.format(
                DateTimeFormatter.ofPattern("LLLL yyyy", locale)
        )));

        renderWeekdays();
        renderWeeks(today);
    }

    private void renderWeekdays() {
        weekdaysRow.removeAllViews();
        DayOfWeek firstDayOfWeek = WeekFields.of(locale).getFirstDayOfWeek();

        for (int index = 0; index < 7; index++) {
            DayOfWeek day = firstDayOfWeek.plus(index);
            TextView dayLabel = new TextView(this);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(dpToPx(3), 0, dpToPx(3), 0);
            dayLabel.setLayoutParams(params);
            dayLabel.setGravity(Gravity.CENTER);
            dayLabel.setText(day.getDisplayName(TextStyle.NARROW_STANDALONE, locale).toUpperCase(locale));
            dayLabel.setTextColor(palette.getBodyTextColor());
            dayLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            dayLabel.setTypeface(Typeface.DEFAULT_BOLD);
            weekdaysRow.addView(dayLabel);
        }
    }

    private void renderWeeks(LocalDate today) {
        calendarWeeksContainer.removeAllViews();

        DayOfWeek firstDayOfWeek = WeekFields.of(locale).getFirstDayOfWeek();
        LocalDate firstOfMonth = displayedMonth.atDay(1);
        int leadingEmptyDays = Math.floorMod(
                firstOfMonth.getDayOfWeek().getValue() - firstDayOfWeek.getValue(),
                7
        );

        int dayNumber = 1;
        for (int week = 0; week < CALENDAR_WEEKS; week++) {
            LinearLayout weekRow = new LinearLayout(this);
            weekRow.setOrientation(LinearLayout.HORIZONTAL);
            weekRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            for (int day = 0; day < 7; day++) {
                int slot = week * 7 + day;
                boolean belongsToDisplayedMonth =
                        slot >= leadingEmptyDays && dayNumber <= displayedMonth.lengthOfMonth();
                Integer visibleDay = belongsToDisplayedMonth ? dayNumber : null;
                boolean isToday = belongsToDisplayedMonth
                        && today.getYear() == displayedMonth.getYear()
                        && today.getMonth() == displayedMonth.getMonth()
                        && today.getDayOfMonth() == dayNumber;

                weekRow.addView(createDayCell(visibleDay, isToday));

                if (belongsToDisplayedMonth) {
                    dayNumber++;
                }
            }

            calendarWeeksContainer.addView(weekRow);
        }
    }

    private View createDayCell(Integer dayNumber, boolean isToday) {
        TextView dayView = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dpToPx(68), 1f);
        params.setMargins(dpToPx(2), dpToPx(4), dpToPx(2), 0);
        dayView.setLayoutParams(params);
        dayView.setGravity(Gravity.CENTER);
        dayView.setTypeface(Typeface.DEFAULT_BOLD);
        dayView.setTextSize(TypedValue.COMPLEX_UNIT_SP, isToday ? 30 : 24);
        dayView.setIncludeFontPadding(false);

        if (dayNumber == null) {
            dayView.setText("");
            dayView.setBackground(null);
            dayView.setClickable(false);
            dayView.setFocusable(false);
            return dayView;
        }

        dayView.setText(String.valueOf(dayNumber));
        dayView.setBackground(null);

        if (isToday) {
            dayView.setTextColor(palette.getPrimaryColor());
            dayView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        } else {
            dayView.setTextColor(palette.getBodyTextColor());
        }

        return dayView;
    }

    private GradientDrawable createRoundedBackground(
            int fillColor,
            int strokeColor,
            int cornerRadiusDp,
            int strokeWidthDp
    ) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dpToPx(cornerRadiusDp));
        drawable.setColor(fillColor);

        if (strokeWidthDp > 0) {
            drawable.setStroke(dpToPx(strokeWidthDp), strokeColor);
        }

        return drawable;
    }

    private Locale resolveLocale() {
        LocaleList locales = getResources().getConfiguration().getLocales();
        return locales.isEmpty() ? Locale.getDefault() : locales.get(0);
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private int dpToPx(int dpValue) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                getResources().getDisplayMetrics()
        ));
    }
}
