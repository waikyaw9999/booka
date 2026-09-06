package com.booka.dto;

import com.booka.domain.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
            String confirmationCode,
            Long hotelId,
            String hotelName,
            Long roomId,
            String roomNumber,
            Long roomTypeId,
            String roomTypeName,
            Long guestId,
            String guestName,
            String guestEmail,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer numGuests,
            BigDecimal totalPrice,
            BookingStatus status,
            ReservationChannel channel,
            PaymentStatus paymentStatus,
            String externalReservationId,
            Instant createdAt
    ) {}

    public record UpdateBookingStatusRequest(
            @NotNull BookingStatus status
    ) {}

    public record AssignRoomRequest(
            @NotNull Long roomId
    ) {}

    public record CheckInRequest(
            Boolean force
    ) {}

    public record WalkInReservationRequest(
            @NotNull Long hotelId,
            @NotNull Long roomTypeId,
            Long roomId,
            @NotNull LocalDate checkInDate,
            @NotNull LocalDate checkOutDate,
            @NotNull @Min(1) Integer numGuests,
            @NotBlank @Email String guestEmail,
            @NotBlank String guestFullName,
            String guestPhone
    ) {}

    public record ChannelReservationRequest(
            @NotNull Long hotelId,
            @NotNull Long roomTypeId,
            Long roomId,
            @NotBlank String externalReservationId,
            @NotNull LocalDate checkInDate,
            @NotNull LocalDate checkOutDate,
            @NotNull @Min(1) Integer numGuests,
            @NotBlank @Email String guestEmail,
            @NotBlank String guestFullName,
            String guestPhone,
            BigDecimal totalPrice,
            String status
    ) {}

    public record CheckoutSessionResponse(
            String sessionId,
            String url
    ) {}

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}
