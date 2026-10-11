package rw.ac.auca.security;

import rw.ac.auca.auth.domain.Role;

import java.util.UUID;

/** The logged-in user, read from the JWT on every request (no database lookup). */
public record AuthUser(UUID id, String email, Role role) {

    public boolean is(Role r) {
        return role == r;
    }
}
