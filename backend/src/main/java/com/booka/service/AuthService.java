package com.booka.service;

import com.booka.domain.entity.User;
import com.booka.domain.enums.UserRole;
import com.booka.dto.AuthDtos.*;
import com.booka.dto.HotelDtos.CreateHotelRequest;
import com.booka.exception.BusinessException;
import com.booka.repository.UserRepository;
import com.booka.security.BookaUserDetails;
import com.booka.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final HotelService hotelService;
    private final PropertyStarterService propertyStarterService;
    private final ChannelApiKeyService channelApiKeyService;

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        BookaUserDetails userDetails = (BookaUserDetails) authentication.getPrincipal();
        if (!userDetails.isActive()) {
            throw new BusinessException("This account has been deactivated");
        }
        if ((userDetails.getRole() == UserRole.ADMIN || userDetails.getRole() == UserRole.STAFF)
                && userDetails.getHotelId() == null) {
            throw new BusinessException(
                    "This staff account is not assigned to a property. Contact Booka support.");
        }
        String token = jwtTokenProvider.generateToken(
                userDetails.getId(), userDetails.getEmail(), userDetails.getRole(), userDetails.getHotelId());
        return new AuthResponse(token, userDetails.getId(), userDetails.getEmail(),
                userDetails.getFullName(), userDetails.getRole(), userDetails.getHotelId());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered");
        }
        User user = User.builder()
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .role(UserRole.GUEST)
                .active(true)
                .build();
        userRepository.save(user);
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole(), null);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole(), null);
    }

    /**
     * Sellable onboarding: one property + one manager (ADMIN) bound to that hotel.
     */
    @Transactional
    public AuthResponse registerProperty(RegisterPropertyRequest request) {
        String email = request.managerEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email already registered");
        }

        var hotelResponse = hotelService.createHotel(new CreateHotelRequest(
                request.hotelName().trim(),
                blankToNull(request.description()),
                request.address().trim(),
                request.city().trim(),
                request.country().trim(),
                blankToNull(request.amenities()),
                blankToNull(request.cancellationPolicy())));

        propertyStarterService.seedStarterInventory(hotelResponse.id());
        channelApiKeyService.ensurePropertyChannelKeysFromConfig(hotelResponse.id());

        User manager = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.managerPassword()))
                .fullName(request.managerFullName().trim())
                .role(UserRole.ADMIN)
                .hotelId(hotelResponse.id())
                .active(true)
                .build();
        userRepository.save(manager);

        String token = jwtTokenProvider.generateToken(
                manager.getId(), manager.getEmail(), manager.getRole(), manager.getHotelId());
        return new AuthResponse(
                token,
                manager.getId(),
                manager.getEmail(),
                manager.getFullName(),
                manager.getRole(),
                manager.getHotelId());
    }

    public UserResponse getCurrentUser(BookaUserDetails userDetails) {
        return new UserResponse(userDetails.getId(), userDetails.getEmail(),
                userDetails.getFullName(), userDetails.getRole(), userDetails.getHotelId());
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
