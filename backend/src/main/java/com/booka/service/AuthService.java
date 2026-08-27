package com.booka.service;

import com.booka.domain.entity.User;
import com.booka.domain.enums.UserRole;
import com.booka.dto.AuthDtos.*;
import com.booka.exception.BusinessException;
import com.booka.security.BookaUserDetails;
import com.booka.security.JwtTokenProvider;
import com.booka.repository.UserRepository;
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

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        BookaUserDetails userDetails = (BookaUserDetails) authentication.getPrincipal();
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
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(UserRole.GUEST)
                .build();
        userRepository.save(user);
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole(), null);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole(), null);
    }

    public UserResponse getCurrentUser(BookaUserDetails userDetails) {
        return new UserResponse(userDetails.getId(), userDetails.getEmail(),
                userDetails.getFullName(), userDetails.getRole(), userDetails.getHotelId());
    }
}
