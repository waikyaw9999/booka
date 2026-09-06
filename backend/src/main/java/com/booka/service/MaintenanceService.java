package com.booka.service;

import com.booka.domain.entity.Booking;
import com.booka.domain.entity.MaintenanceTicket;
import com.booka.domain.entity.Room;
import com.booka.domain.enums.RoomStatus;
import com.booka.domain.enums.TicketStatus;
import com.booka.dto.OperationsDtos.*;
import com.booka.exception.BusinessException;
import com.booka.exception.ResourceNotFoundException;
import com.booka.repository.MaintenanceTicketRepository;
import com.booka.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceTicketRepository ticketRepository;
    private final RoomRepository roomRepository;
    private final HotelService hotelService;

    public List<MaintenanceTicketResponse> list(Long hotelId) {
        hotelService.findHotel(hotelId);
        return ticketRepository.findByHotelIdOrderByCreatedAtDesc(hotelId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MaintenanceTicketResponse create(CreateMaintenanceTicketRequest request) {
        hotelService.findHotel(request.hotelId());
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        RoomTypeHotelGuard(room, request.hotelId());

        MaintenanceTicket ticket = ticketRepository.save(MaintenanceTicket.builder()
                .hotelId(request.hotelId())
                .roomId(request.roomId())
                .title(request.title())
                .description(request.description())
                .priority(request.priority())
                .status(TicketStatus.OPEN)
                .reportedBy(request.reportedBy())
                .build());

        if (Boolean.TRUE.equals(request.markRoomOutOfOrder())) {
            room.setStatus(RoomStatus.MAINTENANCE);
            roomRepository.save(room);
        }
        return toResponse(ticket);
    }

    @Transactional
    public MaintenanceTicketResponse update(Long id, UpdateMaintenanceTicketRequest request) {
        MaintenanceTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + id));
        if (request.status() != null) {
            ticket.setStatus(request.status());
            if (request.status() == TicketStatus.RESOLVED || request.status() == TicketStatus.CANCELLED) {
                Room room = roomRepository.findById(ticket.getRoomId())
                        .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
                if (room.getStatus() == RoomStatus.MAINTENANCE) {
                    room.setStatus(RoomStatus.AVAILABLE);
                    roomRepository.save(room);
                }
            }
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }
        if (request.description() != null) {
            ticket.setDescription(request.description());
        }
        return toResponse(ticketRepository.save(ticket));
    }

    private void RoomTypeHotelGuard(Room room, Long hotelId) {
        var roomType = hotelService.findRoomType(room.getRoomTypeId());
        if (!roomType.getHotelId().equals(hotelId)) {
            throw new BusinessException("Room does not belong to hotel");
        }
    }

    private MaintenanceTicketResponse toResponse(MaintenanceTicket ticket) {
        String roomNumber = roomRepository.findById(ticket.getRoomId())
                .map(Room::getRoomNumber)
                .orElse("?");
        return new MaintenanceTicketResponse(
                ticket.getId(),
                ticket.getHotelId(),
                ticket.getRoomId(),
                roomNumber,
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getReportedBy(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}
