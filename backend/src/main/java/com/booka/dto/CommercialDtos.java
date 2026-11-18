package com.booka.dto;

import com.booka.domain.enums.GuestRequestCategory;
import com.booka.domain.enums.GuestRequestStatus;
import com.booka.domain.enums.VipTier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class CommercialDtos {

    private CommercialDtos() {}

    public record GuestSummary(
            Long id,
            String fullName,
            String email,
            String phone,
            String nationality,
            VipTier vipTier,
            boolean blacklisted,
            int stays,
            BigDecimal lifetimeRevenue
    ) {}

    public record GuestStay(
            Long bookingId,
            String confirmationCode,
            String checkInDate,
            String checkOutDate,
            String status,
            String roomTypeName,
            String channel,
            BigDecimal totalPrice
    ) {}

    public record GuestProfile(
            Long id,
            String fullName,
            String email,
            String phone,
            String nationality,
            VipTier vipTier,
            String notes,
            boolean blacklisted,
            int stays,
            int nights,
            BigDecimal lifetimeRevenue,
            List<GuestStay> history
    ) {}

    public record UpdateGuestProfileRequest(
            @Size(max = 80) String nationality,
            @NotNull VipTier vipTier,
            @Size(max = 2000) String notes,
            @NotNull Boolean blacklisted
    ) {}

    public record AuditStay(
            Long bookingId,
            String confirmationCode,
            String guestName,
            String roomNumber,
            String checkInDate,
            String checkOutDate,
            String status
    ) {}

    public record NightAuditRecord(
            Long id,
            String businessDate,
            BigDecimal occupancyPercent,
            BigDecimal adr,
            BigDecimal revpar,
            BigDecimal roomRevenue,
            BigDecimal payments,
            BigDecimal openBalance,
            int roomsSold,
            int inHouse,
            int arrivals,
            int departures,
            int noShows,
            String note,
            Instant closedAt
    ) {}

    public record NightAuditPreview(
            String businessDate,
            boolean alreadyClosed,
            BigDecimal occupancyPercent,
            BigDecimal adr,
            BigDecimal revpar,
            BigDecimal roomRevenue,
            BigDecimal payments,
            BigDecimal openBalance,
            int roomsSold,
            int sellableRooms,
            int inHouse,
            int arrivals,
            int dueOuts,
            int noShowCandidates,
            List<AuditStay> dueOutList,
            List<AuditStay> noShowList,
            List<HotelDtos.ChannelRevenue> channels,
            List<NightAuditRecord> history
    ) {}

    public record CloseNightRequest(
            @Size(max = 500) String note,
            Boolean retainNoShowNight
    ) {}

    public record GuestRequestView(
            Long id,
            Long bookingId,
            String confirmationCode,
            String guestName,
            GuestRequestCategory category,
            String note,
            GuestRequestStatus status,
            Instant createdAt
    ) {}

    public record CreateGuestRequestBody(
            @NotNull Long bookingId,
            @NotNull GuestRequestCategory category,
            @NotBlank @Size(max = 500) String note
    ) {}

    public record UpdateGuestRequestBody(
            @NotNull GuestRequestStatus status
    ) {}
}
