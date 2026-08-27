package com.booka.controller;

import com.booka.dto.BlackoutDtos.BlackoutDateResponse;
import com.booka.dto.HotelDtos.*;
import com.booka.service.BlackoutDateService;
import com.booka.service.HotelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;
    private final BlackoutDateService blackoutDateService;

    @GetMapping
    List<HotelResponse> listHotels() {
        return hotelService.listHotels();
    }

    @GetMapping("/{id}")
    HotelResponse getHotel(@PathVariable Long id) {
        return hotelService.getHotel(id);
    }

    @GetMapping("/{id}/room-types")
    List<RoomTypeResponse> getRoomTypes(@PathVariable Long id) {
        return hotelService.getRoomTypes(id);
    }

    @PostMapping("/{id}/availability")
    List<AvailableRoomTypeResponse> checkAvailability(
            @PathVariable Long id,
            @Valid @RequestBody AvailabilityBody body) {
        var req = new AvailabilityRequest(id, body.checkInDate(), body.checkOutDate(), body.numGuests());
        return hotelService.checkAvailability(req);
    }

    @GetMapping("/{id}/blackout-dates")
    List<BlackoutDateResponse> listBlackoutDates(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from != null && to != null) {
            return blackoutDateService.listByHotelAndRange(id, from, to);
        }
        return blackoutDateService.listByHotel(id);
    }
}
