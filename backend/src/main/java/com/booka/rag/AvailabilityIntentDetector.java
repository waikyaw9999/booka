package com.booka.rag;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AvailabilityIntentDetector {

    private static final String MONTH_NAME =
            "january|february|march|april|may|june|july|august|september|october|november|december|"
                    + "jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec";

    private static final Pattern ISO_RANGE = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2}).{0,20}?(\\d{4}-\\d{2}-\\d{2})",
            Pattern.CASE_INSENSITIVE);
    /** e.g. "December 20 to 22", "Dec 20-22", "December 20 to 22 2026" */
    private static final Pattern NATURAL_SAME_MONTH_RANGE = Pattern.compile(
            "\\b(" + MONTH_NAME + ")\\s+(\\d{1,2})(?:st|nd|rd|th)?"
                    + "\\s*(?:to|-|–|—|through|thru|until|till)\\s*"
                    + "(\\d{1,2})(?:st|nd|rd|th)?"
                    + "(?:\\s*,?\\s*(\\d{4}))?",
            Pattern.CASE_INSENSITIVE);
    /** e.g. "December 20 to January 2", "Dec 20 to Jan 2 2027" */
    private static final Pattern NATURAL_CROSS_MONTH_RANGE = Pattern.compile(
            "\\b(" + MONTH_NAME + ")\\s+(\\d{1,2})(?:st|nd|rd|th)?"
                    + "\\s*(?:to|-|–|—|through|thru|until|till)\\s*"
                    + "(" + MONTH_NAME + ")\\s+(\\d{1,2})(?:st|nd|rd|th)?"
                    + "(?:\\s*,?\\s*(\\d{4}))?",
            Pattern.CASE_INSENSITIVE);
    /** e.g. "20 to 22 December", "20-22 Dec 2026" */
    private static final Pattern NATURAL_DAY_FIRST_RANGE = Pattern.compile(
            "\\b(\\d{1,2})(?:st|nd|rd|th)?"
                    + "\\s*(?:to|-|–|—|through|thru|until|till)\\s*"
                    + "(\\d{1,2})(?:st|nd|rd|th)?"
                    + "\\s+(" + MONTH_NAME + ")"
                    + "(?:\\s*,?\\s*(\\d{4}))?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern GUESTS = Pattern.compile(
            "(\\d+)\\s*(?:guest|guests|people|adults|persons)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MONTH = Pattern.compile(
            "\\b(" + MONTH_NAME + ")\\b(?:\\s+(\\d{4}))?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ROOM_TYPE_HINT = Pattern.compile(
            "\\b(standard|deluxe|suite|twin|cabin|pavilion|heritage|classic|harbour|harbor|skyline|"
                    + "lake|garden|sandstone|sunrise|teak|balcony|double|family|king|queen)\\b"
                    + "(?:\\s+(?:room|rooms|suite|suites|cabin|cabins))?",
            Pattern.CASE_INSENSITIVE);

    public enum Intent {
        FAQ,
        AVAILABILITY,
        CLARIFY_DATES,
        ROOM_TYPES
    }

    public record ParsedAvailabilityQuery(
            Intent intent,
            LocalDate checkIn,
            LocalDate checkOut,
            int numGuests,
            boolean sampleDates,
            String sampleLabel,
            String roomTypeHint
    ) {
        public ParsedAvailabilityQuery(
                Intent intent,
                LocalDate checkIn,
                LocalDate checkOut,
                int numGuests,
                boolean sampleDates,
                String sampleLabel) {
            this(intent, checkIn, checkOut, numGuests, sampleDates, sampleLabel, null);
        }
    }

    public ParsedAvailabilityQuery detect(String message, String conversationText) {
        String messageOnly = message == null ? "" : message.trim();
        String text = (messageOnly + " " + (conversationText == null ? "" : conversationText)).trim();
        String lower = text.toLowerCase(Locale.ROOT);
        String messageLower = messageOnly.toLowerCase(Locale.ROOT);

        String roomTypeHint = parseRoomTypeHint(messageLower)
                .or(() -> parseRoomTypeHint(lower))
                .orElse(null);

        boolean roomTypeCatalog = isRoomTypeCatalogIntent(messageLower) || isRoomTypeCatalogIntent(lower);
        boolean dateSignal = hasDateSignal(messageLower) || hasDateSignal(lower);
        boolean availabilityIntent = isAvailabilityIntent(lower) || isAvailabilityIntent(messageLower);

        // Catalog questions about room types (no stay dates) — answer from inventory.
        // Prefer this over weak "what room…" availability matches inside "room types".
        if (roomTypeCatalog && !dateSignal) {
            return new ParsedAvailabilityQuery(
                    Intent.ROOM_TYPES, null, null, 2, false, null, roomTypeHint);
        }

        // Named room type without dates — list matching room types across properties.
        if (roomTypeHint != null && !availabilityIntent && !dateSignal) {
            return new ParsedAvailabilityQuery(
                    Intent.ROOM_TYPES, null, null, 2, false, null, roomTypeHint);
        }

        if (!availabilityIntent) {
            return new ParsedAvailabilityQuery(Intent.FAQ, null, null, 2, false, null, roomTypeHint);
        }

        int guests = parseGuests(text).orElse(2);

        Optional<LocalDate[]> iso = parseIsoRange(text);
        if (iso.isPresent()) {
            LocalDate in = iso.get()[0];
            LocalDate out = iso.get()[1];
            if (in.isBefore(out)) {
                return new ParsedAvailabilityQuery(
                        Intent.AVAILABILITY, in, out, guests, false, null, roomTypeHint);
            }
        }

        // Prefer exact natural ranges ("December 20 to 22") over month-only samples.
        Optional<LocalDate[]> natural = parseNaturalRange(messageOnly)
                .or(() -> parseNaturalRange(text));
        if (natural.isPresent()) {
            LocalDate in = natural.get()[0];
            LocalDate out = natural.get()[1];
            if (in.isBefore(out)) {
                return new ParsedAvailabilityQuery(
                        Intent.AVAILABILITY, in, out, guests, false, null, roomTypeHint);
            }
        }

        Optional<YearMonth> month = parseMonth(text);
        if (month.isPresent()) {
            YearMonth ym = month.get();
            LocalDate checkIn = ym.atDay(1);
            if (YearMonth.from(checkIn).equals(ym) && checkIn.isBefore(LocalDate.now())) {
                checkIn = LocalDate.now();
            }
            LocalDate checkOut = checkIn.plusDays(2);
            if (YearMonth.from(checkOut).isAfter(ym)) {
                checkOut = ym.atEndOfMonth().plusDays(1);
                if (!checkIn.isBefore(checkOut)) {
                    checkIn = ym.atDay(1);
                    checkOut = checkIn.plusDays(2);
                }
            }
            String label = ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
                    + " " + ym.getYear();
            return new ParsedAvailabilityQuery(
                    Intent.AVAILABILITY, checkIn, checkOut, guests, true, label, roomTypeHint);
        }

        // Availability asked with a room-type name but no dates yet.
        if (roomTypeHint != null) {
            return new ParsedAvailabilityQuery(
                    Intent.CLARIFY_DATES, null, null, guests, false, null, roomTypeHint);
        }

        return new ParsedAvailabilityQuery(Intent.CLARIFY_DATES, null, null, guests, false, null, null);
    }

    private boolean isAvailabilityIntent(String lower) {
        if (containsAny(lower,
                "available", "availability", "book", "booking", "reserve", "reservation",
                "room left", "rooms left", "vacancy", "vacant", "check availability",
                "price for", "rate for", "stay in", "stay from", "nights in",
                "rooms for", "room for")) {
            return true;
        }
        // Avoid matching "what room types" / "which room types".
        return Pattern.compile("\\b(what|which|any)\\s+rooms?\\b(?!\\s+types?\\b)", Pattern.CASE_INSENSITIVE)
                .matcher(lower)
                .find();
    }

    private boolean isRoomTypeCatalogIntent(String lower) {
        return containsAny(lower,
                "room type", "room types", "types of room", "kinds of room",
                "what rooms do", "which rooms do", "list of rooms", "rooms do you have",
                "rooms are there", "what rooms are", "describe room", "room categories",
                "room options", "accommodation types");
    }

    private boolean hasDateSignal(String lower) {
        return NATURAL_SAME_MONTH_RANGE.matcher(lower).find()
                || NATURAL_CROSS_MONTH_RANGE.matcher(lower).find()
                || NATURAL_DAY_FIRST_RANGE.matcher(lower).find()
                || MONTH.matcher(lower).find()
                || ISO_RANGE.matcher(lower).find();
    }

    private Optional<String> parseRoomTypeHint(String lower) {
        if (lower == null || lower.isBlank()) {
            return Optional.empty();
        }
        Matcher m = ROOM_TYPE_HINT.matcher(lower);
        if (m.find()) {
            return Optional.of(m.group(1).toLowerCase(Locale.ROOT));
        }
        return Optional.empty();
    }

    private boolean containsAny(String text, String... needles) {
        for (String n : needles) {
            if (text.contains(n)) return true;
        }
        return false;
    }

    private Optional<Integer> parseGuests(String text) {
        Matcher m = GUESTS.matcher(text);
        if (m.find()) {
            return Optional.of(Math.max(1, Integer.parseInt(m.group(1))));
        }
        return Optional.empty();
    }

    private Optional<LocalDate[]> parseIsoRange(String text) {
        Matcher m = ISO_RANGE.matcher(text);
        if (m.find()) {
            return Optional.of(new LocalDate[]{
                    LocalDate.parse(m.group(1), DateTimeFormatter.ISO_LOCAL_DATE),
                    LocalDate.parse(m.group(2), DateTimeFormatter.ISO_LOCAL_DATE)
            });
        }
        return Optional.empty();
    }

    private Optional<LocalDate[]> parseNaturalRange(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        Matcher cross = NATURAL_CROSS_MONTH_RANGE.matcher(text);
        if (cross.find()) {
            Month startMonth = Month.valueOf(normalizeMonth(cross.group(1)));
            int startDay = Integer.parseInt(cross.group(2));
            Month endMonth = Month.valueOf(normalizeMonth(cross.group(3)));
            int endDay = Integer.parseInt(cross.group(4));
            Integer yearHint = cross.group(5) != null ? Integer.parseInt(cross.group(5)) : null;
            return buildStay(startMonth, startDay, endMonth, endDay, yearHint);
        }

        Matcher same = NATURAL_SAME_MONTH_RANGE.matcher(text);
        if (same.find()) {
            Month month = Month.valueOf(normalizeMonth(same.group(1)));
            int startDay = Integer.parseInt(same.group(2));
            int endDay = Integer.parseInt(same.group(3));
            Integer yearHint = same.group(4) != null ? Integer.parseInt(same.group(4)) : null;
            return buildStay(month, startDay, month, endDay, yearHint);
        }

        Matcher dayFirst = NATURAL_DAY_FIRST_RANGE.matcher(text);
        if (dayFirst.find()) {
            int startDay = Integer.parseInt(dayFirst.group(1));
            int endDay = Integer.parseInt(dayFirst.group(2));
            Month month = Month.valueOf(normalizeMonth(dayFirst.group(3)));
            Integer yearHint = dayFirst.group(4) != null ? Integer.parseInt(dayFirst.group(4)) : null;
            return buildStay(month, startDay, month, endDay, yearHint);
        }

        return Optional.empty();
    }

    /**
     * Builds an exclusive check-out stay [checkIn, checkOut). Guest phrases like
     * "December 20 to 22" are treated as nights of the 20th and 21st → checkout on the 22nd.
     */
    private Optional<LocalDate[]> buildStay(
            Month startMonth, int startDay, Month endMonth, int endDay, Integer yearHint) {
        try {
            int startYear = resolveYear(startMonth, startDay, yearHint);
            LocalDate checkIn = LocalDate.of(startYear, startMonth, startDay);

            int endYear = yearHint != null
                    ? yearHint
                    : (endMonth.getValue() < startMonth.getValue() ? startYear + 1 : startYear);
            // If end month/day is before check-in in the same resolved year, roll end year.
            LocalDate tentativeEnd = LocalDate.of(endYear, endMonth, endDay);
            if (!tentativeEnd.isAfter(checkIn)) {
                tentativeEnd = LocalDate.of(endYear + 1, endMonth, endDay);
            }
            // "20 to 22" means checkout on the 22nd (exclusive end already matches guest wording).
            LocalDate checkOut = tentativeEnd;
            if (!checkIn.isBefore(checkOut)) {
                return Optional.empty();
            }
            return Optional.of(new LocalDate[]{checkIn, checkOut});
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private int resolveYear(Month month, int day, Integer yearHint) {
        if (yearHint != null) {
            return yearHint;
        }
        LocalDate today = LocalDate.now();
        LocalDate candidate;
        try {
            candidate = LocalDate.of(today.getYear(), month, day);
        } catch (Exception e) {
            candidate = YearMonth.of(today.getYear(), month).atEndOfMonth();
        }
        if (candidate.isBefore(today)) {
            return today.getYear() + 1;
        }
        return today.getYear();
    }

    private Optional<YearMonth> parseMonth(String text) {
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
