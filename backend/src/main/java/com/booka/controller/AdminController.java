package com.booka.controller;

import com.booka.dto.BlackoutDtos.*;
import com.booka.dto.BookingDtos.*;
import com.booka.dto.FolioDtos.*;
import com.booka.dto.HotelDtos.*;
import com.booka.dto.OperationsDtos.*;
import com.booka.security.BookaUserDetails;
import com.booka.service.*;
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
    private final FrontOfficeService frontOfficeService;
    private final MaintenanceService maintenanceService;
    private final FolioService folioService;

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

    @GetMapping("/bookings/by-code/{code}")
    BookingResponse getBookingByCode(@PathVariable String code) {
        return bookingService.getBookingByConfirmationCode(code);
    }

    @PatchMapping("/bookings/{id}/status")
    BookingResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingStatusRequest request) {
        return bookingService.updateStatus(id, request.status());
    }

    @PostMapping("/bookings/{id}/check-in")
    BookingResponse checkIn(
            @PathVariable Long id,
            @RequestBody(required = false) CheckInRequest request) {
        boolean force = request != null && Boolean.TRUE.equals(request.force());
        return bookingService.checkIn(id, force);
    }

    @PostMapping("/bookings/{id}/check-out")
    BookingResponse checkOut(@PathVariable Long id) {
        return bookingService.checkOut(id);
    }

    @PostMapping("/bookings/{id}/assign-room")
    BookingResponse assignRoom(
            @PathVariable Long id,
            @Valid @RequestBody AssignRoomRequest request) {
        return bookingService.assignRoom(id, request.roomId());
    }

    @PostMapping("/reservations/walk-in")
    BookingResponse walkIn(@Valid @RequestBody WalkInReservationRequest request) {
        return bookingService.createWalkIn(request);
    }

    @GetMapping("/rooms")
    List<RoomBoardItem> roomBoard(@AuthenticationPrincipal BookaUserDetails user) {
        Long hotelId = user.getHotelId() != null ? user.getHotelId() : 1L;
        return frontOfficeService.getRoomBoard(hotelId);
    }

    @PatchMapping("/rooms/{id}/housekeeping")
    RoomBoardItem updateHousekeeping(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHousekeepingRequest request) {
        return frontOfficeService.updateHousekeeping(id, request);
    }

    @GetMapping("/maintenance-tickets")
    List<MaintenanceTicketResponse> listTickets(@AuthenticationPrincipal BookaUserDetails user) {
        Long hotelId = user.getHotelId() != null ? user.getHotelId() : 1L;
        return maintenanceService.list(hotelId);
    }

    @PostMapping("/maintenance-tickets")
    MaintenanceTicketResponse createTicket(
            @AuthenticationPrincipal BookaUserDetails user,
            @Valid @RequestBody CreateMaintenanceTicketRequest request) {
        Long hotelId = user.getHotelId() != null ? user.getHotelId() : request.hotelId();
        var scoped = new CreateMaintenanceTicketRequest(
                hotelId,
                request.roomId(),
                request.title(),
                request.description(),
                request.priority(),
                request.reportedBy(),
                request.markRoomOutOfOrder());
        return maintenanceService.create(scoped);
    }

    @PatchMapping("/maintenance-tickets/{id}")
    MaintenanceTicketResponse updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMaintenanceTicketRequest request) {
        return maintenanceService.update(id, request);
    }

    @GetMapping("/folios/{bookingId}")
    FolioResponse getFolio(@PathVariable Long bookingId) {
        return folioService.getFolioByBookingId(bookingId);
    }

    @PostMapping("/folios/{bookingId}/charges")
    FolioResponse postCharge(
            @PathVariable Long bookingId,
            @Valid @RequestBody PostChargeRequest request) {
        return folioService.postCharge(bookingId, request);
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
