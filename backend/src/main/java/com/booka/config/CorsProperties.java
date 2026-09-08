package com.booka.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "booka.cors")
@Getter
@Setter
public class CorsProperties {

    /**
     * Comma-separated browser origins allowed by CORS
     * (e.g. {@code https://booka.juneko.online,http://localhost:5173}).
     */
    private String allowedOrigins =
            "http://localhost:5173,http://localhost:3000,http://localhost:80";

    public List<String> resolvedOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
