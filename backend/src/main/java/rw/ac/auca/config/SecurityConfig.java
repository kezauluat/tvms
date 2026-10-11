package rw.ac.auca.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import rw.ac.auca.security.JwtAuthFilter;
import rw.ac.auca.security.JwtService;
import rw.ac.auca.security.OAuth2LoginSuccessHandler;

import java.util.Arrays;
import java.util.List;

/**
 * RBAC layer 1: URL rules per role. Layer 2 is @PreAuthorize on controller methods,
 * layer 3 is ownership checks inside the services (a driver only sees their own fines).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final OAuth2LoginSuccessHandler oauth2SuccessHandler;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.cors-origins}")
    private String[] corsOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                .requestMatchers("/api/public/**", "/actuator/health", "/oauth2/**", "/login/oauth2/**", "/error").permitAll()
                .requestMatchers("/api/admin/**", "/api/officers/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/drivers/**", "/api/vehicles/**").hasAnyRole("ADMIN", "OFFICER")
                .requestMatchers("/api/drivers/**", "/api/vehicles/**").hasRole("ADMIN")
                .requestMatchers("/api/me/**").hasRole("DRIVER")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .oauth2Login(o -> o
                .successHandler(oauth2SuccessHandler)
                .failureHandler((req, res, ex) -> res.sendRedirect(frontendUrl + "/login?error=oauth")))
            .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(corsOrigins).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
