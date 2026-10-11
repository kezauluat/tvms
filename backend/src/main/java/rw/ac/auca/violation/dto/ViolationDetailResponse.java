package rw.ac.auca.violation.dto;

/** A violation plus its roadside evidence from MongoDB (evidence may be null). */
public record ViolationDetailResponse(ViolationResponse violation, EvidenceResponse evidence) {}
