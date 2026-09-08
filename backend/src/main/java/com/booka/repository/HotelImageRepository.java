package com.booka.repository;

import com.booka.domain.entity.HotelImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HotelImageRepository extends JpaRepository<HotelImage, Long> {

    List<HotelImage> findByHotelIdOrderBySortOrderAscIdAsc(Long hotelId);

    Optional<HotelImage> findByIdAndHotelId(Long id, Long hotelId);

    Optional<HotelImage> findFirstByHotelIdAndPrimaryTrue(Long hotelId);

    @Query("SELECT COALESCE(MAX(i.sortOrder), -1) FROM HotelImage i WHERE i.hotelId = :hotelId")
    int findMaxSortOrder(@Param("hotelId") Long hotelId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE HotelImage i SET i.primary = false WHERE i.hotelId = :hotelId")
    void clearPrimaryForHotel(@Param("hotelId") Long hotelId);

    long countByHotelId(Long hotelId);
}
