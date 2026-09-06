package com.booka.service;

import com.booka.config.StripeProperties;
import com.booka.domain.entity.Booking;
import com.booka.domain.enums.BookingStatus;
import com.booka.domain.enums.PaymentStatus;
import com.booka.dto.BookingDtos.CheckoutSessionResponse;
import com.booka.exception.BusinessException;
import com.booka.repository.BookingRepository;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Stripe Checkout + webhook fulfillment. Kept for later integration; the guest
 * booking UI currently skips Checkout and creates CONFIRMED + N_A bookings instead.
 * Re-enable by calling createCheckoutSession after createBooking when STRIPE_SECRET_KEY is set.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final StripeClient stripeClient;
    private final StripeProperties stripeProperties;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    public boolean isConfigured() {
        return StringUtils.hasText(stripeProperties.getSecretKey());
    }

    @Transactional
    public CheckoutSessionResponse createCheckoutSession(Long bookingId) {
        if (!isConfigured()) {
            throw new BusinessException("Stripe is not configured. Set STRIPE_SECRET_KEY to enable Checkout.");
        }
        Booking booking = bookingService.findBooking(bookingId);
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Booking is already paid");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Cannot pay for a cancelled booking");
        }

        long amountCents = booking.getTotalPrice()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        String integrationId = "booka_direct_" + randomSuffix(8);
        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(stripeProperties.getSuccessUrl() + "&bookingId=" + bookingId)
                .setCancelUrl(stripeProperties.getCancelUrl() + "&bookingId=" + bookingId)
                .setClientReferenceId(String.valueOf(bookingId))
                .putMetadata("bookingId", String.valueOf(bookingId))
                .putMetadata("confirmationCode", booking.getConfirmationCode())
                .putMetadata("integration_identifier", integrationId)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(stripeProperties.getCurrency())
                                .setUnitAmount(amountCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Booka stay " + booking.getConfirmationCode())
                                        .build())
                                .build())
                        .build());

        try {
            Session session = stripeClient.v1().checkout().sessions().create(builder.build());
            booking.setStripeCheckoutSessionId(session.getId());
            bookingRepository.save(booking);
            return new CheckoutSessionResponse(session.getId(), session.getUrl());
        } catch (StripeException ex) {
            log.error("Stripe Checkout Session create failed", ex);
            throw new BusinessException("Unable to start Stripe Checkout: " + ex.getMessage());
        }
    }

    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        if (!StringUtils.hasText(stripeProperties.getWebhookSecret())) {
            throw new BusinessException("STRIPE_WEBHOOK_SECRET is not configured");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeProperties.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            throw new BusinessException("Invalid Stripe webhook signature");
        }

        String type = event.getType();
        if (!"checkout.session.completed".equals(type)
                && !"checkout.session.async_payment_succeeded".equals(type)) {
            if ("checkout.session.async_payment_failed".equals(type)) {
                log.warn("Stripe async payment failed: {}", event.getId());
            }
            return;
        }

        Session session = deserializeSession(event);
        if (session == null) {
            log.warn("Could not deserialize Checkout Session from event {}", event.getId());
            return;
        }
        if ("unpaid".equalsIgnoreCase(session.getPaymentStatus())) {
            log.info("Ignoring unpaid checkout session {}", session.getId());
            return;
        }

        Long bookingId = null;
        if (session.getClientReferenceId() != null) {
            bookingId = Long.parseLong(session.getClientReferenceId());
        } else if (session.getMetadata() != null && session.getMetadata().get("bookingId") != null) {
            bookingId = Long.parseLong(session.getMetadata().get("bookingId"));
        }
        if (bookingId == null) {
            bookingRepository.findByStripeCheckoutSessionId(session.getId())
                    .ifPresent(b -> bookingService.markPaidAndConfirmed(b.getId(), session.getId()));
            return;
        }
        bookingService.markPaidAndConfirmed(bookingId, session.getId());
    }

    private Session deserializeSession(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject obj = deserializer.getObject().orElse(null);
        if (obj instanceof Session session) {
            return session;
        }
        try {
            StripeObject unsafe = deserializer.deserializeUnsafe();
            if (unsafe instanceof Session session) {
                return session;
            }
        } catch (Exception e) {
            log.warn("Failed to deserialize Checkout Session from event {}: {}", event.getId(), e.getMessage());
        }
        return null;
    }

    private static String randomSuffix(int len) {
        String s = UUID.randomUUID().toString().replace("-", "");
        return s.substring(0, Math.min(len, s.length()));
    }
}
