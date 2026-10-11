package rw.ac.auca.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.audit.AuditService;
import rw.ac.auca.auth.domain.AuthProvider;
import rw.ac.auca.auth.domain.Role;
import rw.ac.auca.auth.domain.User;
import rw.ac.auca.auth.dto.*;
import rw.ac.auca.auth.repository.UserRepository;
import rw.ac.auca.driver.domain.Driver;
import rw.ac.auca.driver.repository.DriverRepository;
import rw.ac.auca.driver.service.DriverService;
import rw.ac.auca.exception.BusinessValidationException;
import rw.ac.auca.exception.ConflictException;
import rw.ac.auca.exception.ForbiddenException;
import rw.ac.auca.exception.ResourceNotFoundException;
import rw.ac.auca.exception.UnauthorizedException;
import rw.ac.auca.security.AuthUser;
import rw.ac.auca.security.JwtService;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final DriverService driverService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = normalizeEmail(req.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        User user = userRepository.save(User.builder()
                .fullName(req.fullName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(Role.DRIVER)
                .provider(AuthProvider.LOCAL)
                .build());
        linkDriverProfile(user, req.licenseNumber(), req.dateOfBirth(), req.phoneNumber(), req.address());
        auditService.log(user.getId(), "REGISTER", "User", user.getId());
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(normalizeEmail(req.email()))
                .filter(u -> u.getPasswordHash() != null && passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!user.isEnabled()) {
            throw new ForbiddenException("Account is disabled. Contact the administrator.");
        }
        auditService.log(user.getId(), "LOGIN", "User", user.getId());
        return issueTokens(user);
    }

    /** First Google login creates a DRIVER account; the driver then completes the profile once. */
    @Transactional
    public AuthResponse loginWithGoogle(String email, String name, String googleId) {
        if (email == null) {
            throw new BusinessValidationException("Google account has no email");
        }
        String normalized = normalizeEmail(email);
        User user = userRepository.findByEmail(normalized).orElseGet(() -> userRepository.save(User.builder()
                .fullName(name != null ? name : normalized)
                .email(normalized)
                .role(Role.DRIVER)
                .provider(AuthProvider.GOOGLE)
                .providerId(googleId)
                .build()));
        if (!user.isEnabled()) {
            throw new ForbiddenException("Account is disabled. Contact the administrator.");
        }
        auditService.log(user.getId(), "LOGIN_GOOGLE", "User", user.getId());
        return issueTokens(user);
    }

    @Transactional
    public UserResponse completeProfile(AuthUser authUser, CompleteProfileRequest req) {
        User user = userRepository.findById(authUser.id()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.DRIVER) {
            throw new ForbiddenException("Only driver accounts have a driver profile");
        }
        if (driverRepository.findByUserId(user.getId()).isPresent()) {
            throw new ConflictException("Your driver profile is already complete");
        }
        linkDriverProfile(user, req.licenseNumber(), req.dateOfBirth(), req.phoneNumber(), req.address());
        return toResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        User user = refreshTokenService.consume(refreshToken);
        if (!user.isEnabled()) {
            throw new ForbiddenException("Account is disabled. Contact the administrator.");
        }
        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    @Transactional(readOnly = true)
    public UserResponse me(AuthUser authUser) {
        return toResponse(userRepository.findById(authUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    /**
     * Links the account to a driver record. If an admin already registered this license, the date of
     * birth must match (identity check); otherwise a new driver record is created with the usual
     * business rules (18+ years old, unique license).
     */
    private void linkDriverProfile(User user, String licenseNumber, LocalDate dateOfBirth, String phone, String address) {
        String license = licenseNumber.trim().toUpperCase();
        Driver driver = driverRepository.findByLicenseNumber(license).orElse(null);
        if (driver != null) {
            if (driver.getUser() != null) {
                throw new ConflictException("This license number already has an online account");
            }
            if (!driver.getDateOfBirth().equals(dateOfBirth)) {
                throw new BusinessValidationException("Date of birth does not match our records for this license");
            }
            driver.setPhoneNumber(phone);
            if (address != null && !address.isBlank()) {
                driver.setAddress(address);
            }
        } else {
            Driver created = new Driver();
            created.setFullName(user.getFullName());
            created.setLicenseNumber(license);
            created.setDateOfBirth(dateOfBirth);
            created.setPhoneNumber(phone);
            created.setAddress(address);
            driver = driverService.registerDriver(created);
        }
        driver.setUser(user);
        driverRepository.save(driver);
    }

    private AuthResponse issueTokens(User user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                refreshTokenService.create(user),
                "Bearer",
                jwtService.getAccessExpirationSeconds(),
                toResponse(user));
    }

    private UserResponse toResponse(User user) {
        boolean profileComplete = user.getRole() != Role.DRIVER
                || driverRepository.findByUserId(user.getId()).isPresent();
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name(),
                user.getProvider().name(), profileComplete);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
