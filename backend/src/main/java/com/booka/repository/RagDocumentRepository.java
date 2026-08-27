package com.booka.repository;

import com.booka.domain.entity.RagDocument;
import com.booka.domain.enums.RagAudience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RagDocumentRepository extends JpaRepository<RagDocument, Long> {

    @Query("""
            SELECT d FROM RagDocument d
            WHERE d.audience = :audience
            AND (:hotelId IS NULL OR d.hotelId IS NULL OR d.hotelId = :hotelId)
            """)
    List<RagDocument> findByAudienceAndHotelId(
            @Param("audience") RagAudience audience,
            @Param("hotelId") Long hotelId);
}
