package com.booka.service;

import com.booka.domain.entity.Booking;
import com.booka.domain.entity.Guest;
import com.booka.domain.entity.GuestRequest;
import com.booka.domain.enums.GuestRequestStatus;
import com.booka.dto.CommercialDtos.CreateGuestRequestBody;
import com.booka.dto.CommercialDtos.GuestRequestView;
import com.booka.dto.CommercialDtos.UpdateGuestRequestBody;
import com.booka.exception.BusinessException;
import com.booka.exception.ResourceNotFoundException;
import com.booka.repository.GuestRepository;
import com.booka.repository.GuestRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GuestRequestService {

    private final GuestRequestRepository guestRequestRepository;
    private final GuestRepository guestRepository;
    private final BookingService bookingService;

    public List<GuestRequestView> list(Long hotelId) {
        return guestRequestRepository.findByHotelIdOrderByCreatedAtDesc(hotelId).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public GuestRequestView create(Long hotelId, CreateGuestRequestBody request) {
        Booking booking = bookingService.findBooking(request.bookingId());
        if (!booking.getHotelId().equals(hotelId)) {
            throw new BusinessException("Booking belongs to another property");
        }
        GuestRequest saved = guestRequestRepository.save(GuestRequest.builder()
                .hotelId(hotelId)
                .bookingId(booking.getId())
                .category(request.category())
                .note(request.note().trim())
                .status(GuestRequestStatus.OPEN)
                .build());
        return toView(saved);
    }

    @Transactional
    public GuestRequestView update(Long hotelId, Long id, UpdateGuestRequestBody request) {
        GuestRequest existing = guestRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guest request not found"));
        if (!existing.getHotelId().equals(hotelId)) {
            throw new BusinessException("Request belongs to another property");
        }
        existing.setStatus(request.status());
        return toView(guestRequestRepository.save(existing));
    }

    private GuestRequestView toView(GuestRequest request) {
        Booking booking = bookingService.findBooking(request.getBookingId());
        Guest guest = guestRepository.findById(booking.getGuestId())
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found"));
        return new GuestRequestView(
                request.getId(),
                booking.getId(),
                booking.getConfirmationCode(),
                guest.getFullName(),
                request.getCategory(),
                request.getNote(),
                request.getStatus(),
                request.getCreatedAt());
    }
}
