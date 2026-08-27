package com.booka.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;

public final class BlackoutDtos {

    private BlackoutDtos() {}

    public record BlackoutDateResponse(
            Long id,
            Long hotelId,
            Long roomTypeId,
            String roomTypeName,
            LocalDate startDate,
            LocalDate endDate,
            String reason,
            Instant createdAt
    ) {}

    public record CreateBlackoutDateRequest(
            @NotNull Long hotelId,
            Long roomTypeId,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotBlank String reason
    ) {}
}
