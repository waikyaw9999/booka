package com.booka.service;

import com.booka.domain.entity.*;
import com.booka.domain.enums.RoomStatus;
import com.booka.dto.HotelDtos.*;
import com.booka.dto.ImageDtos;
import com.booka.exception.BusinessException;
import com.booka.exception.ResourceNotFoundException;
import com.booka.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final HotelImageRepository hotelImageRepository;
    private final RoomImageRepository roomImageRepository;
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
    public HotelResponse updateHotel(Long id, UpdateHotelRequest request, Long staffHotelId) {
        assertHotelAccess(id, staffHotelId);
        Hotel hotel = findHotel(id);
        if (request.amenities() != null) {
            hotel.setAmenities(normalizeAmenities(request.amenities()));
        }
        if (request.cancellationPolicy() != null) {
            hotel.setCancellationPolicy(request.cancellationPolicy().isBlank()
                    ? null
                    : request.cancellationPolicy().trim());
        }
        if (request.description() != null) {
            hotel.setDescription(request.description().isBlank() ? null : request.description().trim());
        }
        return toHotelResponse(hotelRepository.save(hotel));
    }

    @Transactional
    public RoomTypeResponse createRoomType(CreateRoomTypeRequest request) {
        findHotel(request.hotelId());
        RoomType roomType = RoomType.builder()
                .hotelId(request.hotelId())
                .name(request.name().trim())
                .description(request.description())
                .maxOccupancy(request.maxOccupancy())
                .baseRate(request.baseRate())
                .amenities(normalizeAmenities(request.amenities()))
                .build();
        return toRoomTypeResponse(roomTypeRepository.save(roomType));
    }

    @Transactional
    public RoomTypeResponse updateRoomType(Long id, UpdateRoomTypeRequest request, Long staffHotelId) {
        RoomType roomType = findRoomType(id);
        assertHotelAccess(roomType.getHotelId(), staffHotelId);

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BusinessException("Room type name cannot be blank");
            }
            roomType.setName(request.name().trim());
        }
        if (request.description() != null) {
            roomType.setDescription(request.description().isBlank() ? null : request.description().trim());
        }
        if (request.maxOccupancy() != null) {
            roomType.setMaxOccupancy(request.maxOccupancy());
        }
        if (request.baseRate() != null) {
            if (request.baseRate().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Base rate must be greater than zero");
            }
            roomType.setBaseRate(request.baseRate());
        }
        if (request.amenities() != null) {
            roomType.setAmenities(normalizeAmenities(request.amenities()));
        }
        return toRoomTypeResponse(roomTypeRepository.save(roomType));
    }

    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request, Long staffHotelId) {
        RoomType roomType = findRoomType(request.roomTypeId());
        assertHotelAccess(roomType.getHotelId(), staffHotelId);

        String roomNumber = request.roomNumber().trim();
        if (roomRepository.existsByRoomTypeIdAndRoomNumberIgnoreCase(request.roomTypeId(), roomNumber)) {
            throw new BusinessException("Room number already exists for this room type: " + roomNumber);
        }

        Room room = Room.builder()
                .roomTypeId(request.roomTypeId())
                .roomNumber(roomNumber)
                .floor(request.floor())
                .notes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim())
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

    void assertHotelAccess(Long hotelId, Long staffHotelId) {
        if (staffHotelId != null && !staffHotelId.equals(hotelId)) {
            throw new BusinessException("Resource does not belong to your hotel");
        }
    }

    private String normalizeAmenities(String amenities) {
        if (amenities == null) {
            return null;
        }
        String trimmed = amenities.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (!checkIn.isBefore(checkOut)) {
            throw new BusinessException("Check-in date must be before check-out date");
        }
    }

    private HotelResponse toHotelResponse(Hotel h) {
        var images = hotelImageRepository.findByHotelIdOrderBySortOrderAscIdAsc(h.getId()).stream()
                .map(img -> new ImageDtos.ImageResponse(
                        img.getId(),
                        img.getUrl(),
                        img.getCaption(),
                        img.getSortOrder(),
                        img.isPrimary()))
                .toList();
        String primaryUrl = images.stream()
                .filter(ImageDtos.ImageResponse::primary)
                .map(ImageDtos.ImageResponse::url)
                .findFirst()
                .orElse(images.isEmpty() ? null : images.get(0).url());
        return new HotelResponse(
                h.getId(),
                h.getName(),
                h.getDescription(),
                h.getAddress(),
                h.getCity(),
                h.getCountry(),
                h.getAmenities(),
                h.getCancellationPolicy(),
                images,
                primaryUrl);
    }

    private RoomTypeResponse toRoomTypeResponse(RoomType rt) {
        String primaryImageUrl = roomImageRepository.findDisplayUrlForRoomType(rt.getId()).orElse(null);
        return new RoomTypeResponse(
                rt.getId(),
                rt.getHotelId(),
                rt.getName(),
                rt.getDescription(),
                rt.getMaxOccupancy(),
                rt.getBaseRate(),
                rt.getAmenities(),
                primaryImageUrl);
    }

    private RoomResponse toRoomResponse(Room r) {
        return new RoomResponse(
                r.getId(),
                r.getRoomTypeId(),
                r.getRoomNumber(),
                r.getStatus().name(),
                r.getFloor(),
                r.getNotes());
    }
}
