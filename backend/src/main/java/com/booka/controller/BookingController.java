package com.booka.controller;

import com.booka.dto.BookingDtos.*;
import com.booka.dto.FolioDtos.FolioResponse;
import com.booka.service.BookingService;
import com.booka.service.FolioService;
import com.booka.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final FolioService folioService;

    @PostMapping
    BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request) {
        return bookingService.createBooking(request);
    }

    @GetMapping("/{id}")
    BookingResponse getBooking(@PathVariable Long id) {
        return bookingService.getBooking(id);
    }

    @GetMapping("/{id}/folio")
    FolioResponse getFolio(@PathVariable Long id) {
        return folioService.getFolioByBookingId(id);
    }

    /** Unused by guest BookingPage for now (Stripe deferred). Keep for later Checkout redirect. */
    @PostMapping("/{id}/checkout-session")
    CheckoutSessionResponse createCheckoutSession(@PathVariable Long id) {
        return paymentService.createCheckoutSession(id);
    }
}
