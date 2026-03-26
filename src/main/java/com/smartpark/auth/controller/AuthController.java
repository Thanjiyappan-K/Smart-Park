package com.smartpark.auth.controller;

import com.smartpark.auth.dto.AuthResponse;
import com.smartpark.auth.dto.LoginRequest;
import com.smartpark.auth.dto.RefreshTokenRequest;
import com.smartpark.auth.dto.RegisterRequest;
import com.smartpark.auth.service.AuthService;
import com.smartpark.auth.service.BlacklistService;
import com.smartpark.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final BlacklistService blacklistService;

    @PostMapping("/register/driver")
    public ResponseEntity<ApiResponse<String>> registerDriver(@RequestBody @Valid RegisterRequest request) {
        authService.registerDriver(request);
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .message("Driver registered successfully")
                .data(null)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register/owner")
    public ResponseEntity<ApiResponse<String>> registerOwner(@RequestBody @Valid RegisterRequest request) {
        authService.registerOwner(request);
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .message("Owner registered, pending verification")
                .data(null)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody @Valid LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Login successful")
                .data(authResponse)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        AuthResponse authResponse = authService.refreshToken(request.getRefreshToken());
        ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Token refreshed successfully")
                .data(authResponse)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            blacklistService.blacklist(token);
        }

        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .message("Logged out successfully")
                .data(null)
                .build();
        return ResponseEntity.ok(response);
    }
}
