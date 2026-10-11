package rw.ac.auca.violation.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sent by an officer at the roadside. driverLicenseNumber is only needed when the person driving
 * is not the registered owner. Evidence fields (notes, GPS, photos, details) go to MongoDB.
 */
public record RecordViolationRequest(
        @NotBlank @Size(max = 15) String plateNumber,
        @NotNull UUID violationTypeId,
        @NotBlank @Size(max = 200) String location,
        LocalDateTime occurredAt,
        @Size(max = 30) String driverLicenseNumber,
        @Size(max = 1000) String notes,
        @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @Size(max = 5) List<@NotBlank @Size(max = 500) String> photoUrls,
        @Size(max = 10) Map<@NotBlank @Size(max = 40) String, @Size(max = 200) String> details
) {}
