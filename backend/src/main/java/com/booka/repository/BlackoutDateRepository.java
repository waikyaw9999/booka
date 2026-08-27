package com.booka.repository;

import com.booka.domain.entity.BlackoutDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BlackoutDateRepository extends JpaRepository<BlackoutDate, Long> {

    List<BlackoutDate> findByHotelIdOrderByStartDateAsc(Long hotelId);

    @Query("""
            SELECT b FROM BlackoutDate b
            WHERE b.hotelId = :hotelId
            AND (b.roomTypeId IS NULL OR b.roomTypeId = :roomTypeId)
            AND b.startDate < :checkOut
            AND b.endDate > :checkIn
            """)
    List<BlackoutDate> findOverlappingForRoomType(
            @Param("hotelId") Long hotelId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut);

    @Query("""
            SELECT b FROM BlackoutDate b
            WHERE b.hotelId = :hotelId
            AND b.startDate < :toDate
            AND b.endDate > :fromDate
            """)
    List<BlackoutDate> findByHotelAndDateRange(
            @Param("hotelId") Long hotelId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT COUNT(b) > 0 FROM BlackoutDate b
            WHERE b.hotelId = :hotelId
            AND ((b.roomTypeId IS NULL AND :roomTypeId IS NULL) OR b.roomTypeId = :roomTypeId)
            AND b.startDate < :endDate
            AND b.endDate > :startDate
            AND (:excludeId IS NULL OR b.id <> :excludeId)
            """)
    boolean existsOverlappingScope(
            @Param("hotelId") Long hotelId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId);
}
