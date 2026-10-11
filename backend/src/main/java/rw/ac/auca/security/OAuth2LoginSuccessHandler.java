package rw.ac.auca.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import rw.ac.auca.auth.dto.AuthResponse;
import rw.ac.auca.auth.service.AuthService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** After Google login succeeds, issue our own JWT + refresh token and send the user back to React. */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User googleUser = (OAuth2User) authentication.getPrincipal();
        try {
            AuthResponse tokens = authService.loginWithGoogle(
                    googleUser.getAttribute("email"),
                    googleUser.getAttribute("name"),
                    googleUser.getAttribute("sub"));
            request.getSession().invalidate();
            response.sendRedirect(frontendUrl + "/oauth2/callback#accessToken=" + enc(tokens.accessToken())
                    + "&refreshToken=" + enc(tokens.refreshToken()));
        } catch (RuntimeException e) {
            response.sendRedirect(frontendUrl + "/login?error=oauth");
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
