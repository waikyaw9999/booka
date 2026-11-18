package com.booka.dto;

import com.booka.domain.enums.FolioLineType;
import com.booka.domain.enums.FolioStatus;
import com.booka.domain.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
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

    public record PostPaymentRequest(
            @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount,
            @NotNull PaymentMethod method,
            String reference
    ) {}
}
