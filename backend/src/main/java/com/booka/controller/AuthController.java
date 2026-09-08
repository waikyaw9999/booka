package com.booka.controller;

import com.booka.dto.AuthDtos.*;
import com.booka.security.BookaUserDetails;
import com.booka.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    Map<String, String> loginHint() {
        return Map.of(
                "message", "Use POST with JSON body {\"email\",\"password\"} to authenticate.",
                "method", "POST",
                "path", "/api/v1/auth/login");
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal BookaUserDetails user) {
        return authService.getCurrentUser(user);
    }
}
