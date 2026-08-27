package com.booka.repository;

import com.booka.domain.entity.RagChunk;
import com.booka.domain.enums.RagAudience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RagChunkRepository extends JpaRepository<RagChunk, Long> {

    @Query("""
            SELECT c FROM RagChunk c
            WHERE c.audience IN :audiences
            AND (:hotelId IS NULL OR c.hotelId IS NULL OR c.hotelId = :hotelId)
            """)
    List<RagChunk> findByAudiencesAndHotel(
            @Param("audiences") List<RagAudience> audiences,
            @Param("hotelId") Long hotelId);
}
