package com.booka.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "booka.rag")
@Getter
@Setter
public class RagProperties {
    private int topK = 5;
    private double similarityThreshold = 0.3;
    private int publicRateLimitPerMinute = 20;
    private int staffRateLimitPerMinute = 60;
}
