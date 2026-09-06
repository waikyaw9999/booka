package com.booka.service;

import com.booka.domain.entity.Booking;
import com.booka.domain.entity.ChannelApiKey;
import com.booka.domain.enums.BookingStatus;
import com.booka.domain.enums.PaymentStatus;
import com.booka.domain.enums.ReservationChannel;
import com.booka.dto.BookingDtos.BookingResponse;
import com.booka.dto.BookingDtos.ChannelReservationRequest;
import com.booka.exception.BusinessException;
import com.booka.repository.BookingRepository;
import com.booka.repository.ChannelApiKeyRepository;
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

    public void assertValidApiKey(String channel, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("Missing X-Channel-Api-Key");
        }
        String normalized = normalizeChannel(channel);
        List<ChannelApiKey> keys = channelApiKeyRepository.findByChannelAndActiveTrue(normalized);
        boolean ok = keys.stream().anyMatch(k -> passwordEncoder.matches(apiKey, k.getKeyHash()));
        if (!ok) {
            throw new BusinessException("Invalid channel API key");
        }
    }

    @Transactional
    public BookingResponse upsert(String channelPath, ChannelReservationRequest request) {
        ReservationChannel channel = mapChannel(channelPath);
        var existing = bookingRepository.findByChannelAndExternalReservationId(
                channel, request.externalReservationId());
        if (existing.isPresent()) {
            Booking booking = existing.get();
            if ("CANCELLED".equalsIgnoreCase(request.status())) {
                return bookingService.updateStatus(booking.getId(), BookingStatus.CANCELLED);
            }
            return bookingService.getBooking(booking.getId());
        }

        BookingStatus status = BookingStatus.CONFIRMED;
        if ("CANCELLED".equalsIgnoreCase(request.status())) {
            status = BookingStatus.CANCELLED;
        }

        return bookingService.createInternal(
                request.hotelId(),
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
