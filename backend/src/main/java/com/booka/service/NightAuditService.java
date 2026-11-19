package com.booka.service;

import com.booka.domain.entity.Booking;
import com.booka.domain.entity.Guest;
import com.booka.domain.entity.Hotel;
import com.booka.domain.entity.NightAudit;
import com.booka.dto.CommercialDtos.AuditStay;
import com.booka.dto.CommercialDtos.CloseNightRequest;
import com.booka.dto.CommercialDtos.NightAuditPreview;
import com.booka.dto.CommercialDtos.NightAuditRecord;
import com.booka.exception.BusinessException;
import com.booka.exception.ResourceNotFoundException;
import com.booka.repository.GuestRepository;
import com.booka.repository.HotelRepository;
import com.booka.repository.NightAuditRepository;
import com.booka.repository.RoomRepository;
import com.booka.service.PropertyFlashService.Snapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NightAuditService {

    private final PropertyFlashService propertyFlashService;
    private final NightAuditRepository nightAuditRepository;
    private final HotelRepository hotelRepository;
    private final BookingService bookingService;
    private final GuestRepository guestRepository;
    private final RoomRepository roomRepository;

    public NightAuditPreview preview(Long hotelId) {
        Snapshot flash = propertyFlashService.compute(hotelId);
        boolean closed = nightAuditRepository
                .findByHotelIdAndBusinessDate(hotelId, flash.businessDate())
                .isPresent();
        return new NightAuditPreview(
                flash.businessDate().toString(),
                closed,
                flash.occupancyPercent(),
                flash.adr(),
                flash.revpar(),
                flash.roomRevenue(),
                flash.payments(),
                flash.openBalance(),
                flash.roomsSold(),
                flash.sellableRooms(),
                flash.inHouse(),
                flash.arrivals(),
                flash.dueOuts(),
                flash.noShowCandidates(),
                flash.dueOutBookings().stream().map(this::toStay).toList(),
                flash.noShowBookings().stream().map(this::toStay).toList(),
                flash.channels(),
                history(hotelId));
    }

    @Transactional
    public NightAuditRecord close(Long hotelId, CloseNightRequest request) {
        Snapshot before = propertyFlashService.compute(hotelId);
        if (nightAuditRepository.findByHotelIdAndBusinessDate(hotelId, before.businessDate()).isPresent()) {
            throw new BusinessException("This business date is already closed");
        }
        if (!before.dueOutBookings().isEmpty()) {
            throw new BusinessException(before.dueOuts()
                    + " departure(s) are still in house. Check them out or extend the stay before closing the night.");
        }
        boolean retain = request.retainNoShowNight() == null || request.retainNoShowNight();
        for (Booking booking : before.noShowBookings()) {
            bookingService.markNoShow(booking.getId(), retain, hotelId);
        }
        Snapshot after = propertyFlashService.compute(hotelId);
        String note = request.note() == null ? null : request.note().trim();
        NightAudit saved = nightAuditRepository.save(NightAudit.builder()
                .hotelId(hotelId)
                .businessDate(before.businessDate())
                .occupancyPercent(after.occupancyPercent())
                .adr(after.adr())
                .revpar(after.revpar())
                .roomRevenue(after.roomRevenue())
                .payments(after.payments())
                .openBalance(after.openBalance())
                .roomsSold(after.roomsSold())
                .sellableRooms(after.sellableRooms())
                .inHouse(after.inHouse())
                .arrivals(before.arrivals())
                .departures(before.departed())
                .noShows(before.noShowCandidates())
                .note(note == null || note.isEmpty() ? null : note)
                .closedAt(Instant.now())
                .build());

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));
        hotel.setBusinessDate(before.businessDate().plusDays(1));
        hotelRepository.save(hotel);
        return toRecord(saved);
    }

    private List<NightAuditRecord> history(Long hotelId) {
        return nightAuditRepository.findTop14ByHotelIdOrderByBusinessDateDesc(hotelId).stream()
                .map(this::toRecord)
                .toList();
    }

    private NightAuditRecord toRecord(NightAudit audit) {
        return new NightAuditRecord(
                audit.getId(),
                audit.getBusinessDate().toString(),
                audit.getOccupancyPercent(),
                audit.getAdr(),
                audit.getRevpar(),
                audit.getRoomRevenue(),
                audit.getPayments(),
                audit.getOpenBalance(),
                audit.getRoomsSold(),
                audit.getInHouse(),
                audit.getArrivals(),
                audit.getDepartures(),
                audit.getNoShows(),
                audit.getNote(),
                audit.getClosedAt());
    }

    private AuditStay toStay(Booking booking) {
        Guest guest = guestRepository.findById(booking.getGuestId())
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found"));
        String roomNumber = booking.getRoomId() == null
                ? null
                : roomRepository.findById(booking.getRoomId()).map(room -> room.getRoomNumber()).orElse(null);
        return new AuditStay(
                booking.getId(),
                booking.getConfirmationCode(),
                guest.getFullName(),
                roomNumber,
                booking.getCheckInDate().toString(),
                booking.getCheckOutDate().toString(),
                booking.getStatus().name());
    }
}
