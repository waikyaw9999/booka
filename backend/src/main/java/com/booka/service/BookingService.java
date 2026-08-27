package com.booka.service;

import com.booka.domain.entity.Booking;
import com.booka.domain.entity.Guest;
import com.booka.domain.entity.Room;
import com.booka.domain.entity.RoomType;
import com.booka.domain.enums.BookingStatus;
import com.booka.domain.enums.RoomStatus;
import com.booka.dto.BookingDtos.*;
import com.booka.exception.BusinessException;
import com.booka.exception.ResourceNotFoundException;
import com.booka.repository.BookingRepository;
import com.booka.repository.GuestRepository;
import com.booka.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final GuestRepository guestRepository;
    private final RoomRepository roomRepository;
    private final HotelService hotelService;
    private final BlackoutDateService blackoutDateService;

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        hotelService.findHotel(request.hotelId());
        RoomType roomType = hotelService.findRoomType(request.roomTypeId());

        if (!request.checkInDate().isBefore(request.checkOutDate())) {
            throw new BusinessException("Check-in date must be before check-out date");
        }
        if (request.numGuests() > roomType.getMaxOccupancy()) {
            throw new BusinessException("Number of guests exceeds room capacity");
        }

        blackoutDateService.validateNoBlackout(
                request.hotelId(), request.roomTypeId(),
                request.checkInDate(), request.checkOutDate());

        List<Room> available = roomRepository.findAvailableRooms(
                request.roomTypeId(), request.checkInDate(), request.checkOutDate());
        if (available.isEmpty()) {
            throw new BusinessException("No rooms available for the selected dates");
        }

        Room room = available.get(0);
        long nights = ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
        BigDecimal totalPrice = roomType.getBaseRate().multiply(BigDecimal.valueOf(nights));

        Guest guest = guestRepository.findByEmail(request.guestEmail())
                .map(existing -> {
                    existing.setFullName(request.guestFullName());
                    existing.setPhone(request.guestPhone());
                    return guestRepository.save(existing);
                })
                .orElseGet(() -> guestRepository.save(Guest.builder()
                        .email(request.guestEmail())
                        .fullName(request.guestFullName())
                        .phone(request.guestPhone())
                        .build()));

        Booking booking = Booking.builder()
                .hotelId(request.hotelId())
                .roomId(room.getId())
                .guestId(guest.getId())
                .checkInDate(request.checkInDate())
                .checkOutDate(request.checkOutDate())
                .numGuests(request.numGuests())
                .totalPrice(totalPrice)
                .status(BookingStatus.CONFIRMED)
                .build();

        return toResponse(bookingRepository.save(booking));
    }

    public PageResponse<BookingResponse> listBookings(Long hotelId, int page, int size) {
        Page<Booking> result = hotelId != null
                ? bookingRepository.findByHotelId(hotelId, PageRequest.of(page, size))
                : bookingRepository.findAll(PageRequest.of(page, size));
        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public BookingResponse getBooking(Long id) {
        return toResponse(findBooking(id));
    }

    @Transactional
    public BookingResponse updateStatus(Long id, BookingStatus status) {
        Booking booking = findBooking(id);
        validateStatusTransition(booking.getStatus(), status);
        booking.setStatus(status);

        Room room = roomRepository.findById(booking.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        if (status == BookingStatus.CHECKED_IN) {
            room.setStatus(RoomStatus.OCCUPIED);
        } else if (status == BookingStatus.CHECKED_OUT || status == BookingStatus.CANCELLED) {
            room.setStatus(RoomStatus.AVAILABLE);
        }
        roomRepository.save(room);

        return toResponse(bookingRepository.save(booking));
    }

    private void validateStatusTransition(BookingStatus current, BookingStatus next) {
        boolean valid = switch (current) {
            case PENDING, CONFIRMED -> next == BookingStatus.CHECKED_IN || next == BookingStatus.CANCELLED;
            case CHECKED_IN -> next == BookingStatus.CHECKED_OUT;
            case CHECKED_OUT, CANCELLED -> false;
        };
        if (!valid) {
            throw new BusinessException("Invalid status transition from " + current + " to " + next);
        }
    }

    private Booking findBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
    }

    private BookingResponse toResponse(Booking booking) {
        var hotel = hotelService.findHotel(booking.getHotelId());
        Room room = roomRepository.findById(booking.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        RoomType roomType = hotelService.findRoomType(room.getRoomTypeId());
        Guest guest = guestRepository.findById(booking.getGuestId())
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found"));

        return new BookingResponse(
                booking.getId(), booking.getHotelId(), hotel.getName(),
                booking.getRoomId(), room.getRoomNumber(), roomType.getName(),
                guest.getId(), guest.getFullName(), guest.getEmail(),
                booking.getCheckInDate(), booking.getCheckOutDate(),
                booking.getNumGuests(), booking.getTotalPrice(),
                booking.getStatus(), booking.getCreatedAt());
    }
}
