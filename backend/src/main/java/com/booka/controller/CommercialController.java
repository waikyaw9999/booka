package com.booka.controller;

import com.booka.dto.CommercialDtos.*;
import com.booka.dto.FolioDtos.FolioResponse;
import com.booka.dto.FolioDtos.PostPaymentRequest;
import com.booka.exception.BusinessException;
import com.booka.security.BookaUserDetails;
import com.booka.service.BookingService;
import com.booka.service.FolioService;
import com.booka.service.GuestProfileService;
import com.booka.service.GuestRequestService;
import com.booka.service.NightAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class CommercialController {

    private final GuestProfileService guestProfileService;
    private final GuestRequestService guestRequestService;
    private final NightAuditService nightAuditService;
    private final FolioService folioService;
    private final BookingService bookingService;

    @GetMapping("/guests")
    List<GuestSummary> searchGuests(
            @AuthenticationPrincipal BookaUserDetails user,
            @RequestParam(defaultValue = "") String q) {
        return guestProfileService.search(hotelId(user), q);
    }

    @GetMapping("/guests/{id}")
    GuestProfile getGuest(@AuthenticationPrincipal BookaUserDetails user, @PathVariable Long id) {
        return guestProfileService.get(hotelId(user), id);
    }

    @PatchMapping("/guests/{id}")
    GuestProfile updateGuest(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateGuestProfileRequest request) {
        return guestProfileService.update(hotelId(user), id, request);
    }

    @GetMapping("/guest-requests")
    List<GuestRequestView> listRequests(@AuthenticationPrincipal BookaUserDetails user) {
        return guestRequestService.list(hotelId(user));
    }

    @PostMapping("/guest-requests")
    GuestRequestView createRequest(
            @AuthenticationPrincipal BookaUserDetails user,
            @Valid @RequestBody CreateGuestRequestBody request) {
        return guestRequestService.create(hotelId(user), request);
    }

    @PatchMapping("/guest-requests/{id}")
    GuestRequestView updateRequest(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateGuestRequestBody request) {
        return guestRequestService.update(hotelId(user), id, request);
    }

    @GetMapping("/night-audit")
    NightAuditPreview nightAudit(@AuthenticationPrincipal BookaUserDetails user) {
        return nightAuditService.preview(hotelId(user));
    }

    @PostMapping("/night-audit/close")
    NightAuditRecord closeNight(
            @AuthenticationPrincipal BookaUserDetails user,
            @Valid @RequestBody(required = false) CloseNightRequest request) {
        return nightAuditService.close(hotelId(user), request == null ? new CloseNightRequest(null, true) : request);
    }

    @PostMapping("/bookings/{id}/no-show")
    com.booka.dto.BookingDtos.BookingResponse noShow(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long id,
            @RequestBody(required = false) NoShowBody request) {
        var booking = bookingService.findBooking(id);
        if (user.getHotelId() != null && !user.getHotelId().equals(booking.getHotelId())) {
            throw new BusinessException("Booking belongs to another property");
        }
        boolean retain = request == null || request.retainOneNight() == null || request.retainOneNight();
        return bookingService.markNoShow(id, retain);
    }

    @PostMapping("/folios/{bookingId}/payments")
    FolioResponse postPayment(
            @AuthenticationPrincipal BookaUserDetails user,
            @PathVariable Long bookingId,
            @Valid @RequestBody PostPaymentRequest request) {
        var booking = bookingService.findBooking(bookingId);
        if (user.getHotelId() != null && !user.getHotelId().equals(booking.getHotelId())) {
            throw new BusinessException("Booking belongs to another property");
        }
        FolioResponse folio = folioService.postDeskPayment(
                bookingId, request.amount(), request.method(), request.reference());
        bookingService.syncPaymentStatus(bookingId);
        return folio;
    }

    private Long hotelId(BookaUserDetails user) {
        return user.getHotelId() != null ? user.getHotelId() : 1L;
    }

    public record NoShowBody(Boolean retainOneNight) {}
}