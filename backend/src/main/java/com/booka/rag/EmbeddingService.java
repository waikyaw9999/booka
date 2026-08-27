package com.booka.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EmbeddingService {

    private static final int DIMENSIONS = 384;
    private final ObjectMapper objectMapper;

    public float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        if (text == null || text.isBlank()) {
            return vector;
        }
        String[] tokens = text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .split("\\s+");
        for (String token : tokens) {
            if (token.isBlank()) continue;
            int index = Math.floorMod(token.hashCode(), DIMENSIONS);
            vector[index] += 1.0f;
        }
        return normalize(vector);
    }

    public String serialize(float[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize embedding", e);
        }
    }

    public float[] deserialize(String json) {
        if (json == null || json.isBlank()) {
            return new float[DIMENSIONS];
        }
        try {
            return objectMapper.readValue(json, float[].class);
        } catch (JsonProcessingException e) {
            return embed(json);
        }
    }

    public double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public double keywordScore(String query, String content) {
        Set<String> queryTokens = tokenize(query);
        Set<String> contentTokens = tokenize(content);
        if (queryTokens.isEmpty()) return 0;
        long matches = queryTokens.stream().filter(contentTokens::contains).count();
        return (double) matches / queryTokens.size();
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("\\s+"))
                .filter(t -> t.length() > 2)
                .collect(Collectors.toSet());
    }

    private float[] normalize(float[] vector) {
        double norm = 0;
        for (float v : vector) norm += v * v;
        norm = Math.sqrt(norm);
        if (norm == 0) return vector;
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }
        return normalized;
    }
}
