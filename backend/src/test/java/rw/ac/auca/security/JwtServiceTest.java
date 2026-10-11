package rw.ac.auca.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import rw.ac.auca.TestData;
import rw.ac.auca.auth.domain.Role;
import rw.ac.auca.auth.domain.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService("test-secret-key-that-is-long-enough-for-hs256-signing", 60_000);

    @Test
    void tokenCarriesUserIdAndRole() {
        User officer = TestData.user(Role.OFFICER);
        Jwt jwt = jwtService.decode(jwtService.generateAccessToken(officer));

        assertThat(jwt.getSubject()).isEqualTo(officer.getEmail());
        assertThat(jwt.getClaimAsString("uid")).isEqualTo(officer.getId().toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("OFFICER");
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtService attacker = new JwtService("a-completely-different-secret-key-for-hs256-signing!!", 60_000);
        String forged = attacker.generateAccessToken(TestData.user(Role.ADMIN));

        assertThatThrownBy(() -> jwtService.decode(forged)).isInstanceOf(JwtException.class);
    }
}
