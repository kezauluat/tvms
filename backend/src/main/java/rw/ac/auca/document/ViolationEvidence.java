package rw.ac.auca.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Evidence captured at the roadside. Stored in MongoDB because every offence type carries
 * different data (speeding: recorded speed + limit, drunk driving: alcohol level, ...).
 */
@Document("violation_evidence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViolationEvidence {

    @Id
    private String id;

    /** UUID of the PostgreSQL violation row (stored as text). */
    @Indexed(unique = true)
    private String violationId;

    private String violationReference;
    private String officerBadge;
    private String notes;
    private Double latitude;
    private Double longitude;

    @Builder.Default
    private List<String> photoUrls = new ArrayList<>();

    /** Offence-specific measurements, e.g. {"recordedSpeedKmh": "92", "speedLimitKmh": "60"}. */
    @Builder.Default
    private Map<String, String> details = new LinkedHashMap<>();

    @Builder.Default
    private Instant capturedAt = Instant.now();
}
