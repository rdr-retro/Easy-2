package com.rdr.easy2;

import android.net.Uri;
import android.text.TextUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

public final class Gs1DateParser {
    private static final char GROUP_SEPARATOR = 29;
    private static final List<String> SUPPORTED_AIS = Arrays.asList(
            "7003",
            "8008",
            "01",
            "10",
            "11",
            "13",
            "15",
            "17",
            "21",
            "22"
    );

    private Gs1DateParser() {
    }

    public static ParsedDate parse(String rawValue) {
        if (TextUtils.isEmpty(rawValue)) {
            return null;
        }

        String value = rawValue.trim();
        ParsedDate parsedDate = parseDigitalLink(value);
        if (parsedDate != null) {
            return parsedDate;
        }

        parsedDate = parseParenthesized(value);
        if (parsedDate != null) {
            return parsedDate;
        }

        return parseElementString(value);
    }

    private static ParsedDate parseDigitalLink(String value) {
        Uri uri = Uri.parse(value);
        if (uri == null || TextUtils.isEmpty(uri.getScheme())) {
            return null;
        }

        ParsedDate parsedDate = createParsedDate("17", uri.getQueryParameter("17"));
        if (parsedDate != null) {
            return parsedDate;
        }

        parsedDate = createParsedDate("15", uri.getQueryParameter("15"));
        if (parsedDate != null) {
            return parsedDate;
        }

        List<String> segments = uri.getPathSegments();
        for (int index = 0; index < segments.size() - 1; index++) {
            String ai = segments.get(index);
            if ("17".equals(ai) || "15".equals(ai)) {
                parsedDate = createParsedDate(ai, segments.get(index + 1));
                if (parsedDate != null) {
                    return parsedDate;
                }
            }
        }

        return null;
    }

    private static ParsedDate parseParenthesized(String value) {
        for (String ai : Arrays.asList("17", "15")) {
            String marker = "(" + ai + ")";
            int markerIndex = value.indexOf(marker);
            if (markerIndex < 0) {
                continue;
            }

            int start = markerIndex + marker.length();
            if (start + 6 > value.length()) {
                continue;
            }

            ParsedDate parsedDate = createParsedDate(ai, value.substring(start, start + 6));
            if (parsedDate != null) {
                return parsedDate;
            }
        }

        return null;
    }

    private static ParsedDate parseElementString(String value) {
        String normalized = stripSymbologyIdentifier(value)
                .replace(String.valueOf(GROUP_SEPARATOR), "|");
        int index = 0;

        while (index < normalized.length()) {
            char currentChar = normalized.charAt(index);
            if (currentChar == '|') {
                index++;
                continue;
            }

            String ai = matchAi(normalized, index);
            if (ai == null) {
                return null;
            }

            index += ai.length();
            if ("10".equals(ai) || "21".equals(ai) || "22".equals(ai)) {
                while (index < normalized.length() && normalized.charAt(index) != '|') {
                    index++;
                }
                continue;
            }

            int valueLength = getFixedLength(ai);
            if (valueLength <= 0 || index + valueLength > normalized.length()) {
                return null;
            }

            String elementValue = normalized.substring(index, index + valueLength);
            if (!TextUtils.isDigitsOnly(elementValue)) {
                return null;
            }

            if ("17".equals(ai) || "15".equals(ai)) {
                return createParsedDate(ai, elementValue);
            }

            index += valueLength;
        }

        return null;
    }

    private static String stripSymbologyIdentifier(String value) {
        if (value.length() >= 3 && value.charAt(0) == ']') {
            return value.substring(3);
        }
        return value;
    }

    private static String matchAi(String value, int index) {
        for (String ai : SUPPORTED_AIS) {
            if (value.startsWith(ai, index)) {
                return ai;
            }
        }
        return null;
    }

    private static int getFixedLength(String ai) {
        switch (ai) {
            case "01":
                return 14;
            case "11":
            case "13":
            case "15":
            case "17":
                return 6;
            case "7003":
                return 10;
            case "8008":
                return 12;
            default:
                return -1;
        }
    }

    private static ParsedDate createParsedDate(String ai, String value) {
        if (TextUtils.isEmpty(value) || value.length() != 6 || !TextUtils.isDigitsOnly(value)) {
            return null;
        }

        int year = resolveYear(Integer.parseInt(value.substring(0, 2)));
        int month = Integer.parseInt(value.substring(2, 4));
        int day = Integer.parseInt(value.substring(4, 6));

        if (month < 1 || month > 12) {
            return null;
        }

        ParsedDate.Type type = "17".equals(ai)
                ? ParsedDate.Type.EXPIRATION
                : ParsedDate.Type.BEST_BEFORE;

        if (day == 0) {
            return new ParsedDate(type, null, YearMonth.of(year, month));
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        if (day > yearMonth.lengthOfMonth()) {
            return null;
        }

        return new ParsedDate(type, LocalDate.of(year, month, day), null);
    }

    private static int resolveYear(int twoDigitYear) {
        int currentYear = LocalDate.now().getYear();
        int[] candidates = new int[]{
                1900 + twoDigitYear,
                2000 + twoDigitYear,
                2100 + twoDigitYear
        };

        for (int candidate : candidates) {
            if (candidate >= currentYear - 49 && candidate <= currentYear + 50) {
                return candidate;
            }
        }

        return 2000 + twoDigitYear;
    }

    public static final class ParsedDate {
        public enum Type {
            EXPIRATION,
            BEST_BEFORE
        }

        private final Type type;
        private final LocalDate exactDate;
        private final YearMonth monthOnlyDate;

        private ParsedDate(Type type, LocalDate exactDate, YearMonth monthOnlyDate) {
            this.type = type;
            this.exactDate = exactDate;
            this.monthOnlyDate = monthOnlyDate;
        }

        public Type getType() {
            return type;
        }

        public LocalDate getExactDate() {
            return exactDate;
        }

        public YearMonth getMonthOnlyDate() {
            return monthOnlyDate;
        }
    }
}
