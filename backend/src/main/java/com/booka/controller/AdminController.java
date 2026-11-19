package com.booka.controller;

import com.booka.dto.AuthDtos.CreateStaffRequest;
import com.booka.dto.AuthDtos.StaffMemberResponse;
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
    private final StaffUserService staffUserService;

    @GetMapping("/dashboard")
    DashboardStatsResponse dashboard(@AuthenticationPrincipal BookaUserDetails user) {
        return adminService.getDashboardStats(hotelId(user));
    }

    @GetMapping("/bookings")
    PageResponse<BookingResponse> listBookings(
            @AuthenticationPrincipal BookaUserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return bookingService.listBookings(hotelId(user), page, size);
    }

    @GetMapping("/bookings/by-code/{code}")
    BookingResponse getBookingByCode(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable String code) {
        return bookingService.getBookingByConfirmationCode(code, hotelId(user));
    }

    @PatchMapping("/bookings/{id}/status")
    BookingResponse updateStatus(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingStatusRequest request) {
        return bookingService.updateStatus(id, request.status(), hotelId(user));
    }

    @PostMapping("/bookings/{id}/check-in")
    BookingResponse checkIn(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id,
            @RequestBody(required = false) CheckInRequest request) {
        boolean force = request != null && Boolean.TRUE.equals(request.force());
        return bookingService.checkIn(id, force, hotelId(user));
    }

    @PostMapping("/bookings/{id}/check-out")
    BookingResponse checkOut(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id) {
        return bookingService.checkOut(id, hotelId(user));
    }

    @PostMapping("/bookings/{id}/assign-room")
    BookingResponse assignRoom(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody AssignRoomRequest request) {
        return bookingService.assignRoom(id, request.roomId(), hotelId(user));
    }

    @PostMapping("/reservations/walk-in")
    BookingResponse walkIn(
            @AuthenticationPrincipal BookaUserDetails user,
            @Valid @RequestBody WalkInReservationRequest request) {
        return bookingService.createWalkIn(request, hotelId(user));
    }

    @GetMapping("/rooms")
    List<RoomBoardItem> roomBoard(@AuthenticationPrincipal BookaUserDetails user) {
        return frontOfficeService.getRoomBoard(hotelId(user));
    }

    @PatchMapping("/rooms/{id}/housekeeping")
    RoomBoardItem updateHousekeeping(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateHousekeepingRequest request) {
        return frontOfficeService.updateHousekeeping(id, request, hotelId(user));
    }

    @GetMapping("/maintenance-tickets")
    List<MaintenanceTicketResponse> listTickets(@AuthenticationPrincipal BookaUserDetails user) {
        return maintenanceService.list(hotelId(user));
    }

    @PostMapping("/maintenance-tickets")
    MaintenanceTicketResponse createTicket(
            @AuthenticationPrincipal BookaUserDetails user,
            @Valid @RequestBody CreateMaintenanceTicketRequest request) {
        Long hotelId = hotelId(user);
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
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateMaintenanceTicketRequest request) {
        return maintenanceService.update(id, request, hotelId(user));
    }

    @GetMapping("/folios/{bookingId}")
    FolioResponse getFolio(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long bookingId) {
        return folioService.getFolioByBookingId(bookingId, hotelId(user));
    }

    @PostMapping("/folios/{bookingId}/charges")
    FolioResponse postCharge(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long bookingId,
            @Valid @RequestBody PostChargeRequest request) {
        return folioService.postCharge(bookingId, request, hotelId(user));
    }

    @PatchMapping("/hotels/{id}")
    HotelResponse updateHotel(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateHotelRequest request) {
        return hotelService.updateHotel(id, request, hotelId(user));
    }

    @PostMapping("/room-types")
    RoomTypeResponse createRoomType(
            @AuthenticationPrincipal BookaUserDetails user,
            @Valid @RequestBody CreateRoomTypeRequest request) {
        return hotelService.createRoomType(request, hotelId(user));
    }

    @PatchMapping("/room-types/{id}")
    RoomTypeResponse updateRoomType(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoomTypeRequest request) {
        return hotelService.updateRoomType(id, request, hotelId(user));
    }

    @PostMapping("/rooms")
    RoomResponse createRoom(
            @AuthenticationPrincipal BookaUserDetails user,
            @Valid @RequestBody CreateRoomRequest request) {
        return hotelService.createRoom(request, hotelId(user));
    }

    @GetMapping("/hotels/{id}/images")
    List<ImageResponse> listHotelImages(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id) {
        hotelService.assertHotelAccess(id, hotelId(user));
        return imageService.listHotelImages(id);
    }

    @PostMapping("/hotels/{id}/images")
    ImageResponse addHotelImage(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody AddImageRequest request) {
        return imageService.addHotelImage(id, request, hotelId(user));
    }

    @PostMapping(value = "/hotels/{id}/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ImageResponse uploadHotelImage(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "primary", required = false) Boolean primary) {
        return imageService.uploadHotelImage(id, file, caption, primary, hotelId(user));
    }

    @DeleteMapping("/hotels/{hotelId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteHotelImage(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long hotelId,
            @PathVariable Long imageId) {
        imageService.deleteHotelImage(hotelId, imageId, hotelId(user));
    }

    @PostMapping("/hotels/{hotelId}/images/{imageId}/primary")
    ImageResponse setPrimaryHotelImage(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long hotelId,
            @PathVariable Long imageId) {
        return imageService.setPrimaryHotelImage(hotelId, imageId, hotelId(user));
    }

    @GetMapping("/rooms/{id}/images")
    List<ImageResponse> listRoomImages(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id) {
        return imageService.listRoomImages(id, hotelId(user));
    }

    @PostMapping("/rooms/{id}/images")
    ImageResponse addRoomImage(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody AddImageRequest request) {
        return imageService.addRoomImage(id, request, hotelId(user));
    }

    @PostMapping(value = "/rooms/{id}/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ImageResponse uploadRoomImage(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "primary", required = false) Boolean primary) {
        return imageService.uploadRoomImage(id, file, caption, primary, hotelId(user));
    }

    @DeleteMapping("/rooms/{roomId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteRoomImage(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long roomId,
            @PathVariable Long imageId) {
        imageService.deleteRoomImage(roomId, imageId, hotelId(user));
    }

    @PostMapping("/rooms/{roomId}/images/{imageId}/primary")
    ImageResponse setPrimaryRoomImage(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long roomId,
            @PathVariable Long imageId) {
        return imageService.setPrimaryRoomImage(roomId, imageId, hotelId(user));
    }

    @GetMapping("/blackout-dates")
    List<BlackoutDateResponse> listBlackoutDates(@AuthenticationPrincipal BookaUserDetails user) {
        return blackoutDateService.listByHotel(hotelId(user));
    }

    @PostMapping("/blackout-dates")
    BlackoutDateResponse createBlackoutDate(
            @AuthenticationPrincipal BookaUserDetails user,
            @Valid @RequestBody CreateBlackoutDateRequest request) {
        Long hotelId = hotelId(user);
        var scoped = new CreateBlackoutDateRequest(
                hotelId, request.roomTypeId(), request.startDate(), request.endDate(), request.reason());
        return blackoutDateService.create(scoped);
    }

    @DeleteMapping("/blackout-dates/{id}")
    void deleteBlackoutDate(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id) {
        blackoutDateService.delete(id, hotelId(user));
    }

    @GetMapping("/staff")
    List<StaffMemberResponse> listStaff(@AuthenticationPrincipal BookaUserDetails user) {
        return staffUserService.listStaff(user);
    }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    StaffMemberResponse inviteStaff(
            @AuthenticationPrincipal BookaUserDetails user,
            @Valid @RequestBody CreateStaffRequest request) {
        return staffUserService.inviteStaff(user, request);
    }

    private Long hotelId(BookaUserDetails user) {
        return hotelService.requireStaffHotelId(user.getHotelId());
    }
}
