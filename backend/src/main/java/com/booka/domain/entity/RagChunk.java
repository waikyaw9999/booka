package com.booka.domain.entity;

import com.booka.domain.enums.RagAudience;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "rag_chunks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RagChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private String section;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RagAudience audience;

    @Column(name = "hotel_id")
    private Long hotelId;

    @Column(columnDefinition = "TEXT")
    private String embedding;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
