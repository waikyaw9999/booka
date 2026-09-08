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
     * Comma-separated CORS origin <em>patterns</em> (Spring {@code allowedOriginPatterns}).
     * Supports wildcards such as {@code https://*.juneko.online} and {@code http://localhost:*}.
     * Env: {@code CORS_ALLOWED_ORIGINS}.
     */
    private String allowedOriginPatterns =
            "https://booka.juneko.online,https://*.juneko.online,http://localhost:*,http://127.0.0.1:*";

    public List<String> resolvedOriginPatterns() {
        return Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
