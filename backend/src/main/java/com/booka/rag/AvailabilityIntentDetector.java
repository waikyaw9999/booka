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

    private static final Pattern ISO_RANGE = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2}).{0,20}?(\\d{4}-\\d{2}-\\d{2})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern GUESTS = Pattern.compile(
            "(\\d+)\\s*(?:guest|guests|people|adults|persons)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MONTH = Pattern.compile(
            "\\b(january|february|march|april|may|june|july|august|september|october|november|december|"
                    + "jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec)\\b(?:\\s+(\\d{4}))?",
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
        return MONTH.matcher(lower).find() || ISO_RANGE.matcher(lower).find();
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
