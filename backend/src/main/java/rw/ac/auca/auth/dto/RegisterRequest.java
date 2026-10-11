package rw.ac.auca.auth.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** Driver self-registration. If the license already exists (added by an admin), the account is linked to it. */
public record RegisterRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "must contain letters and numbers")
        String password,
        @NotBlank @Pattern(regexp = "^(\\+2507|07)[2389]\\d{7}$", message = "must be a valid Rwandan number")
        String phoneNumber,
        @NotBlank @Size(max = 30) String licenseNumber,
        @NotNull @Past LocalDate dateOfBirth,
        @Size(max = 255) String address
) {}
