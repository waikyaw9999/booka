package com.booka.service;

import com.booka.domain.entity.Booking;
import com.booka.domain.entity.Folio;
import com.booka.domain.entity.FolioLineItem;
import com.booka.domain.entity.Hotel;
import com.booka.domain.entity.Room;
import com.booka.domain.enums.BookingStatus;
import com.booka.domain.enums.FolioLineType;
import com.booka.domain.enums.FolioStatus;
import com.booka.domain.enums.ReservationChannel;
import com.booka.domain.enums.RoomStatus;
import com.booka.dto.HotelDtos.ChannelRevenue;
import com.booka.exception.ResourceNotFoundException;
import com.booka.repository.BookingRepository;
import com.booka.repository.FolioLineItemRepository;
import com.booka.repository.FolioRepository;
import com.booka.repository.HotelRepository;
import com.booka.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PropertyFlashService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final FolioRepository folioRepository;
    private final FolioLineItemRepository folioLineItemRepository;
    private final FolioService folioService;

    public Snapshot compute(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));
        LocalDate businessDate = hotel.getBusinessDate() != null ? hotel.getBusinessDate() : LocalDate.now();

        List<Room> rooms = roomRepository.findByHotelId(hotelId);
        int totalRooms = rooms.size();
        int maintenanceRooms = (int) rooms.stream().filter(r -> r.getStatus() == RoomStatus.MAINTENANCE).count();
        int occupiedRooms = (int) rooms.stream().filter(r -> r.getStatus() == RoomStatus.OCCUPIED).count();
        int sellableRooms = Math.max(0, totalRooms - maintenanceRooms);

        List<Booking> bookings = bookingRepository.findAllByHotelId(hotelId);
        List<Booking> inHouse = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CHECKED_IN)
                .filter(b -> !b.getCheckInDate().isAfter(businessDate) && b.getCheckOutDate().isAfter(businessDate))
                .toList();
        List<Booking> dueOuts = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CHECKED_IN && !b.getCheckOutDate().isAfter(businessDate))
                .toList();
        List<Booking> arrivals = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.PENDING)
                .filter(b -> b.getCheckInDate().equals(businessDate))
                .toList();
        List<Booking> noShows = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.PENDING)
                .filter(b -> !b.getCheckInDate().isAfter(businessDate))
                .toList();
        int departed = (int) bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CHECKED_OUT && b.getCheckOutDate().equals(businessDate))
                .count();

        Map<ReservationChannel, BigDecimal> revenueByChannel = new EnumMap<>(ReservationChannel.class);
        Map<ReservationChannel, Integer> roomsByChannel = new EnumMap<>(ReservationChannel.class);
        BigDecimal roomRevenue = BigDecimal.ZERO;
        for (Booking booking : inHouse) {
            BigDecimal nightly = roomRevenueForNight(booking);
            roomRevenue = roomRevenue.add(nightly);
            revenueByChannel.merge(booking.getChannel(), nightly, BigDecimal::add);
            roomsByChannel.merge(booking.getChannel(), 1, Integer::sum);
        }

        int roomsSold = inHouse.size();
        BigDecimal adr = roomsSold == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : roomRevenue.divide(BigDecimal.valueOf(roomsSold), 2, RoundingMode.HALF_UP);
        BigDecimal revpar = sellableRooms == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : roomRevenue.divide(BigDecimal.valueOf(sellableRooms), 2, RoundingMode.HALF_UP);
        BigDecimal occupancy = sellableRooms == 0
                ? BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(roomsSold * 100.0 / sellableRooms).setScale(1, RoundingMode.HALF_UP);

        ZoneId zone = ZoneId.systemDefault();
        Instant from = businessDate.atStartOfDay(zone).toInstant();
        Instant to = businessDate.plusDays(1).atStartOfDay(zone).toInstant();
        BigDecimal payments = folioLineItemRepository
                .findForHotelBetween(hotelId, FolioLineType.PAYMENT, from, to)
                .stream()
                .map(FolioLineItem::getAmount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal openBalance = BigDecimal.ZERO;
        for (Folio folio : folioRepository.findByHotelIdAndStatus(hotelId, FolioStatus.OPEN)) {
            openBalance = openBalance.add(folioService.getFolioByBookingId(folio.getBookingId()).balance());
        }
        openBalance = openBalance.setScale(2, RoundingMode.HALF_UP);

        List<ChannelRevenue> channels = new ArrayList<>();
        for (ReservationChannel channel : ReservationChannel.values()) {
            int count = roomsByChannel.getOrDefault(channel, 0);
            if (count == 0) {
                continue;
            }
            channels.add(new ChannelRevenue(
                    channel.name(),
                    count,
                    revenueByChannel.getOrDefault(channel, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)));
        }
        channels.sort(Comparator.comparing(ChannelRevenue::roomRevenue).reversed());

        return new Snapshot(
                businessDate,
                occupancy,
                adr,
                revpar,
                roomRevenue.setScale(2, RoundingMode.HALF_UP),
                payments,
                openBalance,
                roomsSold,
                sellableRooms,
                totalRooms,
                occupiedRooms,
                maintenanceRooms,
                inHouse.size(),
                arrivals.size(),
                departed,
                dueOuts.size(),
                noShows.size(),
                channels,
                dueOuts,
                noShows);
    }

    static BigDecimal roomRevenueForNight(Booking booking) {
        long nights = Math.max(1, ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate()));
        BigDecimal perNightWithTax = booking.getTotalPrice()
                .divide(BigDecimal.valueOf(nights), 4, RoundingMode.HALF_UP);
        BigDecimal divisor = BigDecimal.ONE.add(FolioService.TAX_RATE);
        return perNightWithTax.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    public record Snapshot(
            LocalDate businessDate,
            BigDecimal occupancyPercent,
            BigDecimal adr,
            BigDecimal revpar,
            BigDecimal roomRevenue,
            BigDecimal payments,
            BigDecimal openBalance,
            int roomsSold,
            int sellableRooms,
            int totalRooms,
            int occupiedRooms,
            int maintenanceRooms,
            int inHouse,
            int arrivals,
            int departed,
            int dueOuts,
            int noShowCandidates,
            List<ChannelRevenue> channels,
            List<Booking> dueOutBookings,
            List<Booking> noShowBookings
    ) {}
}
