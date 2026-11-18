package com.booka.repository;

import com.booka.domain.entity.Folio;
import com.booka.domain.enums.FolioStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolioRepository extends JpaRepository<Folio, Long> {
    Optional<Folio> findByBookingId(Long bookingId);

    List<Folio> findByHotelIdAndStatus(Long hotelId, FolioStatus status);
}
