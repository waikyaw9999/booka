package com.booka.repository;

import com.booka.domain.entity.GuestRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuestRequestRepository extends JpaRepository<GuestRequest, Long> {
    List<GuestRequest> findByHotelIdOrderByCreatedAtDesc(Long hotelId);
}
