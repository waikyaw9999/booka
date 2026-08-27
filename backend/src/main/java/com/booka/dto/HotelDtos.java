package com.booka.dto;

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
            String status
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
            String name,
            String description,
            String address,
            String city,
            String country,
            String amenities,
            String cancellationPolicy
    ) {}

    public record CreateRoomTypeRequest(
            Long hotelId,
            String name,
            String description,
            Integer maxOccupancy,
            BigDecimal baseRate,
            String amenities
    ) {}

    public record CreateRoomRequest(
            Long roomTypeId,
            String roomNumber
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
