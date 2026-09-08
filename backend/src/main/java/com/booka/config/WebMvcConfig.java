package com.booka.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolute = Path.of(uploadProperties.getDir()).toAbsolutePath().normalize().toUri().toString();
        String pattern = uploadProperties.getPublicPath();
        if (!pattern.endsWith("/")) {
            pattern = pattern + "/";
        }
        registry.addResourceHandler(pattern + "**")
                .addResourceLocations(absolute.endsWith("/") ? absolute : absolute + "/");
    }
}
