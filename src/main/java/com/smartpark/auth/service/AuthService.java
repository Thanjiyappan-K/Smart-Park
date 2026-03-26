package com.smartpark.auth.service;

import com.smartpark.auth.dto.AuthResponse;
import com.smartpark.auth.dto.LoginRequest;
import com.smartpark.auth.dto.RegisterRequest;
import com.smartpark.auth.entity.RefreshToken;
import com.smartpark.auth.jwt.JwtService;
import com.smartpark.auth.repository.RefreshTokenRepository;
import com.smartpark.user.entity.User;
import com.smartpark.user.enums.Role;
import com.smartpark.user.enums.UserStatus;
import com.smartpark.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private static final long REFRESH_TOKEN_EXPIRATION_DAYS = 7;

    @Transactional
    public void registerDriver(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone already exists");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.DRIVER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
    }

    @Transactional
    public void registerOwner(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone already exists");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.PARKING_OWNER)
                .status(UserStatus.PENDING_VERIFICATION)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmailOrPhone())
                .or(() -> userRepository.findByPhone(request.getEmailOrPhone()))
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new RuntimeException("User is blocked");
        }

        String token = jwtService.generateToken(user);
        String refreshToken = generateRefreshToken(user);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired");
        }

        User user = refreshToken.getUser();

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new RuntimeException("User is blocked");
        }

        // Generate new JWT
        String newToken = jwtService.generateToken(user);

        // Optionally rotate refresh token
        refreshTokenRepository.delete(refreshToken);
        String newRefreshToken = generateRefreshToken(user);

        return AuthResponse.builder()
                .token(newToken)
                .refreshToken(newRefreshToken)
                .role(user.getRole().name())
                .build();
    }

    private String generateRefreshToken(User user) {
        // Delete existing refresh token for user
        refreshTokenRepository.findByUser(user).ifPresent(refreshTokenRepository::delete);

        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiryDate(LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRATION_DAYS))
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }
}
