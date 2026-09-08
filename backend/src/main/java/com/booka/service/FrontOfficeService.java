package com.booka.service;

import com.booka.domain.entity.Booking;
import com.booka.domain.entity.Room;
import com.booka.domain.entity.RoomType;
import com.booka.domain.enums.HousekeepingStatus;
import com.booka.dto.OperationsDtos.*;
import com.booka.exception.BusinessException;
import com.booka.exception.ResourceNotFoundException;
import com.booka.repository.BookingRepository;
import com.booka.repository.GuestRepository;
import com.booka.repository.RoomImageRepository;
import com.booka.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FrontOfficeService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final GuestRepository guestRepository;
    private final RoomImageRepository roomImageRepository;
    private final HotelService hotelService;

    public List<RoomBoardItem> getRoomBoard(Long hotelId) {
        hotelService.findHotel(hotelId);
        List<Room> rooms = roomRepository.findByHotelId(hotelId);
        Map<Long, String> primaryByRoom = Map.of();
        if (!rooms.isEmpty()) {
            primaryByRoom = roomImageRepository
                    .findByRoomIdInAndPrimaryTrue(rooms.stream().map(Room::getId).toList())
                    .stream()
                    .collect(Collectors.toMap(
                            img -> img.getRoomId(),
                            img -> img.getUrl(),
                            (a, b) -> a));
        }
        Map<Long, String> imageMap = primaryByRoom;
        return rooms.stream().map(room -> toBoardItem(room, imageMap.get(room.getId()))).toList();
    }

    @Transactional
    public RoomBoardItem updateHousekeeping(Long roomId, UpdateHousekeepingRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
        HousekeepingStatus next = request.housekeepingStatus();
        boolean force = Boolean.TRUE.equals(request.force());
        if (next == HousekeepingStatus.CLEAN
                && room.getHousekeepingStatus() != HousekeepingStatus.INSPECTING
                && !force) {
            throw new BusinessException("Room must be INSPECTING before CLEAN (or pass force=true)");
        }
        room.setHousekeepingStatus(next);
        Room saved = roomRepository.save(room);
        String primaryUrl = roomImageRepository.findFirstByRoomIdAndPrimaryTrue(saved.getId())
                .map(img -> img.getUrl())
                .orElse(null);
        return toBoardItem(saved, primaryUrl);
    }

    private RoomBoardItem toBoardItem(Room room, String primaryImageUrl) {
        RoomType roomType = hotelService.findRoomType(room.getRoomTypeId());
        var current = bookingRepository.findActiveAssignedByRoomId(room.getId()).stream().findFirst();
        Long bookingId = null;
        String guestName = null;
        String confirmation = null;
        if (current.isPresent()) {
            Booking b = current.get();
            bookingId = b.getId();
            confirmation = b.getConfirmationCode();
            guestName = guestRepository.findById(b.getGuestId())
                    .map(g -> g.getFullName())
                    .orElse(null);
        }
        return new RoomBoardItem(
                room.getId(),
                room.getRoomTypeId(),
                roomType.getName(),
                room.getRoomNumber(),
                room.getFloor(),
                room.getStatus(),
                room.getHousekeepingStatus(),
                room.getNotes(),
                bookingId,
                guestName,
                confirmation,
                primaryImageUrl);
    }
}
