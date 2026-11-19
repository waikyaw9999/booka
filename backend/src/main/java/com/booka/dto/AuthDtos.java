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

    /**
     * Self-serve property onboarding: creates a hotel + manager (ADMIN) bound to that hotel.
     */
    public record RegisterPropertyRequest(
            @NotBlank String hotelName,
            String description,
            @NotBlank String address,
            @NotBlank String city,
            @NotBlank String country,
            String amenities,
            String cancellationPolicy,
            @NotBlank @Email String managerEmail,
            @NotBlank @Size(min = 6) String managerPassword,
            @NotBlank String managerFullName
    ) {}

    public record CreateStaffRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6) String password,
            @NotBlank String fullName
    ) {}

    public record ResetStaffPasswordRequest(
            @NotBlank @Size(min = 6) String newPassword
    ) {}

    public record StaffMemberResponse(
            Long id,
            String email,
            String fullName,
            UserRole role,
            Long hotelId,
            boolean active
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
