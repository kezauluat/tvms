package rw.ac.auca.violation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AppealDecisionRequest(@NotNull Boolean accepted, @NotBlank @Size(max = 500) String note) {}
