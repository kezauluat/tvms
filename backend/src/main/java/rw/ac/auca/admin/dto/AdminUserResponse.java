package rw.ac.auca.admin.dto;

import rw.ac.auca.auth.domain.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserResponse(UUID id, String fullName, String email, String role, String provider,
                                boolean enabled, LocalDateTime createdAt) {
    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole().name(),
                u.getProvider().name(), u.isEnabled(), u.getCreatedAt());
    }
}
