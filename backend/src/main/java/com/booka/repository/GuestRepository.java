package com.booka.repository;

import com.booka.domain.entity.Guest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuestRepository extends JpaRepository<Guest, Long> {
    Optional<Guest> findByEmail(String email);

    @Query("""
            SELECT g FROM Guest g
            WHERE EXISTS (
                SELECT b.id FROM Booking b
                WHERE b.guestId = g.id AND b.hotelId = :hotelId
            )
            AND (
                :q = ''
                OR LOWER(g.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(g.email) LIKE LOWER(CONCAT('%', :q, '%'))
                OR COALESCE(g.phone, '') LIKE CONCAT('%', :q, '%')
            )
            ORDER BY g.fullName
            """)
    List<Guest> searchAtHotel(@Param("hotelId") Long hotelId, @Param("q") String q, Pageable pageable);
}
