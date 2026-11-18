package com.booka.repository;

import com.booka.domain.entity.FolioLineItem;
import com.booka.domain.enums.FolioLineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FolioLineItemRepository extends JpaRepository<FolioLineItem, Long> {
    List<FolioLineItem> findByFolioIdOrderByPostedAtAsc(Long folioId);

    Optional<FolioLineItem> findByFolioIdAndExternalRef(Long folioId, String externalRef);

    @Query("""
            SELECT li FROM FolioLineItem li, Folio f
            WHERE li.folioId = f.id
            AND f.hotelId = :hotelId
            AND li.type = :type
            AND li.postedAt >= :from
            AND li.postedAt < :to
            """)
    List<FolioLineItem> findForHotelBetween(
            @Param("hotelId") Long hotelId,
            @Param("type") FolioLineType type,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
