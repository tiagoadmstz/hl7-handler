package io.github.tdteixeira.hl7handler.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

/**
 * High-performance parser for HL7 v2 TS (Time Stamp) and DTM (Date/Time) data types.
 * Supports variable precisions (YYYY, YYYYMM, YYYYMMDD, YYYYMMDDHHMM, YYYYMMDDHHMMSS, decimal seconds)
 * and optional timezones (±ZZZZ).
 */
public final class Hl7DateTime {

    private Hl7DateTime() {}

    private static final class ParsedDateTime {
        private final LocalDateTime localDateTime;
        private final ZoneOffset offset;
        private final boolean isDateOnly;

        ParsedDateTime(LocalDateTime localDateTime, ZoneOffset offset, boolean isDateOnly) {
            this.localDateTime = localDateTime;
            this.offset = offset;
            this.isDateOnly = isDateOnly;
        }
    }

    /**
     * Parses an HL7 timestamp string into the most appropriate java.time type:
     * - YYYY, YYYYMM, YYYYMMDD -> LocalDate
     * - YYYYMMDDHHMM, YYYYMMDDHHMMSS[.SSSS] -> LocalDateTime
     * - with timezone ±ZZZZ -> OffsetDateTime
     */
    public static TemporalAccessor parse(String text) {
        ParsedDateTime dt = parseInternal(text);
        if (dt.offset != null) {
            return OffsetDateTime.of(dt.localDateTime, dt.offset);
        }
        if (dt.isDateOnly) {
            return dt.localDateTime.toLocalDate();
        }
        return dt.localDateTime;
    }

    /**
     * Parses an HL7 timestamp string into an OffsetDateTime, using UTC as fallback if no timezone is present.
     */
    public static OffsetDateTime parseOffsetDateTime(String text) {
        return parseOffsetDateTime(text, ZoneOffset.UTC);
    }

    /**
     * Parses an HL7 timestamp string into an OffsetDateTime, using the given default offset if no timezone is present.
     */
    public static OffsetDateTime parseOffsetDateTime(String text, ZoneOffset defaultOffset) {
        ParsedDateTime dt = parseInternal(text);
        ZoneOffset effectiveOffset = dt.offset != null ? dt.offset : (defaultOffset != null ? defaultOffset : ZoneOffset.UTC);
        return OffsetDateTime.of(dt.localDateTime, effectiveOffset);
    }

    /**
     * Parses an HL7 timestamp string into a LocalDateTime (ignoring any timezone offset if present).
     */
    public static LocalDateTime parseLocalDateTime(String text) {
        ParsedDateTime dt = parseInternal(text);
        return dt.localDateTime;
    }

    /**
     * Parses an HL7 timestamp string into a LocalDate (YYYY, YYYYMM, or YYYYMMDD).
     */
    public static LocalDate parseLocalDate(String text) {
        ParsedDateTime dt = parseInternal(text);
        return dt.localDateTime.toLocalDate();
    }

    private static ParsedDateTime parseInternal(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new DateTimeParseException("HL7 date/time text cannot be null or empty", text != null ? text : "", 0);
        }
        String clean = text.trim();

        ZoneOffset offset = null;
        String dateTimePart = clean;

        int tzIdx = Math.max(clean.lastIndexOf('+'), clean.lastIndexOf('-'));
        if (tzIdx >= 4) {
            String tzStr = clean.substring(tzIdx);
            try {
                offset = ZoneOffset.of(tzStr);
                dateTimePart = clean.substring(0, tzIdx);
            } catch (Exception ignored) {
                // Not a valid timezone offset, keep as part of datetime string
            }
        } else if (clean.endsWith("Z") || clean.endsWith("z")) {
            offset = ZoneOffset.UTC;
            dateTimePart = clean.substring(0, clean.length() - 1);
        }

        int nanos = 0;
        boolean hasFraction = false;
        int dotIdx = dateTimePart.indexOf('.');
        String digits = dateTimePart;
        if (dotIdx != -1) {
            hasFraction = true;
            digits = dateTimePart.substring(0, dotIdx);
            String fracStr = dateTimePart.substring(dotIdx + 1);
            nanos = parseNanos(fracStr);
        }

        int len = digits.length();
        if (len < 4) {
            throw new DateTimeParseException("HL7 date/time must contain at least a 4-digit year: " + text, text, 0);
        }

        int year = Integer.parseInt(digits.substring(0, 4));
        int month = (len >= 6) ? Integer.parseInt(digits.substring(4, 6)) : 1;
        int day = (len >= 8) ? Integer.parseInt(digits.substring(6, 8)) : 1;
        int hour = (len >= 10) ? Integer.parseInt(digits.substring(8, 10)) : 0;
        int minute = (len >= 12) ? Integer.parseInt(digits.substring(10, 12)) : 0;
        int second = (len >= 14) ? Integer.parseInt(digits.substring(12, 14)) : 0;

        boolean isDateOnly = (len <= 8 && !hasFraction);
        LocalDateTime ldt = LocalDateTime.of(year, month, day, hour, minute, second, nanos);
        return new ParsedDateTime(ldt, offset, isDateOnly);
    }

    private static int parseNanos(String fracStr) {
        if (fracStr == null || fracStr.isEmpty()) {
            return 0;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fracStr.length(); i++) {
            char c = fracStr.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
            } else {
                break;
            }
        }
        if (sb.length() == 0) {
            return 0;
        }
        if (sb.length() > 9) {
            sb.setLength(9);
        }
        while (sb.length() < 9) {
            sb.append('0');
        }
        return Integer.parseInt(sb.toString());
    }
}
