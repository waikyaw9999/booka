package com.booka.rag;

import com.booka.dto.HotelDtos.AvailabilityRequest;
import com.booka.dto.HotelDtos.AvailableRoomTypeResponse;
import com.booka.dto.HotelDtos.HotelResponse;
import com.booka.dto.HotelDtos.RoomTypeResponse;
import com.booka.dto.RagDtos.AvailabilityOption;
import com.booka.dto.RagDtos.RagChatResponse;
import com.booka.dto.RagDtos.RagCitation;
import com.booka.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AvailabilityToolService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    private final HotelService hotelService;
    private final GroqChatService groqChatService;

    public RagChatResponse clarifyDates(int numGuests) {
        return clarifyDates(numGuests, null);
    }

    public RagChatResponse clarifyDates(int numGuests, String roomTypeHint) {
        int guests = Math.max(1, numGuests);
        LocalDate checkIn = LocalDate.now().plusDays(7);
        LocalDate checkOut = checkIn.plusDays(2);
        LocalDate altIn = LocalDate.now().plusDays(14);
        LocalDate altOut = altIn.plusDays(2);

        String roomBit = roomTypeHint != null && !roomTypeHint.isBlank()
                ? " for " + capitalize(roomTypeHint) + " rooms"
                : "";
        String primary = availabilityQuestion(roomTypeHint, checkIn, checkOut, guests);
        String alternate = availabilityQuestion(roomTypeHint, altIn, altOut, guests);

        String answer = """
                I can check live room availability%s for you.
                Please share your check-in and check-out dates (YYYY-MM-DD), and number of guests.

                Or tap a date range below to search:
                """.formatted(roomBit).trim();

        return new RagChatResponse(
                answer,
                List.of(),
                groqChatService.isConfigured(),
                "CLARIFY_DATES",
                List.of(),
                null,
                List.of(),
                null,
                null,
                List.of(primary, alternate));
    }

    private static String availabilityQuestion(
            String roomTypeHint, LocalDate checkIn, LocalDate checkOut, int guests) {
        if (roomTypeHint != null && !roomTypeHint.isBlank()) {
            return "Available %s rooms from %s to %s for %d guests"
                    .formatted(capitalize(roomTypeHint), checkIn.format(ISO), checkOut.format(ISO), guests);
        }
        return "Available rooms from %s to %s for %d guests"
                .formatted(checkIn.format(ISO), checkOut.format(ISO), guests);
    }

    private static String capitalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public RagChatResponse listRoomTypes(Long hotelId, String roomTypeHint) {
        List<HotelResponse> hotels = hotelId != null
                ? List.of(hotelService.getHotel(hotelId))
                : hotelService.listHotels();

        StringBuilder facts = new StringBuilder();
        int matches = 0;

        for (HotelResponse hotel : hotels) {
            List<RoomTypeResponse> types = hotelService.getRoomTypes(hotel.id()).stream()
                    .filter(rt -> matchesRoomType(rt.name(), roomTypeHint))
                    .toList();
            if (types.isEmpty()) {
                continue;
            }
            facts.append("Hotel: ").append(hotel.name())
                    .append(" (").append(hotel.city()).append(")\n");
            for (RoomTypeResponse rt : types) {
                matches++;
                facts.append("- ").append(rt.name())
                        .append(": sleeps ").append(rt.maxOccupancy())
                        .append(", $").append(rt.baseRate()).append("/night");
                if (rt.description() != null && !rt.description().isBlank()) {
                    facts.append(" — ").append(rt.description());
                }
                if (rt.amenities() != null && !rt.amenities().isBlank()) {
                    facts.append(" [").append(rt.amenities()).append("]");
                }
                facts.append('\n');
            }
            facts.append('\n');
        }

        String prefix = roomTypeHint != null && !roomTypeHint.isBlank()
                ? "Room types matching \"" + roomTypeHint + "\":\n\n"
                : "Room types across " + (hotelId != null ? "this property" : "Booka properties") + ":\n\n";

        if (matches == 0) {
            String empty = roomTypeHint != null
                    ? "I could not find room types matching \"" + roomTypeHint + "\". Try another name like Standard, Deluxe, or Suite."
                    : "No room types are listed yet.";
            return new RagChatResponse(
                    empty,
                    List.of(new RagCitation("Room type catalog", "Inventory API", hotelId)),
                    groqChatService.isConfigured(),
                    "ROOM_TYPES",
                    List.of(),
                    null);
        }

        String system = """
                You are a hotel concierge for Booka.
                Summarize ONLY the room type catalog facts provided.
                Group by hotel. Mention name, occupancy, nightly rate, and a short description when present.
                Do not invent rooms or prices. Be concise and friendly.
                """;

        String generated = groqChatService.chat(system, List.of(
                java.util.Map.of("role", "user", "content", "Room type facts:\n" + facts)));

        String answer;
        if (generated != null && !generated.isBlank()) {
            answer = prefix + generated;
        } else {
            answer = prefix + facts;
        }

        return new RagChatResponse(
                answer.trim(),
                List.of(new RagCitation("Room type catalog", "Inventory API", hotelId)),
                groqChatService.isConfigured(),
                "ROOM_TYPES",
                List.of(),
                null);
    }

    public RagChatResponse checkAvailability(
            Long hotelId,
            LocalDate checkIn,
            LocalDate checkOut,
            int numGuests,
            boolean sampleDates,
            String sampleLabel) {
        return checkAvailability(hotelId, checkIn, checkOut, numGuests, sampleDates, sampleLabel, null);
    }

    public RagChatResponse checkAvailability(
            Long hotelId,
            LocalDate checkIn,
            LocalDate checkOut,
            int numGuests,
            boolean sampleDates,
            String sampleLabel,
            String roomTypeHint) {

        List<HotelResponse> hotels = hotelId != null
                ? List.of(hotelService.getHotel(hotelId))
                : hotelService.listHotels();

        List<AvailabilityOption> options = new ArrayList<>();
        StringBuilder facts = new StringBuilder();

        for (HotelResponse hotel : hotels) {
            List<AvailableRoomTypeResponse> available = hotelService.checkAvailability(
                            new AvailabilityRequest(hotel.id(), checkIn, checkOut, numGuests))
                    .stream()
                    .filter(item -> matchesRoomType(item.roomType().name(), roomTypeHint))
                    .toList();

            facts.append("Hotel: ").append(hotel.name())
                    .append(" (").append(hotel.city()).append(")\n");
            facts.append("Dates: ").append(checkIn).append(" to ").append(checkOut)
                    .append(" (").append(numGuests).append(" guests)\n");
            if (roomTypeHint != null && !roomTypeHint.isBlank()) {
                facts.append("Filter: room types matching \"").append(roomTypeHint).append("\"\n");
            }

            if (available.isEmpty()) {
                facts.append("No matching rooms available for these dates (may be booked out, blacked out, or filtered out).\n\n");
                continue;
            }

            for (AvailableRoomTypeResponse item : available) {
                options.add(new AvailabilityOption(
                        hotel.id(),
                        hotel.name(),
                        item.roomType().id(),
                        item.roomType().name(),
                        item.availableCount(),
                        item.totalPrice(),
                        item.nightlyRate(),
                        item.nights(),
                        checkIn.toString(),
                        checkOut.toString()));
                facts.append("- ").append(item.roomType().name())
                        .append(": ").append(item.availableCount()).append(" room(s), ")
                        .append(item.nights()).append(" night(s), $")
                        .append(item.nightlyRate()).append("/night, total $")
                        .append(item.totalPrice()).append('\n');
            }
            facts.append('\n');
        }

        String hotelPath = null;
        if (hotelId != null) {
            hotelPath = "/hotels/" + hotelId + "?checkIn=" + checkIn.format(ISO)
                    + "&checkOut=" + checkOut.format(ISO) + "&guests=" + numGuests
                    + "#availability";
        } else {
            Long soleHotelId = null;
            for (AvailabilityOption o : options) {
                if (soleHotelId == null) {
                    soleHotelId = o.hotelId();
                } else if (!soleHotelId.equals(o.hotelId())) {
                    soleHotelId = null;
                    break;
                }
            }
            if (soleHotelId != null) {
                hotelPath = "/hotels/" + soleHotelId + "?checkIn=" + checkIn.format(ISO)
                        + "&checkOut=" + checkOut.format(ISO) + "&guests=" + numGuests
                        + "#availability";
            }
        }

        String answer = buildAnswer(
                facts.toString(), options, sampleDates, sampleLabel, checkIn, checkOut, numGuests, roomTypeHint, hotelId == null);
        List<RagCitation> citations = List.of(new RagCitation(
                "Live hotel availability",
                "Inventory API",
                hotelId));

        List<String> suggested = List.of();
        if (sampleDates) {
            // Offer the exact sample range as a clickable follow-up question.
            suggested = List.of(availabilityQuestion(roomTypeHint, checkIn, checkOut, numGuests));
        }

        return new RagChatResponse(
                answer,
                citations,
                groqChatService.isConfigured(),
                "AVAILABILITY",
                options,
                hotelPath,
                List.of(),
                null,
                null,
                suggested);
    }

    static boolean matchesRoomType(String roomTypeName, String hint) {
        if (hint == null || hint.isBlank()) {
            return true;
        }
        if (roomTypeName == null || roomTypeName.isBlank()) {
            return false;
        }
        String name = roomTypeName.toLowerCase(Locale.ROOT);
        String h = hint.toLowerCase(Locale.ROOT).trim();
        return name.contains(h) || h.contains(name) || tokensOverlap(name, h);
    }

    private static boolean tokensOverlap(String name, String hint) {
        String[] hintTokens = hint.split("[\\s/-]+");
        for (String token : hintTokens) {
            if (token.length() < 3) continue;
            if (name.contains(token)) return true;
        }
        return false;
    }

    private String buildAnswer(
            String facts,
            List<AvailabilityOption> options,
            boolean sampleDates,
            String sampleLabel,
            LocalDate checkIn,
            LocalDate checkOut,
            int numGuests,
            String roomTypeHint,
            boolean allProperties) {

        String scope = allProperties ? "across all Booka properties" : "for this property";
        String filterBit = roomTypeHint != null && !roomTypeHint.isBlank()
                ? " matching \"" + roomTypeHint + "\""
                : "";

        String prefix = sampleDates
                ? "I checked live availability " + scope + filterBit + " for a sample stay in " + sampleLabel
                + " (" + checkIn + " → " + checkOut + ", " + numGuests + " guests). "
                + "Share exact dates if you need a precise quote.\n\n"
                : "Here is live availability " + scope + filterBit + " for " + checkIn + " → " + checkOut
                + " (" + numGuests + " guests):\n\n";

        String system = """
                You are a hotel concierge for Booka.
                Summarize ONLY the live availability facts provided.
                Cover every hotel that has results. Group by hotel when multiple properties appear.
                Do not invent rooms, prices, or dates.
                If none are available, say so and suggest trying different dates or room types.
                Be concise and friendly. End by inviting the guest to open a hotel page to book.
                """;

        String generated = groqChatService.chat(system, List.of(
                java.util.Map.of("role", "user", "content", "Availability facts:\n" + facts)));

        if (generated != null && !generated.isBlank()) {
            return prefix + generated;
        }

        if (options.isEmpty()) {
            return prefix + "No matching rooms are available for these dates. They may be fully booked, filtered out, or fall on a blackout period. Try different dates or room types.";
        }

        StringBuilder sb = new StringBuilder(prefix);
        String currentHotel = null;
        for (AvailabilityOption o : options) {
            if (!o.hotelName().equals(currentHotel)) {
                currentHotel = o.hotelName();
                sb.append(currentHotel).append('\n');
            }
            sb.append("• ").append(o.roomTypeName())
                    .append(": ").append(o.availableCount()).append(" left, $")
                    .append(o.totalPrice()).append(" total ($")
                    .append(o.nightlyRate()).append("/night)\n");
        }
        sb.append("\nOpen a hotel page to continue booking.");
        return sb.toString().trim();
    }
}
