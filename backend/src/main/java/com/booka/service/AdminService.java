package com.booka.service;

import com.booka.domain.enums.BookingStatus;
import com.booka.dto.HotelDtos.DashboardStatsResponse;
import com.booka.repository.BookingRepository;
import com.booka.service.PropertyFlashService.Snapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final BookingRepository bookingRepository;
    private final PropertyFlashService propertyFlashService;

    public DashboardStatsResponse getDashboardStats(Long hotelId) {
        Snapshot flash = propertyFlashService.compute(hotelId);
        long available = flash.sellableRooms() - flash.occupiedRooms();
        if (available < 0) {
            available = 0;
        }
        long checkIns = bookingRepository.findUpcomingCheckIns(
                hotelId,
                flash.businessDate(),
                List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING)).size();
        long active = bookingRepository.findAllByHotelId(hotelId).stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.CHECKED_IN)
                .count();

        return new DashboardStatsResponse(
                flash.totalRooms(),
                flash.occupiedRooms(),
                available,
                flash.maintenanceRooms(),
                checkIns,
                flash.dueOuts(),
                active,
                flash.businessDate().toString(),
                flash.occupancyPercent(),
                flash.roomRevenue(),
                flash.adr(),
                flash.revpar(),
                flash.openBalance(),
                flash.inHouse(),
                flash.noShowCandidates(),
                flash.dueOuts(),
                flash.channels());
    }
}
