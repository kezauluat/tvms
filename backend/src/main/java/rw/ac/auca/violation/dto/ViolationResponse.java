package rw.ac.auca.violation.dto;

import rw.ac.auca.fine.domain.Fine;
import rw.ac.auca.violation.domain.Violation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Flat view of a violation and its fine, used by every list in the UI. */
public record ViolationResponse(
        UUID id,
        String referenceNumber,
        String plateNumber,
        String vehicleDescription,
        UUID driverId,
        String driverName,
        String driverLicenseNumber,
        String officerName,
        String officerBadge,
        UUID violationTypeId,
        String violationName,
        Integer demeritPoints,
        String location,
        LocalDateTime occurredAt,
        String status,
        String appealStatus,
        String appealReason,
        String appealDecisionNote,
        UUID fineId,
        BigDecimal fineAmount,
        BigDecimal penaltyAmount,
        BigDecimal totalDue,
        LocalDate dueDate,
        String fineStatus,
        String paymentReference,
        LocalDateTime paidAt
) {
    public static ViolationResponse from(Violation v) {
        Fine f = v.getFine();
        var vehicle = v.getVehicle();
        return new ViolationResponse(
                v.getId(), v.getReferenceNumber(),
                vehicle.getPlateNumber(), vehicle.getMake() + " " + vehicle.getModel() + " (" + vehicle.getYear() + ")",
                v.getDriver().getId(), v.getDriver().getFullName(), v.getDriver().getLicenseNumber(),
                v.getOfficer().getFullName(), v.getOfficer().getBadgeNumber(),
                v.getType().getId(), v.getType().getName(), v.getType().getDemeritPoints(),
                v.getLocation(), v.getOccurredAt(),
                v.getStatus().name(), v.getAppealStatus().name(), v.getAppealReason(), v.getAppealDecisionNote(),
                f == null ? null : f.getId(),
                f == null ? null : f.getAmount(),
                f == null ? null : f.getPenaltyAmount(),
                f == null ? null : f.totalDue(),
                f == null ? null : f.getDueDate(),
                f == null ? null : f.getStatus().name(),
                f == null ? null : f.getPaymentReference(),
                f == null ? null : f.getPaidAt());
    }
}
