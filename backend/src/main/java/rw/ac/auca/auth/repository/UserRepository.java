package rw.ac.auca.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.auth.domain.Role;
import rw.ac.auca.auth.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(Role role);
}
