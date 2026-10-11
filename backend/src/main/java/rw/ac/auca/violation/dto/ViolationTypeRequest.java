package rw.ac.auca.violation.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ViolationTypeRequest(
        @NotBlank @Size(max = 40) @Pattern(regexp = "^[A-Za-z_]+$", message = "letters and underscores only")
        String code,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 255) String description,
        @NotNull @DecimalMin(value = "1000", message = "must be at least 1,000 RWF") BigDecimal fineAmount,
        @NotNull @Min(0) @Max(12) Integer demeritPoints,
        Boolean active
) {}
