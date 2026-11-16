package in.co.bel.acempp.util;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Date;
import java.util.Locale;

public final class DateTimeConversionUtil {

    public static final String UI_DATE_PATTERN = "dd-MM-yyyy";
    public static final String UI_DATE_TIME_PATTERN = "dd-MM-yyyy HH:mm:ss";
    public static final String SUPPORTED_DATE_FORMATS = "dd-MM-yyyy or yyyy-MM-dd";
    public static final String SUPPORTED_DATE_TIME_FORMATS =
            "dd-MM-yyyy HH:mm:ss, dd-MM-yyyy HH:mm, an ISO date-time, dd-MM-yyyy, or yyyy-MM-dd";

    private static final DateTimeFormatter UI_DATE = strictFormatter("dd-MM-uuuu");
    private static final DateTimeFormatter UI_DATE_TIME =
            strictFormatter("dd-MM-uuuu HH:mm:ss");
    private static final DateTimeFormatter LEGACY_UI_DATE_TIME =
            strictFormatter("dd-MM-uuuu HH:mm");
    private static final DateTimeFormatter UI_DATE_TIME_OUTPUT =
            DateTimeFormatter.ofPattern(UI_DATE_TIME_PATTERN);

    private DateTimeConversionUtil() {
    }

    public static LocalDate parseLocalDate(String value) {
        if (value == null || value.isBlank()) return null;

        String text = value.trim();
        try {
            return LocalDate.parse(text, UI_DATE);
        } catch (DateTimeParseException ignored) {
            // Try the existing ISO API format next.
        }
        try {
            return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException exception) {
            throw invalidFormat(text, SUPPORTED_DATE_FORMATS, exception);
        }
    }

    public static LocalDateTime parseLocalDateTime(String value) {
        if (value == null || value.isBlank()) return null;

        String text = value.trim();
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // Try the UI date-time format next.
        }

        try {
            return LocalDateTime.parse(text, UI_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // Continue with legacy and ISO timestamp formats.
        }
        try {
            return LocalDateTime.parse(text, LEGACY_UI_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // Try timestamp formats that carry an offset or zone next.
        }
        try {
            return fromInstant(OffsetDateTime.parse(text).toInstant());
        } catch (DateTimeParseException ignored) {
            // Try a named-zone timestamp next.
        }
        try {
            return fromInstant(ZonedDateTime.parse(text).toInstant());
        } catch (DateTimeParseException ignored) {
            // Try an ISO instant next.
        }
        try {
            return fromInstant(Instant.parse(text));
        } catch (DateTimeParseException ignored) {
            // Legacy Mongo maps may expose java.util.Date.toString() values.
        }
        LocalDateTime legacyDate = parseLegacyDateToString(text);
        if (legacyDate != null) {
            return legacyDate;
        }
        try {
            // Date-only values use the current server-local time.
            LocalDate date = parseLocalDate(text);
            return date.atTime(LocalTime.now(ZoneId.systemDefault()));
        } catch (DateTimeParseException exception) {
            throw invalidFormat(text, SUPPORTED_DATE_TIME_FORMATS, exception);
        }
    }

    private static LocalDateTime parseLegacyDateToString(String value) {
        SimpleDateFormat formatter =
                new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
        formatter.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        Date parsed = formatter.parse(value, position);
        if (parsed == null || position.getIndex() != value.length()) {
            return null;
        }
        return fromInstant(parsed.toInstant());
    }

    public static String formatLocalDateTime(LocalDateTime value) {
        return value == null ? null : UI_DATE_TIME_OUTPUT.format(value);
    }

    public static String formatLocalDateTimeString(String value) {
        if (value == null || !value.contains("T")) return value;
        try {
            return formatLocalDateTime(parseLocalDateTime(value));
        } catch (DateTimeParseException ignored) {
            return value;
        }
    }

    public static LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate date) return date;
        if (value instanceof LocalDateTime dateTime) return dateTime.toLocalDate();
        if (value instanceof OffsetDateTime dateTime) {
            return fromInstant(dateTime.toInstant()).toLocalDate();
        }
        if (value instanceof ZonedDateTime dateTime) {
            return fromInstant(dateTime.toInstant()).toLocalDate();
        }
        if (value instanceof Instant instant) return fromInstant(instant).toLocalDate();
        if (value instanceof Date date) return fromInstant(date.toInstant()).toLocalDate();
        if (!(value instanceof String text) || text.isBlank()) return null;

        try {
            return parseLocalDate(text);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    public static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime dateTime) return dateTime;
        if (value instanceof LocalDate date) {
            return date.atTime(LocalTime.now(ZoneId.systemDefault()));
        }
        if (value instanceof OffsetDateTime dateTime) {
            return fromInstant(dateTime.toInstant());
        }
        if (value instanceof ZonedDateTime dateTime) {
            return fromInstant(dateTime.toInstant());
        }
        if (value instanceof Instant instant) return fromInstant(instant);
        if (value instanceof Date date) return fromInstant(date.toInstant());
        if (!(value instanceof String text) || text.isBlank()) return null;

        try {
            return parseLocalDateTime(text);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static DateTimeFormatter strictFormatter(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseSensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ROOT)
                .withResolverStyle(ResolverStyle.STRICT);
    }

    private static DateTimeParseException invalidFormat(
            String value,
            String expectedFormats,
            DateTimeParseException cause) {
        return new DateTimeParseException(
                "Invalid date value '" + value + "'. Expected " + expectedFormats,
                value,
                Math.max(0, cause.getErrorIndex()),
                cause);
    }

    private static LocalDateTime fromInstant(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
