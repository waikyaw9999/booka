package com.booka.service;

import com.booka.domain.entity.Booking;
import com.booka.domain.entity.ChannelApiKey;
import com.booka.domain.entity.Room;
import com.booka.domain.entity.RoomType;
import com.booka.domain.enums.BookingStatus;
import com.booka.domain.enums.PaymentStatus;
import com.booka.domain.enums.ReservationChannel;
import com.booka.dto.BookingDtos.BookingResponse;
import com.booka.dto.BookingDtos.ChannelReservationRequest;
import com.booka.exception.BusinessException;
import com.booka.exception.ResourceNotFoundException;
import com.booka.repository.BookingRepository;
import com.booka.repository.ChannelApiKeyRepository;
import com.booka.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ChannelReservationService {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final ChannelApiKeyRepository channelApiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final HotelService hotelService;
    private final RoomRepository roomRepository;

    /**
     * Validates the API key for this channel + hotel. Global (null hotel_id) keys are rejected.
     */
    public Long resolveHotelIdForApiKey(String channel, String apiKey, Long requestedHotelId) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("Missing X-Channel-Api-Key");
        }
        if (requestedHotelId == null) {
            throw new BusinessException("hotelId is required");
        }
        hotelService.findHotel(requestedHotelId);

        String normalized = normalizeChannel(channel);
        List<ChannelApiKey> keys =
                channelApiKeyRepository.findByChannelAndHotelIdAndActiveTrue(normalized, requestedHotelId);
        boolean ok = keys.stream()
                .filter(k -> k.getHotelId() != null)
                .anyMatch(k -> passwordEncoder.matches(apiKey, k.getKeyHash()));
        if (!ok) {
            throw new BusinessException(
                    "Invalid channel API key for this property (keys are per-hotel)");
        }
        return requestedHotelId;
    }

    @Transactional
    public BookingResponse upsert(String channelPath, String apiKey, ChannelReservationRequest request) {
        Long hotelId = resolveHotelIdForApiKey(channelPath, apiKey, request.hotelId());
        ReservationChannel channel = mapChannel(channelPath);

        RoomType roomType = hotelService.findRoomType(request.roomTypeId());
        if (!hotelId.equals(roomType.getHotelId())) {
            throw new BusinessException("roomTypeId does not belong to hotelId");
        }
        if (request.roomId() != null) {
            Room room = roomRepository.findById(request.roomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
            RoomType roomHotelType = hotelService.findRoomType(room.getRoomTypeId());
            if (!hotelId.equals(roomHotelType.getHotelId())) {
                throw new BusinessException("roomId does not belong to hotelId");
            }
        }

        var existing = bookingRepository.findByChannelAndExternalReservationId(
                channel, request.externalReservationId());
        if (existing.isPresent()) {
            Booking booking = existing.get();
            if (!hotelId.equals(booking.getHotelId())) {
                throw new BusinessException(
                        "externalReservationId belongs to another property");
            }
            if ("CANCELLED".equalsIgnoreCase(request.status())) {
                return bookingService.updateStatus(
                        booking.getId(), BookingStatus.CANCELLED, booking.getHotelId());
            }
            return bookingService.getBooking(booking.getId());
        }

        BookingStatus status = BookingStatus.CONFIRMED;
        if ("CANCELLED".equalsIgnoreCase(request.status())) {
            status = BookingStatus.CANCELLED;
        }

        return bookingService.createInternal(
                hotelId,
                request.roomTypeId(),
                request.roomId(),
                request.checkInDate(),
                request.checkOutDate(),
                request.numGuests(),
                request.guestEmail(),
                request.guestFullName(),
                request.guestPhone(),
                channel,
                request.externalReservationId(),
                status,
                PaymentStatus.N_A,
                request.roomId() == null,
                request.totalPrice());
    }

    private ReservationChannel mapChannel(String channelPath) {
        String n = normalizeChannel(channelPath);
        return switch (n) {
            case "OTA", "BOOKING_COM", "EXPEDIA", "AGODA" -> ReservationChannel.OTA;
            case "CHANNEL_MANAGER", "CM" -> ReservationChannel.CHANNEL_MANAGER;
            default -> ReservationChannel.OTA;
        };
    }

    private String normalizeChannel(String channel) {
        return channel == null ? "" : channel.trim().toUpperCase(Locale.ROOT);
    }
}
