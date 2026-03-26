package com.smartpark.auth.service;

import com.smartpark.auth.entity.TokenBlacklist;
import com.smartpark.auth.jwt.JwtService;
import com.smartpark.auth.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class BlacklistService {

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtService jwtService;

    @Transactional
    public void blacklist(String token) {
        if (isBlacklisted(token)) {
            return;
        }

        Date expiration = jwtService.extractExpiration(token);
        TokenBlacklist blacklist = TokenBlacklist.builder()
                .token(token)
                .expiryDate(new java.sql.Timestamp(expiration.getTime()).toLocalDateTime())
                .build();

        tokenBlacklistRepository.save(blacklist);
    }

    public boolean isBlacklisted(String token) {
        return tokenBlacklistRepository.existsByToken(token);
    }

    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    @Transactional
    public void cleanupExpiredTokens() {
        tokenBlacklistRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
