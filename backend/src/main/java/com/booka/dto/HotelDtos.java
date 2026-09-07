package com.booka.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public final class HotelDtos {

    private HotelDtos() {}

    public record HotelResponse(
            Long id,
            String name,
            String description,
            String address,
            String city,
            String country,
            String amenities,
            String cancellationPolicy
    ) {}

    public record RoomTypeResponse(
            Long id,
            Long hotelId,
            String name,
            String description,
            Integer maxOccupancy,
            BigDecimal baseRate,
            String amenities
    ) {}

    public record RoomResponse(
            Long id,
            Long roomTypeId,
            String roomNumber,
            String status,
            Integer floor,
            String notes
    ) {}

    public record AvailabilityBody(
            java.time.LocalDate checkInDate,
            java.time.LocalDate checkOutDate,
            Integer numGuests
    ) {}

    public record AvailabilityRequest(
            Long hotelId,
            java.time.LocalDate checkInDate,
            java.time.LocalDate checkOutDate,
            Integer numGuests
    ) {}

    public record AvailableRoomTypeResponse(
            RoomTypeResponse roomType,
            int availableCount,
            BigDecimal totalPrice,
            BigDecimal nightlyRate,
            long nights
    ) {}

    public record CreateHotelRequest(
            @NotBlank String name,
            String description,
            @NotBlank String address,
            @NotBlank String city,
            @NotBlank String country,
            String amenities,
            String cancellationPolicy
    ) {}

    public record UpdateHotelRequest(
            String amenities,
            String cancellationPolicy,
            String description
    ) {}

    public record CreateRoomTypeRequest(
            @NotNull Long hotelId,
            @NotBlank String name,
            String description,
            @NotNull @Min(1) Integer maxOccupancy,
            @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal baseRate,
            String amenities
    ) {}

    public record UpdateRoomTypeRequest(
            String name,
            String description,
            @Min(1) Integer maxOccupancy,
            @DecimalMin(value = "0.01", inclusive = true) BigDecimal baseRate,
            String amenities
    ) {}

    public record CreateRoomRequest(
            @NotNull Long roomTypeId,
            @NotBlank String roomNumber,
            Integer floor,
            String notes
    ) {}

    public record DashboardStatsResponse(
            long totalRooms,
            long occupiedRooms,
            long availableRooms,
            long maintenanceRooms,
            long upcomingCheckIns,
            long upcomingCheckOuts,
            long activeBookings
    ) {}
}
