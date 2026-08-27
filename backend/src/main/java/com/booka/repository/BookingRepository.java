package com.booka.repository;

import com.booka.domain.entity.Booking;
import com.booka.domain.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Page<Booking> findByHotelId(Long hotelId, Pageable pageable);

    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.hotelId = :hotelId
            AND b.status IN :statuses
            AND b.checkInDate = :date
            """)
    List<Booking> findUpcomingCheckIns(
            @Param("hotelId") Long hotelId,
            @Param("date") LocalDate date,
            @Param("statuses") List<BookingStatus> statuses);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.hotelId = :hotelId
            AND b.status = com.booka.domain.enums.BookingStatus.CHECKED_IN
            AND b.checkOutDate = :date
            """)
    List<Booking> findUpcomingCheckOuts(
            @Param("hotelId") Long hotelId,
            @Param("date") LocalDate date);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.hotelId = :hotelId
            AND b.checkInDate >= :from
            AND b.checkInDate < :to
            ORDER BY b.checkInDate ASC
            """)
    List<Booking> findByHotelIdAndCheckInRange(
            @Param("hotelId") Long hotelId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.hotelId = :hotelId
            AND b.checkInDate >= :from
            AND b.checkInDate < :to
            AND b.status <> com.booka.domain.enums.BookingStatus.CANCELLED
            """)
    long countActiveByHotelIdAndCheckInRange(
            @Param("hotelId") Long hotelId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.roomId = :roomId
            AND b.status NOT IN (com.booka.domain.enums.BookingStatus.CANCELLED, com.booka.domain.enums.BookingStatus.CHECKED_OUT)
            AND b.checkInDate < :checkOut
            AND b.checkOutDate > :checkIn
            AND (:excludeId IS NULL OR b.id <> :excludeId)
            """)
    boolean existsOverlappingBooking(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("excludeId") Long excludeId);
}
