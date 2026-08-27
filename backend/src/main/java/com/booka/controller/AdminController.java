package com.booka.controller;

import com.booka.domain.enums.BookingStatus;
import com.booka.dto.BlackoutDtos.*;
import com.booka.dto.BookingDtos.*;
import com.booka.dto.HotelDtos.*;
import com.booka.security.BookaUserDetails;
import com.booka.service.AdminService;
import com.booka.service.BlackoutDateService;
import com.booka.service.BookingService;
import com.booka.service.HotelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final BookingService bookingService;
    private final HotelService hotelService;
    private final BlackoutDateService blackoutDateService;

    @GetMapping("/dashboard")
    DashboardStatsResponse dashboard(@AuthenticationPrincipal BookaUserDetails user) {
        Long hotelId = user.getHotelId() != null ? user.getHotelId() : 1L;
        return adminService.getDashboardStats(hotelId);
    }

    @GetMapping("/bookings")
    PageResponse<BookingResponse> listBookings(
            @AuthenticationPrincipal BookaUserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long hotelId = user.getHotelId();
        return bookingService.listBookings(hotelId, page, size);
    }

    @PatchMapping("/bookings/{id}/status")
    BookingResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingStatusRequest request) {
        return bookingService.updateStatus(id, request.status());
    }

    @PostMapping("/hotels")
    HotelResponse createHotel(@Valid @RequestBody CreateHotelRequest request) {
        return hotelService.createHotel(request);
    }

    @PostMapping("/room-types")
    RoomTypeResponse createRoomType(@Valid @RequestBody CreateRoomTypeRequest request) {
        return hotelService.createRoomType(request);
    }

    @PostMapping("/rooms")
    RoomResponse createRoom(@Valid @RequestBody CreateRoomRequest request) {
        return hotelService.createRoom(request);
    }

    @GetMapping("/blackout-dates")
    List<BlackoutDateResponse> listBlackoutDates(@AuthenticationPrincipal BookaUserDetails user) {
        Long hotelId = user.getHotelId() != null ? user.getHotelId() : 1L;
        return blackoutDateService.listByHotel(hotelId);
    }

    @PostMapping("/blackout-dates")
    BlackoutDateResponse createBlackoutDate(
            @AuthenticationPrincipal BookaUserDetails user,
            @Valid @RequestBody CreateBlackoutDateRequest request) {
        Long hotelId = user.getHotelId() != null ? user.getHotelId() : request.hotelId();
        var scoped = new CreateBlackoutDateRequest(
                hotelId, request.roomTypeId(), request.startDate(), request.endDate(), request.reason());
        return blackoutDateService.create(scoped);
    }

    @DeleteMapping("/blackout-dates/{id}")
    void deleteBlackoutDate(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id) {
        blackoutDateService.delete(id, user.getHotelId());
    }
}
