package rw.ac.auca.fine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Simulated Mobile Money payment: only the payer's phone number is needed. */
public record PayFineRequest(
        @NotBlank @Pattern(regexp = "^(\\+2507|07)[2389]\\d{7}$", message = "must be a valid Rwandan mobile number")
        String phoneNumber
) {}
