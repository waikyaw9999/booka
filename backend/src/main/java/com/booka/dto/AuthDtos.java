package com.booka.dto;

import com.booka.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6) String password,
            @NotBlank String fullName
    ) {}

    public record AuthResponse(
            String token,
            Long userId,
            String email,
            String fullName,
            UserRole role,
            Long hotelId
    ) {}

    public record UserResponse(
            Long id,
            String email,
            String fullName,
            UserRole role,
            Long hotelId
    ) {}
}
