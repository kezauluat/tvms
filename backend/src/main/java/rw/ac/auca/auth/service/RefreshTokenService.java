package rw.ac.auca.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rw.ac.auca.auth.domain.RefreshToken;
import rw.ac.auca.auth.domain.User;
import rw.ac.auca.auth.repository.RefreshTokenRepository;
import rw.ac.auca.exception.UnauthorizedException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    public String create(User user) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID() + "." + UUID.randomUUID())
                .expiresAt(Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS))
                .build();
        return refreshTokenRepository.save(token).getToken();
    }

    /** One-time use: the old token is deleted and a new pair is issued (token rotation). */
    public User consume(String token) {
        RefreshToken stored = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        refreshTokenRepository.delete(stored);
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired, please log in again");
        }
        return stored.getUser();
    }

    public void revoke(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }
}
