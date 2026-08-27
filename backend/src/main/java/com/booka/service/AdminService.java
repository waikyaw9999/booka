package com.booka.service;

import com.booka.domain.enums.BookingStatus;
import com.booka.domain.enums.RoomStatus;
import com.booka.dto.HotelDtos.DashboardStatsResponse;
import com.booka.repository.BookingRepository;
import com.booka.repository.RoomRepository;
import com.booka.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final BookingRepository bookingRepository;

    public DashboardStatsResponse getDashboardStats(Long hotelId) {
        List<Long> roomTypeIds = roomTypeRepository.findByHotelId(hotelId).stream()
                .map(rt -> rt.getId()).toList();

        long totalRooms = roomRepository.findAll().stream()
                .filter(r -> roomTypeIds.contains(r.getRoomTypeId())).count();
        long occupied = roomRepository.findAll().stream()
                .filter(r -> roomTypeIds.contains(r.getRoomTypeId()) && r.getStatus() == RoomStatus.OCCUPIED).count();
        long maintenance = roomRepository.findAll().stream()
                .filter(r -> roomTypeIds.contains(r.getRoomTypeId()) && r.getStatus() == RoomStatus.MAINTENANCE).count();
        long available = totalRooms - occupied - maintenance;

        LocalDate today = LocalDate.now();
        long checkIns = bookingRepository.findUpcomingCheckIns(hotelId, today,
                List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING)).size();
        long checkOuts = bookingRepository.findUpcomingCheckOuts(hotelId, today).size();
        long active = bookingRepository.findByHotelId(hotelId, org.springframework.data.domain.PageRequest.of(0, 1000))
                .stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.CHECKED_IN).count();

        return new DashboardStatsResponse(totalRooms, occupied, available, maintenance, checkIns, checkOuts, active);
    }
}
