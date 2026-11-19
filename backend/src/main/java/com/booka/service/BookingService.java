package com.booka.service;

import com.booka.domain.entity.Booking;
import com.booka.domain.entity.Guest;
import com.booka.domain.entity.Room;
import com.booka.domain.entity.RoomType;
import com.booka.domain.enums.*;
import com.booka.dto.BookingDtos.*;
import com.booka.dto.FolioDtos.FolioResponse;
import com.booka.dto.FolioDtos.PostChargeRequest;
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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final GuestRepository guestRepository;
    private final RoomRepository roomRepository;
    private final HotelService hotelService;
    private final BlackoutDateService blackoutDateService;
    private final FolioService folioService;

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        return createInternal(
                request.hotelId(),
                request.roomTypeId(),
                null,
                request.checkInDate(),
                request.checkOutDate(),
                request.numGuests(),
                request.guestEmail(),
                request.guestFullName(),
                request.guestPhone(),
                ReservationChannel.DIRECT,
                null,
                // Stripe Checkout deferred: confirm immediately with N_A payment status.
                // When Stripe is re-enabled, switch back to PENDING + UNPAID then checkout/webhook.
                BookingStatus.CONFIRMED,
                PaymentStatus.N_A,
                true,
                null);
    }

    @Transactional
    public BookingResponse createWalkIn(WalkInReservationRequest request, Long staffHotelId) {
        Long hotelId = hotelService.requireStaffHotelId(staffHotelId);
        hotelService.assertHotelAccess(request.hotelId() != null ? request.hotelId() : hotelId, hotelId);
        return createInternal(
                hotelId,
                request.roomTypeId(),
                request.roomId(),
                request.checkInDate(),
                request.checkOutDate(),
                request.numGuests(),
                request.guestEmail(),
                request.guestFullName(),
                request.guestPhone(),
                ReservationChannel.WALK_IN,
                null,
                BookingStatus.CONFIRMED,
                PaymentStatus.N_A,
                true,
                null);
    }

    @Transactional
    public BookingResponse createInternal(
            Long hotelId,
            Long roomTypeId,
            Long preferredRoomId,
            LocalDate checkIn,
            LocalDate checkOut,
            int numGuests,
            String guestEmail,
            String guestFullName,
            String guestPhone,
            ReservationChannel channel,
            String externalReservationId,
            BookingStatus status,
            PaymentStatus paymentStatus,
            boolean assignRoomIfPossible,
            BigDecimal overrideTotal) {

        hotelService.findHotel(hotelId);
        RoomType roomType = hotelService.findRoomType(roomTypeId);
        if (!roomType.getHotelId().equals(hotelId)) {
            throw new BusinessException("Room type does not belong to hotel");
        }
        if (!checkIn.isBefore(checkOut)) {
            throw new BusinessException("Check-in date must be before check-out date");
        }
        if (numGuests > roomType.getMaxOccupancy()) {
            throw new BusinessException("Number of guests exceeds room capacity");
        }

        blackoutDateService.validateNoBlackout(hotelId, roomTypeId, checkIn, checkOut);

        Long roomId = null;
        if (preferredRoomId != null) {
            Room preferred = roomRepository.findById(preferredRoomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
            if (!preferred.getRoomTypeId().equals(roomTypeId)) {
                throw new BusinessException("Room does not match room type");
            }
            if (preferred.getStatus() == RoomStatus.MAINTENANCE) {
                throw new BusinessException("Room is out of order");
            }
            if (bookingRepository.existsOverlappingBooking(preferredRoomId, checkIn, checkOut, null)) {
                throw new BusinessException("Room is not available for the selected dates");
            }
            roomId = preferredRoomId;
        } else if (assignRoomIfPossible) {
            roomId = pickPreferredRoom(roomTypeId, checkIn, checkOut, null);
            if (roomId == null && channel == ReservationChannel.DIRECT) {
                throw new BusinessException("No rooms available for the selected dates");
            }
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal roomSubtotal = overrideTotal != null
                ? overrideTotal
                : roomType.getBaseRate().multiply(BigDecimal.valueOf(nights));
        BigDecimal totalPrice = FolioService.totalWithTax(roomSubtotal);

        Guest guest = upsertGuest(guestEmail, guestFullName, guestPhone);
        if (guest.isBlacklisted()) {
            throw new BusinessException("This guest is blocked from new reservations");
        }

        Booking booking = Booking.builder()
                .hotelId(hotelId)
                .roomId(roomId)
                .roomTypeId(roomTypeId)
                .guestId(guest.getId())
                .confirmationCode(generateConfirmationCode())
                .channel(channel)
                .externalReservationId(externalReservationId)
                .paymentStatus(paymentStatus)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .numGuests(numGuests)
                .totalPrice(totalPrice)
                .status(status)
                .build();

        booking = bookingRepository.save(booking);
        folioService.openFolioWithRoomCharges(
                booking.getId(),
                hotelId,
                roomSubtotal,
                roomType.getName() + " × " + nights + " night(s)");
        return toResponse(booking);
    }

    public PageResponse<BookingResponse> listBookings(Long hotelId, int page, int size) {
        Long scopedHotelId = hotelService.requireStaffHotelId(hotelId);
        Page<Booking> result = bookingRepository.findByHotelId(scopedHotelId, PageRequest.of(page, size));
        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public BookingResponse getBooking(Long id) {
        return toResponse(findBooking(id));
    }

    public BookingResponse getBookingByConfirmationCode(String confirmationCode, Long staffHotelId) {
        String code = confirmationCode == null ? "" : confirmationCode.trim();
        if (code.isEmpty()) {
            throw new ResourceNotFoundException("Booking not found for confirmation code");
        }
        Booking booking = bookingRepository.findByConfirmationCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found for confirmation code: " + code));
        hotelService.assertHotelAccess(booking.getHotelId(), staffHotelId);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse updateStatus(Long id, BookingStatus status, Long staffHotelId) {
        assertBookingHotel(id, staffHotelId);
        if (status == BookingStatus.CHECKED_IN) {
            return checkIn(id, false, staffHotelId);
        }
        if (status == BookingStatus.CHECKED_OUT) {
            return checkOut(id, staffHotelId);
        }
        if (status == BookingStatus.NO_SHOW) {
            return markNoShow(id, true, staffHotelId);
        }
        Booking booking = findBooking(id);
        validateStatusTransition(booking.getStatus(), status);
        booking.setStatus(status);
        if (status == BookingStatus.CANCELLED && booking.getRoomId() != null) {
            Room room = roomRepository.findById(booking.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
            if (room.getStatus() == RoomStatus.OCCUPIED) {
                room.setStatus(RoomStatus.AVAILABLE);
                roomRepository.save(room);
            }
        }
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse checkIn(Long id, boolean force, Long staffHotelId) {
        assertBookingHotel(id, staffHotelId);
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Only confirmed (or pending) bookings can be checked in");
        }
        if (booking.getStatus() == BookingStatus.PENDING && booking.getPaymentStatus() == PaymentStatus.UNPAID) {
            throw new BusinessException("Booking is unpaid; cannot check in");
        }
        if (booking.getRoomId() == null) {
            throw new BusinessException("Assign a room before check-in");
        }
        Room room = roomRepository.findById(booking.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        if (!force && room.getHousekeepingStatus() != HousekeepingStatus.CLEAN) {
            throw new BusinessException("Room is not clean; pass force=true to override");
        }
        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new BusinessException("Room is under maintenance");
        }
        booking.setStatus(BookingStatus.CHECKED_IN);
        if (booking.getPaymentStatus() == PaymentStatus.UNPAID) {
            booking.setPaymentStatus(PaymentStatus.N_A);
        }
        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse checkOut(Long id, Long staffHotelId) {
        assertBookingHotel(id, staffHotelId);
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new BusinessException("Only checked-in bookings can be checked out");
        }
        booking.setStatus(BookingStatus.CHECKED_OUT);
        if (booking.getRoomId() != null) {
            Room room = roomRepository.findById(booking.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
            room.setStatus(RoomStatus.AVAILABLE);
            room.setHousekeepingStatus(HousekeepingStatus.DIRTY);
            roomRepository.save(room);
        }
        folioService.closeIfSettled(id);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse assignRoom(Long bookingId, Long roomId, Long staffHotelId) {
        assertBookingHotel(bookingId, staffHotelId);
        Booking booking = findBooking(bookingId);
        if (booking.getStatus() == BookingStatus.CHECKED_OUT
                || booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.NO_SHOW) {
            throw new BusinessException("Cannot assign room to a closed booking");
        }
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        var roomType = hotelService.findRoomType(room.getRoomTypeId());
        hotelService.assertHotelAccess(roomType.getHotelId(), staffHotelId);
        if (!room.getRoomTypeId().equals(booking.getRoomTypeId())) {
            throw new BusinessException("Room type mismatch");
        }
        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new BusinessException("Room is out of order");
        }
        if (bookingRepository.existsOverlappingBooking(
                roomId, booking.getCheckInDate(), booking.getCheckOutDate(), booking.getId())) {
            throw new BusinessException("Room has an overlapping reservation");
        }
        Long previousRoomId = booking.getRoomId();
        booking.setRoomId(roomId);
        if (booking.getStatus() == BookingStatus.CHECKED_IN) {
            if (previousRoomId != null && !previousRoomId.equals(roomId)) {
                roomRepository.findById(previousRoomId).ifPresent(prev -> {
                    prev.setStatus(RoomStatus.AVAILABLE);
                    prev.setHousekeepingStatus(HousekeepingStatus.DIRTY);
                    roomRepository.save(prev);
                });
            }
            room.setStatus(RoomStatus.OCCUPIED);
            roomRepository.save(room);
        }
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse markPaidAndConfirmed(Long bookingId, String stripeSessionId) {
        Booking booking = findBooking(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Cannot confirm a cancelled booking");
        }
        if (booking.getPaymentStatus() == PaymentStatus.PAID
                && booking.getStatus() != BookingStatus.PENDING) {
            return toResponse(booking);
        }
        booking.setPaymentStatus(PaymentStatus.PAID);
        if (booking.getStatus() == BookingStatus.PENDING) {
            booking.setStatus(BookingStatus.CONFIRMED);
        }
        if (stripeSessionId != null) {
            booking.setStripeCheckoutSessionId(stripeSessionId);
        }
        folioService.postPayment(bookingId, booking.getTotalPrice(), stripeSessionId);
        folioService.closeIfSettled(bookingId);
        return toResponse(bookingRepository.save(booking));
    }

    Long pickPreferredRoom(Long roomTypeId, LocalDate checkIn, LocalDate checkOut, Long excludeBookingId) {
        List<Room> available = roomRepository.findAvailableRooms(roomTypeId, checkIn, checkOut, excludeBookingId);
        return available.stream()
                .sorted(Comparator
                        .comparing((Room r) -> r.getHousekeepingStatus() != HousekeepingStatus.CLEAN)
                        .thenComparing(Room::getRoomNumber))
                .map(Room::getId)
                .findFirst()
                .orElse(null);
    }

    public Booking findBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
    }

    private Guest upsertGuest(String email, String fullName, String phone) {
        return guestRepository.findByEmail(email)
                .map(existing -> {
                    existing.setFullName(fullName);
                    existing.setPhone(phone);
                    return guestRepository.save(existing);
                })
                .orElseGet(() -> guestRepository.save(Guest.builder()
                        .email(email)
                        .fullName(fullName)
                        .phone(phone)
                        .build()));
    }

    private String generateConfirmationCode() {
        return "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Transactional
    public BookingResponse markNoShow(Long id, boolean retainOneNight, Long staffHotelId) {
        assertBookingHotel(id, staffHotelId);
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Only expected arrivals can be marked as no-show");
        }
        booking.setStatus(BookingStatus.NO_SHOW);
        releaseOccupiedRoom(booking);
        applyNoShowAllowance(booking, retainOneNight);
        return toResponse(bookingRepository.save(booking));
    }

    private void assertBookingHotel(Long bookingId, Long staffHotelId) {
        Booking booking = findBooking(bookingId);
        hotelService.assertHotelAccess(booking.getHotelId(), staffHotelId);
    }

    @Transactional
    public void syncPaymentStatus(Long bookingId) {
        Booking booking = findBooking(bookingId);
        FolioResponse folio;
        try {
            folio = folioService.getFolioByBookingId(bookingId);
        } catch (ResourceNotFoundException ex) {
            return;
        }
        if (folio.paymentsTotal().signum() <= 0) {
            return;
        }
        if (folio.balance().signum() <= 0) {
            booking.setPaymentStatus(PaymentStatus.PAID);
        } else {
            booking.setPaymentStatus(PaymentStatus.PARTIAL);
        }
        bookingRepository.save(booking);
    }

    private void releaseOccupiedRoom(Booking booking) {
        if (booking.getRoomId() == null) {
            return;
        }
        roomRepository.findById(booking.getRoomId()).ifPresent(room -> {
            if (room.getStatus() == RoomStatus.OCCUPIED) {
                room.setStatus(RoomStatus.AVAILABLE);
                roomRepository.save(room);
            }
        });
    }

    private void applyNoShowAllowance(Booking booking, boolean retainOneNight) {
        FolioResponse folio;
        try {
            folio = folioService.getFolioByBookingId(booking.getId());
        } catch (ResourceNotFoundException ex) {
            return;
        }
        if (folio.status() == FolioStatus.CLOSED) {
            return;
        }
        long nights = Math.max(1, ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate()));
        BigDecimal oneNight = booking.getTotalPrice().divide(BigDecimal.valueOf(nights), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal extras = folio.chargesTotal().subtract(booking.getTotalPrice());
        if (extras.signum() < 0) {
            extras = BigDecimal.ZERO;
        }
        BigDecimal target = (retainOneNight ? oneNight : BigDecimal.ZERO).add(extras);
        BigDecimal delta = target.subtract(folio.chargesTotal());
        if (delta.signum() == 0) {
            return;
        }
        folioService.postCharge(booking.getId(), new PostChargeRequest(
                FolioLineType.ADJUSTMENT,
                retainOneNight ? "No-show — retain one night" : "No-show — charges waived",
                delta,
                "NOSHOW-" + booking.getId()));
    }

    private void validateStatusTransition(BookingStatus current, BookingStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == BookingStatus.CONFIRMED || next == BookingStatus.CANCELLED
                    || next == BookingStatus.CHECKED_IN || next == BookingStatus.NO_SHOW;
            case CONFIRMED -> next == BookingStatus.CHECKED_IN || next == BookingStatus.CANCELLED
                    || next == BookingStatus.NO_SHOW;
            case CHECKED_IN -> next == BookingStatus.CHECKED_OUT;
            case CHECKED_OUT, CANCELLED, NO_SHOW -> false;
        };
        if (!valid) {
            throw new BusinessException("Invalid status transition from " + current + " to " + next);
        }
    }

    BookingResponse toResponse(Booking booking) {
        var hotel = hotelService.findHotel(booking.getHotelId());
        RoomType roomType = hotelService.findRoomType(booking.getRoomTypeId());
        Guest guest = guestRepository.findById(booking.getGuestId())
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found"));

        String roomNumber = null;
        if (booking.getRoomId() != null) {
            roomNumber = roomRepository.findById(booking.getRoomId())
                    .map(Room::getRoomNumber)
                    .orElse(null);
        }

        return new BookingResponse(
                booking.getId(),
                booking.getConfirmationCode(),
                booking.getHotelId(),
                hotel.getName(),
                booking.getRoomId(),
                roomNumber,
                booking.getRoomTypeId(),
                roomType.getName(),
                guest.getId(),
                guest.getFullName(),
                guest.getEmail(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getNumGuests(),
                booking.getTotalPrice(),
                booking.getStatus(),
                booking.getChannel(),
                booking.getPaymentStatus(),
                booking.getExternalReservationId(),
                booking.getCreatedAt());
    }
}
