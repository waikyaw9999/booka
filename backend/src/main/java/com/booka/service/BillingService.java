package com.booka.service;

import com.booka.domain.entity.Hotel;
import com.booka.domain.enums.SubscriptionPlan;
import com.booka.domain.enums.SubscriptionStatus;
import com.booka.dto.HotelDtos.HotelBillingResponse;
import com.booka.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Smallest sellable billing model: plan/status on the hotel.
 * Stripe Checkout for subscriptions is deferred — {@link #activateStarterStub} simulates payment.
 */
@Service
@RequiredArgsConstructor
public class BillingService {

    private final HotelRepository hotelRepository;
    private final HotelService hotelService;

    public HotelBillingResponse getBilling(Long staffHotelId) {
        Long hotelId = hotelService.requireStaffHotelId(staffHotelId);
        Hotel hotel = hotelService.findHotel(hotelId);
        return toResponse(hotel);
    }

    @Transactional
    public HotelBillingResponse activateStarterStub(Long staffHotelId) {
        Long hotelId = hotelService.requireStaffHotelId(staffHotelId);
        Hotel hotel = hotelService.findHotel(hotelId);
        Instant renews = Instant.now().plus(30, ChronoUnit.DAYS);
        hotel.setSubscriptionPlan(SubscriptionPlan.STARTER);
        hotel.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        hotel.setSubscriptionRenewsAt(renews);
        if (hotel.getTrialEndsAt() == null) {
            hotel.setTrialEndsAt(Instant.now());
        }
        return toResponse(hotelRepository.save(hotel));
    }

    private HotelBillingResponse toResponse(Hotel hotel) {
        String note = switch (hotel.getSubscriptionStatus()) {
            case TRIALING -> "Trial — activate Starter when ready (payment provider stub).";
            case ACTIVE -> "Subscription active. Stripe Billing can replace the stub later.";
            case PAST_DUE -> "Payment past due — guest bookings still work; renew soon.";
            case CANCELLED -> "Subscription cancelled — contact Booka to reactivate.";
        };
        return new HotelBillingResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getSubscriptionPlan().name(),
                hotel.getSubscriptionStatus().name(),
                hotel.getTrialEndsAt(),
                hotel.getSubscriptionRenewsAt(),
                note);
    }
}
