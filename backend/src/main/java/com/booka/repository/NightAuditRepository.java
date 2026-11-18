package com.booka.repository;

import com.booka.domain.entity.NightAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NightAuditRepository extends JpaRepository<NightAudit, Long> {
    Optional<NightAudit> findByHotelIdAndBusinessDate(Long hotelId, LocalDate businessDate);

    List<NightAudit> findTop14ByHotelIdOrderByBusinessDateDesc(Long hotelId);
}
