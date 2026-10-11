package rw.ac.auca.admin.dto;

import jakarta.validation.constraints.*;

/** Creates the officer record and the login account in one step. */
public record CreateOfficerAccountRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Size(max = 20) String badgeNumber,
        @Size(max = 100) String station,
        @NotBlank @Pattern(regexp = "^(\\+2507|07)[2389]\\d{7}$", message = "must be a valid Rwandan number")
        String phoneNumber,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(min = 8, max = 72) String password
) {}
