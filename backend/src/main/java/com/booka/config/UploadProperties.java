package com.booka.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "booka.upload")
@Getter
@Setter
public class UploadProperties {

    /**
     * Directory for optional local image uploads. Env: {@code BOOKA_UPLOAD_DIR}.
     */
    private String dir = "uploads";

    /**
     * Public URL prefix for uploaded files (served by Spring). Default {@code /uploads}.
     */
    private String publicPath = "/uploads";

    private long maxFileSizeBytes = 5_242_880L;
}
