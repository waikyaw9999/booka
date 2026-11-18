package com.booka.service;

import com.booka.domain.entity.Booking;
import com.booka.domain.entity.Guest;
import com.booka.domain.enums.BookingStatus;
import com.booka.dto.CommercialDtos.GuestProfile;
import com.booka.dto.CommercialDtos.GuestStay;
import com.booka.dto.CommercialDtos.GuestSummary;
import com.booka.dto.CommercialDtos.UpdateGuestProfileRequest;
import com.booka.exception.ResourceNotFoundException;
import com.booka.repository.BookingRepository;
import com.booka.repository.GuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GuestProfileService {

    private final GuestRepository guestRepository;
    private final BookingRepository bookingRepository;
    private final HotelService hotelService;

    public List<GuestSummary> search(Long hotelId, String query) {
        String q = query == null ? "" : query.trim();
        return guestRepository.searchAtHotel(hotelId, q, PageRequest.of(0, 40)).stream()
                .map(guest -> toSummary(hotelId, guest))
                .toList();
    }

    public GuestProfile get(Long hotelId, Long guestId) {
        Guest guest = requireGuestAtHotel(hotelId, guestId);
        return toProfile(hotelId, guest);
    }

    @Transactional
    public GuestProfile update(Long hotelId, Long guestId, UpdateGuestProfileRequest request) {
        Guest guest = requireGuestAtHotel(hotelId, guestId);
        String nationality = request.nationality() == null ? null : request.nationality().trim();
        guest.setNationality(nationality == null || nationality.isEmpty() ? null : nationality);
        guest.setVipTier(request.vipTier());
        String notes = request.notes() == null ? null : request.notes().trim();
        guest.setNotes(notes == null || notes.isEmpty() ? null : notes);
        guest.setBlacklisted(Boolean.TRUE.equals(request.blacklisted()));
        return toProfile(hotelId, guestRepository.save(guest));
    }

    private Guest requireGuestAtHotel(Long hotelId, Long guestId) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found"));
        boolean stayedHere = bookingRepository.findByHotelIdAndGuestIdOrderByCheckInDateDesc(hotelId, guestId)
                .stream()
                .findAny()
                .isPresent();
        if (!stayedHere) {
            throw new ResourceNotFoundException("Guest not found at this property");
        }
        return guest;
    }

    private GuestSummary toSummary(Long hotelId, Guest guest) {
        List<Booking> stays = countedStays(hotelId, guest.getId());
        return new GuestSummary(
                guest.getId(),
                guest.getFullName(),
                guest.getEmail(),
                guest.getPhone(),
                guest.getNationality(),
                guest.getVipTier(),
                guest.isBlacklisted(),
                stays.size(),
                revenue(stays));
    }

    private GuestProfile toProfile(Long hotelId, Guest guest) {
        List<Booking> stays = countedStays(hotelId, guest.getId());
        int nights = stays.stream()
                .mapToInt(b -> (int) Math.max(1, ChronoUnit.DAYS.between(b.getCheckInDate(), b.getCheckOutDate())))
                .sum();
        List<GuestStay> history = bookingRepository
                .findByHotelIdAndGuestIdOrderByCheckInDateDesc(hotelId, guest.getId())
                .stream()
                .map(b -> new GuestStay(
                        b.getId(),
                        b.getConfirmationCode(),
                        b.getCheckInDate().toString(),
                        b.getCheckOutDate().toString(),
                        b.getStatus().name(),
                        hotelService.findRoomType(b.getRoomTypeId()).getName(),
                        b.getChannel().name(),
                        b.getTotalPrice()))
                .toList();
        return new GuestProfile(
                guest.getId(),
                guest.getFullName(),
                guest.getEmail(),
                guest.getPhone(),
                guest.getNationality(),
                guest.getVipTier(),
                guest.getNotes(),
                guest.isBlacklisted(),
                stays.size(),
                nights,
                revenue(stays),
                history);
    }

    private List<Booking> countedStays(Long hotelId, Long guestId) {
        return bookingRepository.findByHotelIdAndGuestIdOrderByCheckInDateDesc(hotelId, guestId).stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .toList();
    }

    private BigDecimal revenue(List<Booking> stays) {
        return stays.stream()
                .map(Booking::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
