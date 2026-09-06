package com.booka.service;

import com.booka.domain.entity.*;
import com.booka.domain.enums.RoomStatus;
import com.booka.dto.HotelDtos.*;
import com.booka.exception.ResourceNotFoundException;
import com.booka.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final BlackoutDateService blackoutDateService;

    public List<HotelResponse> listHotels() {
        return hotelRepository.findAll().stream().map(this::toHotelResponse).toList();
    }

    public HotelResponse getHotel(Long id) {
        return toHotelResponse(findHotel(id));
    }

    public List<RoomTypeResponse> getRoomTypes(Long hotelId) {
        findHotel(hotelId);
        return roomTypeRepository.findByHotelId(hotelId).stream().map(this::toRoomTypeResponse).toList();
    }

    public List<AvailableRoomTypeResponse> checkAvailability(AvailabilityRequest request) {
        findHotel(request.hotelId());
        validateDates(request.checkInDate(), request.checkOutDate());

        long nights = ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
        return roomTypeRepository.findByHotelId(request.hotelId()).stream()
                .filter(rt -> rt.getMaxOccupancy() >= request.numGuests())
                .filter(rt -> !blackoutDateService.isRoomTypeBlackedOut(
                        request.hotelId(), rt.getId(), request.checkInDate(), request.checkOutDate()))
                .map(rt -> {
                    int count = roomRepository.findAvailableRooms(rt.getId(), request.checkInDate(), request.checkOutDate()).size();
                    if (count == 0) return null;
                    var total = rt.getBaseRate().multiply(java.math.BigDecimal.valueOf(nights));
                    return new AvailableRoomTypeResponse(
                            toRoomTypeResponse(rt), count, total, rt.getBaseRate(), nights);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional
    public HotelResponse createHotel(CreateHotelRequest request) {
        Hotel hotel = Hotel.builder()
                .name(request.name())
                .description(request.description())
                .address(request.address())
                .city(request.city())
                .country(request.country())
                .amenities(request.amenities())
                .cancellationPolicy(request.cancellationPolicy())
                .build();
        return toHotelResponse(hotelRepository.save(hotel));
    }

    @Transactional
    public RoomTypeResponse createRoomType(CreateRoomTypeRequest request) {
        findHotel(request.hotelId());
        RoomType roomType = RoomType.builder()
                .hotelId(request.hotelId())
                .name(request.name())
                .description(request.description())
                .maxOccupancy(request.maxOccupancy())
                .baseRate(request.baseRate())
                .amenities(request.amenities())
                .build();
        return toRoomTypeResponse(roomTypeRepository.save(roomType));
    }

    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request) {
        roomTypeRepository.findById(request.roomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Room type not found"));
        Room room = Room.builder()
                .roomTypeId(request.roomTypeId())
                .roomNumber(request.roomNumber())
                .status(RoomStatus.AVAILABLE)
                .housekeepingStatus(com.booka.domain.enums.HousekeepingStatus.CLEAN)
                .build();
        return toRoomResponse(roomRepository.save(room));
    }

    Hotel findHotel(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found: " + id));
    }

    RoomType findRoomType(Long id) {
        return roomTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room type not found: " + id));
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (!checkIn.isBefore(checkOut)) {
            throw new com.booka.exception.BusinessException("Check-in date must be before check-out date");
        }
    }

    private HotelResponse toHotelResponse(Hotel h) {
        return new HotelResponse(h.getId(), h.getName(), h.getDescription(), h.getAddress(),
                h.getCity(), h.getCountry(), h.getAmenities(), h.getCancellationPolicy());
    }

    private RoomTypeResponse toRoomTypeResponse(RoomType rt) {
        return new RoomTypeResponse(rt.getId(), rt.getHotelId(), rt.getName(), rt.getDescription(),
                rt.getMaxOccupancy(), rt.getBaseRate(), rt.getAmenities());
    }

    private RoomResponse toRoomResponse(Room r) {
        return new RoomResponse(r.getId(), r.getRoomTypeId(), r.getRoomNumber(), r.getStatus().name());
    }
}
