package com.booka.controller;

import com.booka.dto.BlackoutDtos.*;
import com.booka.dto.BookingDtos.*;
import com.booka.dto.FolioDtos.*;
import com.booka.dto.HotelDtos.*;
import com.booka.dto.ImageDtos.*;
import com.booka.dto.OperationsDtos.*;
import com.booka.security.BookaUserDetails;
import com.booka.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final ImageService imageService;

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

    @PatchMapping("/hotels/{id}")
    HotelResponse updateHotel(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateHotelRequest request) {
        return hotelService.updateHotel(id, request, user.getHotelId());
    }

    @PostMapping("/room-types")
    RoomTypeResponse createRoomType(@Valid @RequestBody CreateRoomTypeRequest request) {
        return hotelService.createRoomType(request);
    }

    @PatchMapping("/room-types/{id}")
    RoomTypeResponse updateRoomType(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoomTypeRequest request) {
        return hotelService.updateRoomType(id, request, user.getHotelId());
    }

    @PostMapping("/rooms")
    RoomResponse createRoom(
            @AuthenticationPrincipal BookaUserDetails user,
            @Valid @RequestBody CreateRoomRequest request) {
        return hotelService.createRoom(request, user.getHotelId());
    }

    @GetMapping("/hotels/{id}/images")
    List<ImageResponse> listHotelImages(@PathVariable Long id) {
        return imageService.listHotelImages(id);
    }

    @PostMapping("/hotels/{id}/images")
    ImageResponse addHotelImage(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody AddImageRequest request) {
        return imageService.addHotelImage(id, request, user.getHotelId());
    }

    @PostMapping(value = "/hotels/{id}/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ImageResponse uploadHotelImage(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "primary", required = false) Boolean primary) {
        return imageService.uploadHotelImage(id, file, caption, primary, user.getHotelId());
    }

    @DeleteMapping("/hotels/{hotelId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteHotelImage(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long hotelId,
            @PathVariable Long imageId) {
        imageService.deleteHotelImage(hotelId, imageId, user.getHotelId());
    }

    @PostMapping("/hotels/{hotelId}/images/{imageId}/primary")
    ImageResponse setPrimaryHotelImage(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long hotelId,
            @PathVariable Long imageId) {
        return imageService.setPrimaryHotelImage(hotelId, imageId, user.getHotelId());
    }

    @GetMapping("/rooms/{id}/images")
    List<ImageResponse> listRoomImages(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id) {
        return imageService.listRoomImages(id, user.getHotelId());
    }

    @PostMapping("/rooms/{id}/images")
    ImageResponse addRoomImage(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody AddImageRequest request) {
        return imageService.addRoomImage(id, request, user.getHotelId());
    }

    @PostMapping(value = "/rooms/{id}/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ImageResponse uploadRoomImage(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "primary", required = false) Boolean primary) {
        return imageService.uploadRoomImage(id, file, caption, primary, user.getHotelId());
    }

    @DeleteMapping("/rooms/{roomId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteRoomImage(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long roomId,
            @PathVariable Long imageId) {
        imageService.deleteRoomImage(roomId, imageId, user.getHotelId());
    }

    @PostMapping("/rooms/{roomId}/images/{imageId}/primary")
    ImageResponse setPrimaryRoomImage(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long roomId,
            @PathVariable Long imageId) {
        return imageService.setPrimaryRoomImage(roomId, imageId, user.getHotelId());
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
