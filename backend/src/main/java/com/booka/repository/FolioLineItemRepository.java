package com.booka.repository;

import com.booka.domain.entity.FolioLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolioLineItemRepository extends JpaRepository<FolioLineItem, Long> {
    List<FolioLineItem> findByFolioIdOrderByPostedAtAsc(Long folioId);

    Optional<FolioLineItem> findByFolioIdAndExternalRef(Long folioId, String externalRef);
}
