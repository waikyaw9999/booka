package com.booka.rag;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StaffPropertyIntentDetector {

    private static final Pattern MONTH = Pattern.compile(
            "\\b(january|february|march|april|may|june|july|august|september|october|november|december|"
                    + "jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec)\\b(?:\\s+(\\d{4}))?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ROOM_TYPE = Pattern.compile(
            "\\b(standard|deluxe|suite|executive|family|premium)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CONFIRM = Pattern.compile(
            "^(yes|y|yeah|yep|yup|ok|okay|sure|confirm|confirmed|please do|go ahead)\\b.*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PERIOD_IN_HISTORY = Pattern.compile(
            "check-ins in ([A-Za-z]+ \\d{4}) \\((\\d{4}-\\d{2}-\\d{2}) → (\\d{4}-\\d{2}-\\d{2})\\)",
            Pattern.CASE_INSENSITIVE);

    public enum Intent {
        SOP,
        BLACKOUT,
        SUMMARY,
        AVAILABILITY,
        BOOKINGS,
        CLARIFY_BOOKINGS
    }

    public record ParsedStaffQuery(
            Intent intent,
            YearMonth month,
            String roomTypeHint,
            String sampleLabel,
            boolean needsConfirmation,
            LocalDate from,
            LocalDate to
    ) {
        static ParsedStaffQuery of(Intent intent, YearMonth month, String roomTypeHint, String label) {
            LocalDate from = month == null ? null : month.atDay(1);
            LocalDate to = month == null ? null : month.plusMonths(1).atDay(1);
            return new ParsedStaffQuery(intent, month, roomTypeHint, label, false, from, to);
        }
    }

    public ParsedStaffQuery detect(String message, String conversationText) {
        String text = message == null ? "" : message.trim();
        String lower = text.toLowerCase(Locale.ROOT);
        String history = conversationText == null ? "" : conversationText;

        if (isAffirmative(text) && history.toLowerCase(Locale.ROOT).contains("reply yes to confirm")) {
            Optional<ParsedStaffQuery> confirmed = parseConfirmationFromHistory(history);
            if (confirmed.isPresent()) {
                return confirmed.get();
            }
        }

        if (isBlackoutIntent(lower)) {
            YearMonth month = parseRelativeOrNamedMonth(lower, text).orElse(null);
            String label = labelFor(month);
            return ParsedStaffQuery.of(Intent.BLACKOUT, month, parseRoomTypeHint(text), label);
        }

        if (isBookingsIntent(lower)) {
            Optional<YearMonth> monthOpt = parseRelativeOrNamedMonth(lower, text);
            if (monthOpt.isEmpty()) {
                return new ParsedStaffQuery(Intent.CLARIFY_BOOKINGS, null, null, null, false, null, null);
            }
            YearMonth month = monthOpt.get();
            String label = labelFor(month);
            boolean relative = lower.contains("next month") || lower.contains("this month") || lower.contains("last month");
            LocalDate from = month.atDay(1);
            LocalDate to = month.plusMonths(1).atDay(1);
            if (relative) {
                return new ParsedStaffQuery(Intent.CLARIFY_BOOKINGS, month, null, label, true, from, to);
            }
            return new ParsedStaffQuery(Intent.BOOKINGS, month, null, label, false, from, to);
        }

        if (isSummaryIntent(lower)) {
            return ParsedStaffQuery.of(Intent.SUMMARY, null, null, null);
        }

        if (isAvailabilityIntent(lower)) {
            return ParsedStaffQuery.of(Intent.AVAILABILITY, parseMonth(text).orElse(null), parseRoomTypeHint(text), null);
        }

        return ParsedStaffQuery.of(Intent.SOP, null, null, null);
    }

    /** Backwards-compatible overload used by some callers. */
    public ParsedStaffQuery detect(String message) {
        return detect(message, "");
    }

    private Optional<ParsedStaffQuery> parseConfirmationFromHistory(String history) {
        Matcher m = PERIOD_IN_HISTORY.matcher(history);
        String label = null;
        LocalDate from = null;
        LocalDate to = null;
        while (m.find()) {
            label = m.group(1);
            from = LocalDate.parse(m.group(2), DateTimeFormatter.ISO_LOCAL_DATE);
            to = LocalDate.parse(m.group(3), DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (from == null || to == null) {
            return Optional.empty();
        }
        YearMonth month = YearMonth.from(from);
        return Optional.of(new ParsedStaffQuery(Intent.BOOKINGS, month, null, label, false, from, to));
    }

    private boolean isAffirmative(String text) {
        return CONFIRM.matcher(text.trim()).matches();
    }

    private boolean isBlackoutIntent(String lower) {
        return containsAny(lower,
                "blackout", "black-out", "black out", "blocked date", "blocked dates",
                "closure", "closed dates", "renovation dates", "property closed");
    }

    private boolean isBookingsIntent(String lower) {
        return containsAny(lower,
                "how many booking", "how many reservation", "booking count", "reservation count",
                "bookings do we have", "reservations do we have", "bookings in ", "reservations in ",
                "bookings next", "bookings this", "bookings last", "bookings for ",
                "reservations next", "reservations this", "reservations for ",
                "number of booking", "number of reservation");
    }

    private boolean isSummaryIntent(String lower) {
        return containsAny(lower,
                "summary", "dashboard", "overview", "occupancy", "how many rooms",
                "check-in today", "checkins today", "check-outs today", "checkouts today",
                "active booking", "status of the hotel", "property status", "hotel status",
                "today's arrivals", "todays arrivals", "today's departures", "todays departures");
    }

    private boolean isAvailabilityIntent(String lower) {
        return containsAny(lower,
                "available", "availability", "vacancy", "vacant", "inventory",
                "rooms left", "room left", "which room", "open rooms");
    }

    private boolean containsAny(String text, String... needles) {
        for (String n : needles) {
            if (text.contains(n)) return true;
        }
        return false;
    }

    private String parseRoomTypeHint(String text) {
        Matcher m = ROOM_TYPE.matcher(text);
        return m.find() ? m.group(1).toLowerCase(Locale.ROOT) : null;
    }

    private Optional<YearMonth> parseRelativeOrNamedMonth(String lower, String original) {
        LocalDate today = LocalDate.now();
        if (lower.contains("next month")) {
            return Optional.of(YearMonth.from(today).plusMonths(1));
        }
        if (lower.contains("this month")) {
            return Optional.of(YearMonth.from(today));
        }
        if (lower.contains("last month")) {
            return Optional.of(YearMonth.from(today).minusMonths(1));
        }
        return parseMonth(original);
    }

    Optional<YearMonth> parseMonth(String text) {
        Matcher m = MONTH.matcher(text);
        if (!m.find()) return Optional.empty();
        Month month = Month.valueOf(normalizeMonth(m.group(1)));
        int year;
        if (m.group(2) != null) {
            year = Integer.parseInt(m.group(2));
        } else {
            YearMonth candidate = YearMonth.of(LocalDate.now().getYear(), month);
            if (candidate.atEndOfMonth().isBefore(LocalDate.now())) {
                candidate = candidate.plusYears(1);
            }
            year = candidate.getYear();
        }
        return Optional.of(YearMonth.of(year, month));
    }

    LocalDate[] monthRange(YearMonth ym) {
        return new LocalDate[]{ym.atDay(1), ym.plusMonths(1).atDay(1)};
    }

    private String labelFor(YearMonth month) {
        if (month == null) return null;
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear();
    }

    private String normalizeMonth(String raw) {
        String m = raw.toLowerCase(Locale.ROOT);
        return switch (m) {
            case "jan", "january" -> "JANUARY";
            case "feb", "february" -> "FEBRUARY";
            case "mar", "march" -> "MARCH";
            case "apr", "april" -> "APRIL";
            case "may" -> "MAY";
            case "jun", "june" -> "JUNE";
            case "jul", "july" -> "JULY";
            case "aug", "august" -> "AUGUST";
            case "sep", "sept", "september" -> "SEPTEMBER";
            case "oct", "october" -> "OCTOBER";
            case "nov", "november" -> "NOVEMBER";
            case "dec", "december" -> "DECEMBER";
            default -> m.toUpperCase(Locale.ROOT);
        };
    }
}
