package com.booka.service;

import com.booka.domain.entity.Folio;
import com.booka.domain.entity.FolioLineItem;
import com.booka.domain.enums.FolioLineType;
import com.booka.domain.enums.FolioStatus;
import com.booka.dto.FolioDtos.*;
import com.booka.exception.BusinessException;
import com.booka.exception.ResourceNotFoundException;
import com.booka.repository.FolioLineItemRepository;
import com.booka.repository.FolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FolioService {

    public static final BigDecimal TAX_RATE = new BigDecimal("0.10");

    private final FolioRepository folioRepository;
    private final FolioLineItemRepository folioLineItemRepository;

    @Transactional
    public Folio openFolioWithRoomCharges(Long bookingId, Long hotelId, BigDecimal roomSubtotal, String roomDescription) {
        Folio folio = folioRepository.save(Folio.builder()
                .bookingId(bookingId)
                .hotelId(hotelId)
                .status(FolioStatus.OPEN)
                .build());

        postLine(folio.getId(), FolioLineType.ROOM, roomDescription, roomSubtotal, null);
        BigDecimal tax = roomSubtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        postLine(folio.getId(), FolioLineType.TAX, "Tax (10%)", tax, null);
        return folio;
    }

    @Transactional
    public FolioResponse postCharge(Long bookingId, PostChargeRequest request) {
        Folio folio = requireOpenFolio(bookingId);
        if (request.type() != FolioLineType.POS_FB && request.type() != FolioLineType.ADJUSTMENT) {
            throw new BusinessException("Only POS_FB or ADJUSTMENT charges can be posted manually");
        }
        if (request.externalRef() != null && !request.externalRef().isBlank()) {
            folioLineItemRepository.findByFolioIdAndExternalRef(folio.getId(), request.externalRef())
                    .ifPresent(existing -> {
                        throw new BusinessException("Charge with externalRef already posted: " + request.externalRef());
                    });
        }
        postLine(folio.getId(), request.type(), request.description(), request.amount(), request.externalRef());
        return getFolioByBookingId(bookingId);
    }

    @Transactional
    public void postPayment(Long bookingId, BigDecimal amount, String externalRef) {
        Folio folio = requireOpenFolio(bookingId);
        if (externalRef != null) {
            var existing = folioLineItemRepository.findByFolioIdAndExternalRef(folio.getId(), externalRef);
            if (existing.isPresent()) {
                return;
            }
        }
        // Payments reduce balance (negative amount convention for PAYMENT lines)
        BigDecimal paymentAmount = amount.abs().negate();
        postLine(folio.getId(), FolioLineType.PAYMENT, "Stripe payment", paymentAmount, externalRef);
    }

    @Transactional
    public java.util.Optional<FolioResponse> closeIfSettled(Long bookingId) {
        if (folioRepository.findByBookingId(bookingId).isEmpty()) {
            // Legacy bookings created before folios existed — skip billing close.
            return java.util.Optional.empty();
        }
        FolioResponse folio = getFolioByBookingId(bookingId);
        if (folio.balance().compareTo(BigDecimal.ZERO) == 0) {
            Folio entity = folioRepository.findByBookingId(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Folio not found"));
            entity.setStatus(FolioStatus.CLOSED);
            folioRepository.save(entity);
            return java.util.Optional.of(getFolioByBookingId(bookingId));
        }
        return java.util.Optional.of(folio);
    }

    public FolioResponse getFolioByBookingId(Long bookingId) {
        Folio folio = folioRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Folio not found for booking: " + bookingId));
        List<FolioLineItem> items = folioLineItemRepository.findByFolioIdOrderByPostedAtAsc(folio.getId());
        BigDecimal charges = BigDecimal.ZERO;
        BigDecimal payments = BigDecimal.ZERO;
        for (FolioLineItem item : items) {
            if (item.getType() == FolioLineType.PAYMENT) {
                payments = payments.add(item.getAmount().abs());
            } else {
                charges = charges.add(item.getAmount());
            }
        }
        BigDecimal balance = charges.subtract(payments);
        return new FolioResponse(
                folio.getId(),
                folio.getBookingId(),
                folio.getHotelId(),
                folio.getStatus(),
                balance,
                charges,
                payments,
                items.stream().map(this::toLine).toList());
    }

    private Folio requireOpenFolio(Long bookingId) {
        Folio folio = folioRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Folio not found for booking: " + bookingId));
        if (folio.getStatus() == FolioStatus.CLOSED) {
            throw new BusinessException("Folio is closed");
        }
        return folio;
    }

    private void postLine(Long folioId, FolioLineType type, String description, BigDecimal amount, String externalRef) {
        folioLineItemRepository.save(FolioLineItem.builder()
                .folioId(folioId)
                .type(type)
                .description(description)
                .amount(amount.setScale(2, RoundingMode.HALF_UP))
                .externalRef(externalRef)
                .postedAt(Instant.now())
                .build());
    }

    private FolioLineItemResponse toLine(FolioLineItem item) {
        return new FolioLineItemResponse(
                item.getId(), item.getType(), item.getDescription(),
                item.getAmount(), item.getExternalRef(), item.getPostedAt());
    }

    public static BigDecimal totalWithTax(BigDecimal roomSubtotal) {
        return roomSubtotal.add(roomSubtotal.multiply(TAX_RATE)).setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean isChargeType(FolioLineType type) {
        return EnumSet.of(FolioLineType.ROOM, FolioLineType.TAX, FolioLineType.POS_FB, FolioLineType.ADJUSTMENT)
                .contains(type);
    }
}
