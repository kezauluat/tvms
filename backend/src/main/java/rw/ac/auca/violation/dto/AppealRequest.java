package rw.ac.auca.violation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AppealRequest(@NotBlank @Size(min = 20, max = 1000, message = "explain in 20 to 1000 characters") String reason) {}
