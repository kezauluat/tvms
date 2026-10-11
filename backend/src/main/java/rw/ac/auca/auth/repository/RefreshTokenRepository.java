package rw.ac.auca.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.auth.domain.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
}
