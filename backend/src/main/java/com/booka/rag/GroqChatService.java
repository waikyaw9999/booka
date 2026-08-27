package com.booka.rag;

import com.booka.config.GroqProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroqChatService {

    private final RestClient restClient;
    private final GroqProperties groqProperties;

    public boolean isConfigured() {
        return groqProperties.getApiKey() != null && !groqProperties.getApiKey().isBlank();
    }

    public String chat(String systemPrompt, List<Map<String, String>> messages) {
        if (!isConfigured()) {
            return null;
        }

        var allMessages = new java.util.ArrayList<Map<String, String>>();
        allMessages.add(Map.of("role", "system", "content", systemPrompt));
        allMessages.addAll(messages);

        try {
            ChatResponse response = restClient.post()
                    .uri(groqProperties.getBaseUrl() + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + groqProperties.getApiKey())
                    .body(Map.of(
                            "model", groqProperties.getModel(),
                            "messages", allMessages,
                            "temperature", 0.2,
                            "max_tokens", 1024
                    ))
                    .retrieve()
                    .body(ChatResponse.class);

            if (response == null || response.choices == null || response.choices.isEmpty()) {
                return null;
            }
            return response.choices.get(0).message.content;
        } catch (RestClientException e) {
            log.warn("Groq API call failed: {}", e.getMessage());
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ChatResponse {
        public List<Choice> choices;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Choice {
        public Message message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Message {
        public String content;
    }
}
