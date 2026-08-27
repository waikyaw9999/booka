package com.booka.repository;

import com.booka.domain.entity.Room;
import com.booka.domain.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByRoomTypeId(Long roomTypeId);

    @Query("""
            SELECT r FROM Room r
            WHERE r.roomTypeId = :roomTypeId
            AND r.status <> com.booka.domain.enums.RoomStatus.MAINTENANCE
            AND r.id NOT IN (
                SELECT b.roomId FROM Booking b
                WHERE b.status NOT IN (com.booka.domain.enums.BookingStatus.CANCELLED, com.booka.domain.enums.BookingStatus.CHECKED_OUT)
                AND b.checkInDate < :checkOut
                AND b.checkOutDate > :checkIn
            )
            """)
    List<Room> findAvailableRooms(
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut);

    List<Room> findByStatus(RoomStatus status);
}
