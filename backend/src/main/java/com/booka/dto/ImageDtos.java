package com.booka.dto;

import jakarta.validation.constraints.NotBlank;

public final class ImageDtos {

    private ImageDtos() {}

    public record ImageResponse(
            Long id,
            String url,
            String caption,
            Integer sortOrder,
            boolean primary
    ) {}

    public record AddImageRequest(
            @NotBlank String url,
            String caption,
            Integer sortOrder,
            Boolean primary
    ) {}
}
