package com.booka.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "booka.groq")
@Getter
@Setter
public class GroqProperties {
    private String apiKey;
    private String baseUrl;
    private String model;
}
