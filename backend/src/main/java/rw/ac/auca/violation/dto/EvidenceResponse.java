package rw.ac.auca.violation.dto;

import rw.ac.auca.document.ViolationEvidence;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record EvidenceResponse(String notes, Double latitude, Double longitude, List<String> photoUrls,
                               Map<String, String> details, Instant capturedAt) {

    public static EvidenceResponse from(ViolationEvidence e) {
        return new EvidenceResponse(e.getNotes(), e.getLatitude(), e.getLongitude(), e.getPhotoUrls(),
                e.getDetails(), e.getCapturedAt());
    }
}
