package com.booka.rag;

import com.booka.config.RagProperties;
import com.booka.domain.entity.RagChunk;
import com.booka.domain.entity.RagDocument;
import com.booka.domain.enums.RagAudience;
import com.booka.repository.RagChunkRepository;
import com.booka.repository.RagDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RagRetrievalService {

    private final RagChunkRepository ragChunkRepository;
    private final RagDocumentRepository ragDocumentRepository;
    private final EmbeddingService embeddingService;
    private final RagProperties ragProperties;

    public record RetrievedChunk(
            RagChunk chunk,
            RagDocument document,
            double score
    ) {}

    public List<RetrievedChunk> retrieve(String query, Set<RagAudience> audiences, Long hotelId) {
        float[] queryEmbedding = embeddingService.embed(query);
        List<RagChunk> candidates = ragChunkRepository.findByAudiencesAndHotel(
                new ArrayList<>(audiences), hotelId);

        Map<Long, RagDocument> documents = new HashMap<>();
        ragDocumentRepository.findAll().forEach(d -> documents.put(d.getId(), d));

        return candidates.stream()
                .map(chunk -> {
                    float[] chunkEmbedding = chunk.getEmbedding() != null
                            ? embeddingService.deserialize(chunk.getEmbedding())
                            : embeddingService.embed(chunk.getContent());
                    double semantic = embeddingService.cosineSimilarity(queryEmbedding, chunkEmbedding);
                    double keyword = embeddingService.keywordScore(query, chunk.getContent());
                    double score = (semantic * 0.7) + (keyword * 0.3);
                    RagDocument doc = documents.get(chunk.getDocumentId());
                    return new RetrievedChunk(chunk, doc, score);
                })
                .filter(r -> r.score >= ragProperties.getSimilarityThreshold())
                .sorted(Comparator.comparingDouble(RetrievedChunk::score).reversed())
                .limit(ragProperties.getTopK())
                .toList();
    }
}
