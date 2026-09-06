package com.booka.repository;

import com.booka.domain.entity.Folio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FolioRepository extends JpaRepository<Folio, Long> {
    Optional<Folio> findByBookingId(Long bookingId);
}
