package com.booka.controller;

import com.booka.dto.BookingDtos.*;
import com.booka.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request) {
        return bookingService.createBooking(request);
    }

    @GetMapping("/{id}")
    BookingResponse getBooking(@PathVariable Long id) {
        return bookingService.getBooking(id);
    }
}
