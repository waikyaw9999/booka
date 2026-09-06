package com.booka.domain.entity;

import com.booka.domain.enums.FolioLineType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "folio_line_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolioLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "folio_id", nullable = false)
    private Long folioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FolioLineType type;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "external_ref", length = 128)
    private String externalRef;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;
}
