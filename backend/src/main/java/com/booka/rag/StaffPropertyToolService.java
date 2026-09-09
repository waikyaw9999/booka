package com.booka.rag;

import com.booka.domain.entity.Booking;
import com.booka.domain.enums.BookingStatus;
import com.booka.dto.BlackoutDtos.BlackoutDateResponse;
import com.booka.dto.HotelDtos.DashboardStatsResponse;
import com.booka.dto.RagDtos.*;
import com.booka.repository.BookingRepository;
import com.booka.service.AdminService;
import com.booka.service.BlackoutDateService;
import com.booka.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StaffPropertyToolService {

    private final BlackoutDateService blackoutDateService;
    private final AdminService adminService;
    private final HotelService hotelService;
    private final BookingRepository bookingRepository;
    private final GroqChatService groqChatService;
    private final StaffPropertyIntentDetector intentDetector;

    public RagChatResponse clarifyBookingsPeriod(Long hotelId, YearMonth month, String sampleLabel, LocalDate from, LocalDate to) {
        if (month == null || from == null || to == null) {
            return new RagChatResponse(
                    "I can look up booking counts by check-in month. Try: \"How many bookings in September?\" or \"How many bookings next month?\"",
                    List.of(),
                    groqChatService.isConfigured(),
                    "CLARIFY_BOOKINGS",
                    List.of(),
                    null,
                    List.of(),
                    null,
                    null);
        }

        String hotelName = hotelService.getHotel(hotelId != null ? hotelId : 1L).name();
        // Range in the message must stay (from → exclusiveEnd) so confirmation can re-parse it.
        String answer = """
                I can check bookings for %s with check-ins in %s (%s → %s).

                Reply yes to confirm, or tell me a different month (e.g. "October %d").
                """.formatted(hotelName, sampleLabel, from, to, from.getYear()).trim();

        String confirmQuestion = "Yes — check-ins in %s (%s → %s)".formatted(sampleLabel, from, to);
        String namedMonthQuestion = "How many bookings in %s?".formatted(sampleLabel);

        return new RagChatResponse(
                answer,
                List.of(new RagCitation("Booking period confirmation", "Staff chat", hotelId)),
                groqChatService.isConfigured(),
                "CLARIFY_BOOKINGS",
                List.of(),
                null,
                List.of(),
                null,
                null,
                List.of(confirmQuestion, namedMonthQuestion));
    }

    public RagChatResponse answerBookings(Long hotelId, LocalDate from, LocalDate to, String sampleLabel) {
        Long resolvedHotelId = hotelId != null ? hotelId : 1L;
        String hotelName = hotelService.getHotel(resolvedHotelId).name();
        List<Booking> bookings = bookingRepository.findByHotelIdAndCheckInRange(resolvedHotelId, from, to);

        long cancelled = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();
        long confirmed = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long checkedIn = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CHECKED_IN).count();
        long checkedOut = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CHECKED_OUT).count();
        long pending = bookings.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count();
        long active = bookings.size() - cancelled;

        BookingPeriodSummary summary = new BookingPeriodSummary(
                sampleLabel,
                from.toString(),
                to.toString(),
                bookings.size(),
                active,
                cancelled,
                confirmed,
                checkedIn,
                checkedOut,
                pending);

        String facts = """
                Hotel: %s
                Period: %s (check-ins from %s to %s, exclusive end)
                Total bookings: %d
                Active (non-cancelled): %d
                Cancelled: %d
                Pending: %d
                Confirmed: %d
                Checked in: %d
                Checked out: %d
                """.formatted(
                hotelName, sampleLabel, from, to.minusDays(1),
                summary.totalBookings(), summary.activeBookings(), summary.cancelledBookings(),
                summary.pending(), summary.confirmed(), summary.checkedIn(), summary.checkedOut());

        String fallback = """
                Bookings for %s in %s (%s → %s):
                • Total: %d
                • Active (non-cancelled): %d
                • Pending: %d | Confirmed: %d | Checked in: %d | Checked out: %d | Cancelled: %d
                """.formatted(
                hotelName, sampleLabel, from, to.minusDays(1),
                summary.totalBookings(), summary.activeBookings(),
                summary.pending(), summary.confirmed(), summary.checkedIn(),
                summary.checkedOut(), summary.cancelledBookings()).trim();

        String answer = summarize(
                """
                        You are a hotel operations assistant.
                        Summarize ONLY the booking count facts provided.
                        Be concise. Do not invent bookings.
                        """,
                facts,
                fallback);

        return new RagChatResponse(
                answer,
                List.of(new RagCitation("Live booking records", "Bookings API", resolvedHotelId)),
                groqChatService.isConfigured(),
                "BOOKINGS",
                List.of(),
                null,
                List.of(),
                null,
                summary);
    }

    public RagChatResponse answerBlackouts(Long hotelId, YearMonth month, String roomTypeHint, String sampleLabel) {
        Long resolvedHotelId = hotelId != null ? hotelId : 1L;
        String hotelName = hotelService.getHotel(resolvedHotelId).name();

        List<BlackoutDateResponse> blackouts;
        if (month != null) {
            LocalDate[] range = intentDetector.monthRange(month);
            blackouts = blackoutDateService.listByHotelAndRange(resolvedHotelId, range[0], range[1]);
        } else {
            blackouts = blackoutDateService.listByHotel(resolvedHotelId);
        }

        if (roomTypeHint != null && !roomTypeHint.isBlank()) {
            String hint = roomTypeHint.toLowerCase(Locale.ROOT);
            blackouts = blackouts.stream()
                    .filter(b -> b.roomTypeId() == null || matchesRoomType(b, hint))
                    .toList();
        }

        List<BlackoutOption> options = blackouts.stream()
                .map(b -> new BlackoutOption(
                        b.id(),
                        b.hotelId(),
                        b.roomTypeId(),
                        b.roomTypeName() == null ? "Entire property" : b.roomTypeName(),
                        b.startDate().toString(),
                        b.endDate().toString(),
                        b.reason()))
                .toList();

        StringBuilder facts = new StringBuilder();
        facts.append("Hotel: ").append(hotelName).append('\n');
        if (sampleLabel != null) {
            facts.append("Period filter: ").append(sampleLabel).append('\n');
        }
        if (roomTypeHint != null) {
            facts.append("Room type filter: ").append(roomTypeHint).append('\n');
        }
        if (options.isEmpty()) {
            facts.append("No blackout dates match this query.\n");
        } else {
            for (BlackoutOption o : options) {
                facts.append("- ").append(o.startDate()).append(" → ").append(o.endDate())
                        .append(" | scope: ").append(o.roomTypeName())
                        .append(" | reason: ").append(o.reason()).append('\n');
            }
        }

        String answer = summarize(
                """
                        You are a hotel operations assistant.
                        Summarize ONLY the blackout date facts provided.
                        Be concise and actionable. Do not invent dates.
                        """,
                facts.toString(),
                options.isEmpty()
                        ? "No matching blackout dates found for this property"
                        + (sampleLabel != null ? " in " + sampleLabel : "") + "."
                        : formatBlackoutFallback(options, sampleLabel));

        return new RagChatResponse(
                answer,
                List.of(new RagCitation("Live property blackout dates", "Blackout API", resolvedHotelId)),
                groqChatService.isConfigured(),
                "BLACKOUT",
                List.of(),
                null,
                options,
                null);
    }

    public RagChatResponse answerSummary(Long hotelId) {
        Long resolvedHotelId = hotelId != null ? hotelId : 1L;
        String hotelName = hotelService.getHotel(resolvedHotelId).name();
        DashboardStatsResponse stats = adminService.getDashboardStats(resolvedHotelId);

        PropertySummary summary = new PropertySummary(
                stats.totalRooms(),
                stats.occupiedRooms(),
                stats.availableRooms(),
                stats.maintenanceRooms(),
                stats.upcomingCheckIns(),
                stats.upcomingCheckOuts(),
                stats.activeBookings());

        String facts = """
                Hotel: %s
                Total rooms: %d
                Occupied: %d
                Available: %d
                Maintenance: %d
                Check-ins today: %d
                Check-outs today: %d
                Active bookings: %d
                """.formatted(
                hotelName,
                summary.totalRooms(),
                summary.occupiedRooms(),
                summary.availableRooms(),
                summary.maintenanceRooms(),
                summary.upcomingCheckIns(),
                summary.upcomingCheckOuts(),
                summary.activeBookings());

        String fallback = """
                Property summary for %s:
                • Rooms: %d total (%d occupied, %d available, %d maintenance)
                • Today: %d check-ins, %d check-outs
                • Active bookings: %d
                """.formatted(
                hotelName,
                summary.totalRooms(), summary.occupiedRooms(), summary.availableRooms(), summary.maintenanceRooms(),
                summary.upcomingCheckIns(), summary.upcomingCheckOuts(),
                summary.activeBookings()).trim();

        String answer = summarize(
                """
                        You are a hotel operations assistant.
                        Give a brief property status briefing using ONLY the facts provided.
                        Highlight anything operational staff should watch (high occupancy, many arrivals/departures).
                        """,
                facts,
                fallback);

        return new RagChatResponse(
                answer,
                List.of(new RagCitation("Live property dashboard", "Admin stats API", resolvedHotelId)),
                groqChatService.isConfigured(),
                "SUMMARY",
                List.of(),
                null,
                List.of(),
                summary);
    }

    private boolean matchesRoomType(BlackoutDateResponse b, String hint) {
        if (b.roomTypeName() == null) return false;
        return b.roomTypeName().toLowerCase(Locale.ROOT).contains(hint);
    }

    private String formatBlackoutFallback(List<BlackoutOption> options, String sampleLabel) {
        StringBuilder sb = new StringBuilder("Blackout dates");
        if (sampleLabel != null) sb.append(" in ").append(sampleLabel);
        sb.append(":\n");
        for (BlackoutOption o : options) {
            sb.append("• ").append(o.startDate()).append(" → ").append(o.endDate())
                    .append(" (").append(o.roomTypeName()).append(") — ").append(o.reason()).append('\n');
        }
        return sb.toString().trim();
    }

    private String summarize(String systemPrompt, String facts, String fallback) {
        String generated = groqChatService.chat(systemPrompt, List.of(
                Map.of("role", "user", "content", "Facts:\n" + facts)));
        if (generated != null && !generated.isBlank()) {
            return generated;
        }
        return fallback;
    }
}
