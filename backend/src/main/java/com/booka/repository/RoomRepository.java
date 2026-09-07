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

    boolean existsByRoomTypeIdAndRoomNumberIgnoreCase(Long roomTypeId, String roomNumber);

    @Query("""
            SELECT r FROM Room r
            JOIN RoomType rt ON rt.id = r.roomTypeId
            WHERE rt.hotelId = :hotelId
            ORDER BY r.roomNumber
            """)
    List<Room> findByHotelId(@Param("hotelId") Long hotelId);

    @Query("""
            SELECT r FROM Room r
            WHERE r.roomTypeId = :roomTypeId
            AND r.status <> com.booka.domain.enums.RoomStatus.MAINTENANCE
            AND r.id NOT IN (
                SELECT b.roomId FROM Booking b
                WHERE b.roomId IS NOT NULL
                AND (:excludeBookingId IS NULL OR b.id <> :excludeBookingId)
                AND b.status NOT IN (
                    com.booka.domain.enums.BookingStatus.CANCELLED,
                    com.booka.domain.enums.BookingStatus.CHECKED_OUT)
                AND b.checkInDate < :checkOut
                AND b.checkOutDate > :checkIn
            )
            """)
    List<Room> findAvailableRooms(
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("excludeBookingId") Long excludeBookingId);

    default List<Room> findAvailableRooms(Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        return findAvailableRooms(roomTypeId, checkIn, checkOut, null);
    }

    List<Room> findByStatus(RoomStatus status);
}
