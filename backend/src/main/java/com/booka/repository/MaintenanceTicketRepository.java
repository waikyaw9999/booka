package com.booka.repository;

import com.booka.domain.entity.MaintenanceTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, Long> {
    List<MaintenanceTicket> findByHotelIdOrderByCreatedAtDesc(Long hotelId);
}
