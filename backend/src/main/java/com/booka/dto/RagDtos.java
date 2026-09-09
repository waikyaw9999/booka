package com.booka.dto;

import com.booka.domain.enums.RagAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class RagDtos {

    private RagDtos() {}

    public record RagChatRequest(
            @NotBlank @Size(max = 2000) String message,
            Long hotelId,
            List<RagChatMessage> history
    ) {}

    public record RagChatMessage(
            String role,
            String content
    ) {}

    public record RagCitation(
            String documentTitle,
            String section,
            Long hotelId
    ) {}

    public record AvailabilityOption(
            Long hotelId,
            String hotelName,
            Long roomTypeId,
            String roomTypeName,
            int availableCount,
            BigDecimal totalPrice,
            BigDecimal nightlyRate,
            long nights,
            String checkInDate,
            String checkOutDate
    ) {}

    public record BlackoutOption(
            Long id,
            Long hotelId,
            Long roomTypeId,
            String roomTypeName,
            String startDate,
            String endDate,
            String reason
    ) {}

    public record PropertySummary(
            long totalRooms,
            long occupiedRooms,
            long availableRooms,
            long maintenanceRooms,
            long upcomingCheckIns,
            long upcomingCheckOuts,
            long activeBookings
    ) {}

    public record BookingPeriodSummary(
            String periodLabel,
            String fromDate,
            String toDate,
            long totalBookings,
            long activeBookings,
            long cancelledBookings,
            long confirmed,
            long checkedIn,
            long checkedOut,
            long pending
    ) {}

    public record RagChatResponse(
            String answer,
            List<RagCitation> citations,
            boolean ragAvailable,
            String intent,
            List<AvailabilityOption> availability,
            String hotelPath,
            List<BlackoutOption> blackouts,
            PropertySummary propertySummary,
            BookingPeriodSummary bookingSummary,
            List<String> suggestedQuestions
    ) {
        public RagChatResponse(String answer, List<RagCitation> citations, boolean ragAvailable) {
            this(answer, citations, ragAvailable, "FAQ", List.of(), null, List.of(), null, null, List.of());
        }

        public RagChatResponse(
                String answer,
                List<RagCitation> citations,
                boolean ragAvailable,
                String intent,
                List<AvailabilityOption> availability,
                String hotelPath) {
            this(answer, citations, ragAvailable, intent, availability, hotelPath, List.of(), null, null, List.of());
        }

        public RagChatResponse(
                String answer,
                List<RagCitation> citations,
                boolean ragAvailable,
                String intent,
                List<AvailabilityOption> availability,
                String hotelPath,
                List<BlackoutOption> blackouts,
                PropertySummary propertySummary) {
            this(answer, citations, ragAvailable, intent, availability, hotelPath, blackouts, propertySummary, null, List.of());
        }

        public RagChatResponse(
                String answer,
                List<RagCitation> citations,
                boolean ragAvailable,
                String intent,
                List<AvailabilityOption> availability,
                String hotelPath,
                List<BlackoutOption> blackouts,
                PropertySummary propertySummary,
                BookingPeriodSummary bookingSummary) {
            this(answer, citations, ragAvailable, intent, availability, hotelPath, blackouts, propertySummary,
                    bookingSummary, List.of());
        }
    }

    public record IngestDocumentRequest(
            @NotBlank String title,
            @NotBlank String content,
            RagAudience audience,
            Long hotelId,
            String sourceType
    ) {}

    public record RagDocumentResponse(
            Long id,
            String title,
            RagAudience audience,
            Long hotelId,
            String sourceType
    ) {}
}
