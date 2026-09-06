package com.booka.dto;

import com.booka.domain.enums.FolioLineType;
import com.booka.domain.enums.FolioStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class FolioDtos {

    private FolioDtos() {}

    public record FolioLineItemResponse(
            Long id,
            FolioLineType type,
            String description,
            BigDecimal amount,
            String externalRef,
            Instant postedAt
    ) {}

    public record FolioResponse(
            Long id,
            Long bookingId,
            Long hotelId,
            FolioStatus status,
            BigDecimal balance,
            BigDecimal chargesTotal,
            BigDecimal paymentsTotal,
            List<FolioLineItemResponse> lineItems
    ) {}

    public record PostChargeRequest(
            @NotNull FolioLineType type,
            @NotBlank String description,
            @NotNull BigDecimal amount,
            String externalRef
    ) {}
}
