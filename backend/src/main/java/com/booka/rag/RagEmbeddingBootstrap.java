package com.booka.rag;

import com.booka.repository.RagChunkRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RagEmbeddingBootstrap {

    private final RagChunkRepository ragChunkRepository;
    private final EmbeddingService embeddingService;

    @PostConstruct
    @Transactional
    public void embedSeedChunks() {
        var chunks = ragChunkRepository.findAll().stream()
                .filter(c -> c.getEmbedding() == null || c.getEmbedding().isBlank())
                .toList();
        if (chunks.isEmpty()) return;

        chunks.forEach(chunk -> chunk.setEmbedding(
                embeddingService.serialize(embeddingService.embed(chunk.getContent()))));
        ragChunkRepository.saveAll(chunks);
        log.info("Generated embeddings for {} RAG chunks", chunks.size());
    }
}
