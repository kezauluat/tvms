package rw.ac.auca.auth.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** Google users finish their driver profile once before using the portal. */
public record CompleteProfileRequest(
        @NotBlank @Size(max = 30) String licenseNumber,
        @NotNull @Past LocalDate dateOfBirth,
        @NotBlank @Pattern(regexp = "^(\\+2507|07)[2389]\\d{7}$", message = "must be a valid Rwandan number")
        String phoneNumber,
        @Size(max = 255) String address
) {}
