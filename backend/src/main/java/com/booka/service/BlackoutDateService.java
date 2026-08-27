package com.booka.service;

import com.booka.domain.entity.BlackoutDate;
import com.booka.domain.entity.RoomType;
import com.booka.dto.BlackoutDtos.*;
import com.booka.exception.BusinessException;
import com.booka.exception.ResourceNotFoundException;
import com.booka.repository.BlackoutDateRepository;
import com.booka.repository.HotelRepository;
import com.booka.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlackoutDateService {

    private final BlackoutDateRepository blackoutDateRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;

    public List<BlackoutDateResponse> listByHotel(Long hotelId) {
        ensureHotelExists(hotelId);
        return mapResponses(blackoutDateRepository.findByHotelIdOrderByStartDateAsc(hotelId), hotelId);
    }

    public List<BlackoutDateResponse> listByHotelAndRange(Long hotelId, LocalDate from, LocalDate to) {
        ensureHotelExists(hotelId);
        return mapResponses(blackoutDateRepository.findByHotelAndDateRange(hotelId, from, to), hotelId);
    }

    @Transactional
    public BlackoutDateResponse create(CreateBlackoutDateRequest request) {
        ensureHotelExists(request.hotelId());
        validateDateRange(request.startDate(), request.endDate());

        if (request.roomTypeId() != null) {
            RoomType roomType = findRoomType(request.roomTypeId());
            if (!roomType.getHotelId().equals(request.hotelId())) {
                throw new BusinessException("Room type does not belong to this hotel");
            }
        }

        if (blackoutDateRepository.existsOverlappingScope(
                request.hotelId(), request.roomTypeId(),
                request.startDate(), request.endDate(), null)) {
            throw new BusinessException("A blackout date already exists for this scope and period");
        }

        BlackoutDate blackout = BlackoutDate.builder()
                .hotelId(request.hotelId())
                .roomTypeId(request.roomTypeId())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .reason(request.reason())
                .build();

        blackout = blackoutDateRepository.save(blackout);
        return toResponse(blackout, resolveRoomTypeName(blackout.getRoomTypeId()));
    }

    @Transactional
    public void delete(Long id, Long hotelId) {
        BlackoutDate blackout = blackoutDateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blackout date not found: " + id));
        if (hotelId != null && !blackout.getHotelId().equals(hotelId)) {
            throw new BusinessException("Blackout date does not belong to your hotel");
        }
        blackoutDateRepository.delete(blackout);
    }

    public boolean isRoomTypeBlackedOut(Long hotelId, Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        return !blackoutDateRepository.findOverlappingForRoomType(hotelId, roomTypeId, checkIn, checkOut).isEmpty();
    }

    public void validateNoBlackout(Long hotelId, Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        List<BlackoutDate> conflicts = blackoutDateRepository.findOverlappingForRoomType(
                hotelId, roomTypeId, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            BlackoutDate first = conflicts.get(0);
            String scope = first.getRoomTypeId() == null ? "hotel" : "room type";
            throw new BusinessException(
                    "Selected dates overlap a " + scope + " blackout: " + first.getReason());
        }
    }

    private void ensureHotelExists(Long hotelId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new ResourceNotFoundException("Hotel not found: " + hotelId);
        }
    }

    private RoomType findRoomType(Long id) {
        return roomTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room type not found: " + id));
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (!start.isBefore(end)) {
            throw new BusinessException("Blackout start date must be before end date");
        }
    }

    private List<BlackoutDateResponse> mapResponses(List<BlackoutDate> blackouts, Long hotelId) {
        Map<Long, String> roomTypeNames = roomTypeRepository.findByHotelId(hotelId).stream()
                .collect(Collectors.toMap(RoomType::getId, RoomType::getName));
        return blackouts.stream()
                .map(b -> toResponse(b, roomTypeNames.get(b.getRoomTypeId())))
                .toList();
    }

    private String resolveRoomTypeName(Long roomTypeId) {
        if (roomTypeId == null) return null;
        return roomTypeRepository.findById(roomTypeId).map(RoomType::getName).orElse(null);
    }

    private BlackoutDateResponse toResponse(BlackoutDate b, String roomTypeName) {
        return new BlackoutDateResponse(
                b.getId(), b.getHotelId(), b.getRoomTypeId(), roomTypeName,
                b.getStartDate(), b.getEndDate(), b.getReason(), b.getCreatedAt());
    }
}
