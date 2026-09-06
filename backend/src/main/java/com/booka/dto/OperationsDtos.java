package com.booka.dto;

import com.booka.domain.enums.HousekeepingStatus;
import com.booka.domain.enums.RoomStatus;
import com.booka.domain.enums.TicketPriority;
import com.booka.domain.enums.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public final class OperationsDtos {

    private OperationsDtos() {}

    public record RoomBoardItem(
            Long id,
            Long roomTypeId,
            String roomTypeName,
            String roomNumber,
            Integer floor,
            RoomStatus status,
            HousekeepingStatus housekeepingStatus,
            String notes,
            Long currentBookingId,
            String currentGuestName,
            String confirmationCode
    ) {}

    public record UpdateHousekeepingRequest(
            @NotNull HousekeepingStatus housekeepingStatus,
            Boolean force
    ) {}

    public record CreateMaintenanceTicketRequest(
            @NotNull Long hotelId,
            @NotNull Long roomId,
            @NotBlank String title,
            String description,
            @NotNull TicketPriority priority,
            String reportedBy,
            Boolean markRoomOutOfOrder
    ) {}

    public record UpdateMaintenanceTicketRequest(
            TicketStatus status,
            TicketPriority priority,
            String description
    ) {}

    public record MaintenanceTicketResponse(
            Long id,
            Long hotelId,
            Long roomId,
            String roomNumber,
            String title,
            String description,
            TicketPriority priority,
            TicketStatus status,
            String reportedBy,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
