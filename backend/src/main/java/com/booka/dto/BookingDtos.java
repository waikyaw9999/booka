package com.booka.dto;

import com.booka.domain.enums.BookingStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class BookingDtos {

    private BookingDtos() {}

    public record CreateBookingRequest(
            @NotNull Long hotelId,
            @NotNull Long roomTypeId,
            @NotNull @FutureOrPresent LocalDate checkInDate,
            @NotNull LocalDate checkOutDate,
            @NotNull @Min(1) Integer numGuests,
            @NotBlank @Email String guestEmail,
            @NotBlank String guestFullName,
            String guestPhone
    ) {}

    public record BookingResponse(
            Long id,
            Long hotelId,
            String hotelName,
            Long roomId,
            String roomNumber,
            String roomTypeName,
            Long guestId,
            String guestName,
            String guestEmail,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer numGuests,
            BigDecimal totalPrice,
            BookingStatus status,
            Instant createdAt
    ) {}

    public record UpdateBookingStatusRequest(
            @NotNull BookingStatus status
    ) {}

    public record PageResponse<T>(
            java.util.List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}
