package com.booka.service;

import com.booka.domain.entity.Room;
import com.booka.domain.entity.RoomType;
import com.booka.domain.enums.HousekeepingStatus;
import com.booka.domain.enums.RoomStatus;
import com.booka.repository.RoomRepository;
import com.booka.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Seeds a minimal sellable inventory when a property first registers.
 */
@Service
@RequiredArgsConstructor
public class PropertyStarterService {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final HotelService hotelService;

    @Transactional
    public void seedStarterInventory(Long hotelId) {
        hotelService.findHotel(hotelId);
        if (!roomTypeRepository.findByHotelId(hotelId).isEmpty()) {
            return;
        }

        RoomType standard = roomTypeRepository.save(RoomType.builder()
                .hotelId(hotelId)
                .name("Standard")
                .description("Comfortable queen room with workspace essentials.")
                .maxOccupancy(2)
                .baseRate(new BigDecimal("89.00"))
                .amenities("Wi-Fi, Air conditioning, Ensuite")
                .build());

        RoomType deluxe = roomTypeRepository.save(RoomType.builder()
                .hotelId(hotelId)
                .name("Deluxe")
                .description("Spacious room with lounge seating for longer stays.")
                .maxOccupancy(3)
                .baseRate(new BigDecimal("129.00"))
                .amenities("Wi-Fi, Air conditioning, Mini fridge, Ensuite")
                .build());

        saveRoom(standard.getId(), "101", 1);
        saveRoom(standard.getId(), "102", 1);
        saveRoom(deluxe.getId(), "201", 2);
    }

    private void saveRoom(Long roomTypeId, String roomNumber, int floor) {
        roomRepository.save(Room.builder()
                .roomTypeId(roomTypeId)
                .roomNumber(roomNumber)
                .floor(floor)
                .status(RoomStatus.AVAILABLE)
                .housekeepingStatus(HousekeepingStatus.CLEAN)
                .build());
    }
}
