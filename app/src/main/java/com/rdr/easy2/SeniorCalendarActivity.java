package com.rdr.easy2;

import android.content.ContentUris;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.CalendarContract;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
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
    private LocalDate selectedDate;

    private TextView titleView;
    private TextView todayView;
    private TextView monthView;
    private TextView previousMonthButton;
    private TextView nextMonthButton;
    private TextView selectedDayTitleView;
    private TextView selectedDayValueView;
    private TextView selectedDayHintView;
    private TextView openDayButton;
    private TextView addEventButton;
    private TextView goToTodayButton;
    private LinearLayout weekdaysRow;
    private LinearLayout calendarWeeksContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_senior_calendar);
        volumeOverlayController = new VolumeOverlayController(this);
        locale = resolveLocale();
        displayedMonth = YearMonth.now();
        selectedDate = LocalDate.now();

        bindViews();
        findViewById(R.id.close_calendar_button).setOnClickListener(view -> finish());
        previousMonthButton.setOnClickListener(view -> {
            displayedMonth = displayedMonth.minusMonths(1);
            syncSelectionWithDisplayedMonth();
            renderCalendar();
        });
        nextMonthButton.setOnClickListener(view -> {
            displayedMonth = displayedMonth.plusMonths(1);
            syncSelectionWithDisplayedMonth();
            renderCalendar();
        });
        openDayButton.setOnClickListener(view -> openSelectedDateInCalendar());
        addEventButton.setOnClickListener(view -> createEventForSelectedDate());
        goToTodayButton.setOnClickListener(view -> {
            selectedDate = LocalDate.now();
            displayedMonth = YearMonth.from(selectedDate);
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
        selectedDayTitleView = findViewById(R.id.senior_calendar_selected_day_title);
        selectedDayValueView = findViewById(R.id.senior_calendar_selected_day_value);
        selectedDayHintView = findViewById(R.id.senior_calendar_selected_day_hint);
        openDayButton = findViewById(R.id.senior_calendar_open_button);
        addEventButton = findViewById(R.id.senior_calendar_add_event_button);
        goToTodayButton = findViewById(R.id.senior_calendar_go_today_button);
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
        if (selectedDayTitleView != null) {
            selectedDayTitleView.setTextColor(palette.getHeadingColor());
        }
        if (selectedDayValueView != null) {
            selectedDayValueView.setTextColor(palette.getHeadingColor());
        }
        if (selectedDayHintView != null) {
            selectedDayHintView.setTextColor(palette.getBodyTextColor());
        }
        styleNavigationButton(previousMonthButton, "calendar_previous_month");
        styleNavigationButton(nextMonthButton, "calendar_next_month");
        styleActionButton(openDayButton, "calendar_open_day", palette.getPrimaryColor());
        styleActionButton(addEventButton, "calendar_add_event", palette.getChipColor());
        styleActionButton(goToTodayButton, "calendar_go_today", palette.getCircleColor());

        if (volumeOverlayController != null) {
            volumeOverlayController.applyTheme(palette);
        }
    }

    private void styleNavigationButton(TextView button, String stableKey) {
        if (button == null) {
            return;
        }

        int fillColor = ColorblindStyleHelper.resolveSemanticAccentColor(
                stableKey,
                palette.getChipColor(),
                palette
        );
        button.setTextColor(ColorblindStyleHelper.resolveTextColorForBackground(fillColor));
        button.setBackground(createRoundedBackground(
                fillColor,
                fillColor,
                28,
                ColorblindStyleHelper.isColorblindMode(palette) ? 3 : 0
        ));
    }

    private void styleActionButton(TextView button, String stableKey, int fallbackColor) {
        if (button == null) {
            return;
        }

        int fillColor = ColorblindStyleHelper.resolveSemanticAccentColor(
                stableKey,
                fallbackColor,
                palette
        );
        button.setTextColor(ColorblindStyleHelper.resolveTextColorForBackground(fillColor));
        button.setBackground(createRoundedBackground(
                fillColor,
                fillColor,
                24,
                ColorblindStyleHelper.isColorblindMode(palette) ? 3 : 0
        ));
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

        updateSelectedDayPanel();
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
                LocalDate date = belongsToDisplayedMonth
                        ? displayedMonth.atDay(dayNumber)
                        : null;
                boolean isToday = belongsToDisplayedMonth
                        && today.getYear() == displayedMonth.getYear()
                        && today.getMonth() == displayedMonth.getMonth()
                        && today.getDayOfMonth() == dayNumber;
                boolean isSelected = date != null && date.equals(selectedDate);

                weekRow.addView(createDayCell(date, visibleDay, isToday, isSelected));

                if (belongsToDisplayedMonth) {
                    dayNumber++;
                }
            }

            calendarWeeksContainer.addView(weekRow);
        }
    }

    private View createDayCell(
            LocalDate date,
            Integer dayNumber,
            boolean isToday,
            boolean isSelected
    ) {
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
        if (isSelected) {
            int selectedFillColor = ColorblindStyleHelper.resolveSemanticAccentColor(
                    "calendar_selected_day",
                    palette.getPrimaryColor(),
                    palette
            );
            dayView.setTextColor(ColorblindStyleHelper.resolveTextColorForBackground(selectedFillColor));
            dayView.setBackground(createRoundedBackground(
                    selectedFillColor,
                    selectedFillColor,
                    20,
                    ColorblindStyleHelper.isColorblindMode(palette) ? 3 : 0
            ));
        } else if (isToday) {
            int todayAccentColor = ColorblindStyleHelper.resolveSemanticAccentColor(
                    "calendar_today_day",
                    palette.getPrimaryColor(),
                    palette
            );
            dayView.setTextColor(todayAccentColor);
            dayView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
            dayView.setBackground(createRoundedBackground(
                    Color.TRANSPARENT,
                    todayAccentColor,
                    20,
                    ColorblindStyleHelper.isColorblindMode(palette) ? 3 : 2
            ));
        } else {
            dayView.setTextColor(palette.getBodyTextColor());
            dayView.setBackground(null);
        }
        dayView.setClickable(true);
        dayView.setFocusable(true);
        dayView.setOnClickListener(view -> {
            if (date == null) {
                return;
            }
            selectedDate = date;
            renderCalendar();
        });

        return dayView;
    }

    private void updateSelectedDayPanel() {
        if (selectedDayValueView == null || selectedDayHintView == null) {
            return;
        }

        if (selectedDate == null) {
            selectedDayValueView.setText("");
            selectedDayHintView.setText(R.string.senior_calendar_selected_day_hint);
            return;
        }

        String formattedDate = capitalize(selectedDate.format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
        ));
        selectedDayValueView.setText(formattedDate);
        selectedDayHintView.setText(R.string.senior_calendar_selected_day_hint);
    }

    private void syncSelectionWithDisplayedMonth() {
        if (selectedDate != null && YearMonth.from(selectedDate).equals(displayedMonth)) {
            return;
        }

        LocalDate today = LocalDate.now();
        if (YearMonth.from(today).equals(displayedMonth)) {
            selectedDate = today;
            return;
        }

        selectedDate = displayedMonth.atDay(1);
    }

    private void openSelectedDateInCalendar() {
        if (selectedDate == null) {
            return;
        }

        long startMillis = toStartOfDayMillis(selectedDate);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(
                ContentUris.appendId(CalendarContract.CONTENT_URI.buildUpon().appendPath("time"), startMillis)
                        .build()
        );

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
            return;
        }

        Toast.makeText(this, R.string.senior_calendar_calendar_unavailable, Toast.LENGTH_SHORT).show();
    }

    private void createEventForSelectedDate() {
        if (selectedDate == null) {
            return;
        }

        long startMillis = toStartOfDayMillis(selectedDate);
        long endMillis = startMillis + (60L * 60L * 1000L);
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setData(CalendarContract.Events.CONTENT_URI);
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis);
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis);
        intent.putExtra(CalendarContract.Events.TITLE, getString(R.string.senior_calendar_event_title));

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
            return;
        }

        Toast.makeText(this, R.string.senior_calendar_calendar_unavailable, Toast.LENGTH_SHORT).show();
    }

    private long toStartOfDayMillis(LocalDate date) {
        return date
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
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
