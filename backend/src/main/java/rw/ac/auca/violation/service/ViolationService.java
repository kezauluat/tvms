package rw.ac.auca.violation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.audit.AuditService;
import rw.ac.auca.auth.domain.Role;
import rw.ac.auca.base.PageResponse;
import rw.ac.auca.base.ReferenceGenerator;
import rw.ac.auca.document.ViolationEvidence;
import rw.ac.auca.document.repository.ViolationEvidenceRepository;
import rw.ac.auca.driver.domain.Driver;
import rw.ac.auca.driver.repository.DriverRepository;
import rw.ac.auca.exception.BusinessValidationException;
import rw.ac.auca.exception.ForbiddenException;
import rw.ac.auca.exception.ResourceNotFoundException;
import rw.ac.auca.fine.domain.Fine;
import rw.ac.auca.fine.domain.FineStatus;
import rw.ac.auca.fine.repository.FineRepository;
import rw.ac.auca.messaging.NotificationEvent;
import rw.ac.auca.officer.domain.Officer;
import rw.ac.auca.officer.repository.OfficerRepository;
import rw.ac.auca.security.AuthUser;
import rw.ac.auca.vehicle.domain.Vehicle;
import rw.ac.auca.vehicle.repository.VehicleRepository;
import rw.ac.auca.vehicle.service.VehicleServiceImpl;
import rw.ac.auca.violation.domain.AppealStatus;
import rw.ac.auca.violation.domain.Violation;
import rw.ac.auca.violation.domain.ViolationStatus;
import rw.ac.auca.violation.domain.ViolationType;
import rw.ac.auca.violation.dto.*;
import rw.ac.auca.violation.repository.ViolationRepository;
import rw.ac.auca.violation.repository.ViolationTypeRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ViolationService {

    /** Business rules (see the project doc). */
    public static final int FINE_DUE_DAYS = 30;
    public static final int MAX_BACKDATE_DAYS = 7;
    public static final int APPEAL_WINDOW_DAYS = 14;
    public static final int GRACE_DAYS_AFTER_REJECTED_APPEAL = 7;

    private final ViolationRepository violationRepository;
    private final ViolationTypeRepository violationTypeRepository;
    private final FineRepository fineRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final OfficerRepository officerRepository;
    private final ViolationEvidenceRepository evidenceRepository;
    private final ApplicationEventPublisher events;
    private final AuditService auditService;

    /** Officer records an offence: creates the violation, its fine and the roadside evidence. */
    @CacheEvict(cacheNames = "adminStats", allEntries = true)
    @Transactional
    public ViolationResponse record(AuthUser user, RecordViolationRequest req) {
        Officer officer = officerRepository.findByUserId(user.id())
                .orElseThrow(() -> new ForbiddenException("No officer profile is linked to this account"));

        String plate = VehicleServiceImpl.normalizePlate(req.plateNumber());
        Vehicle vehicle = vehicleRepository.findByPlateNumber(plate)
                .orElseThrow(() -> new ResourceNotFoundException("No registered vehicle with plate " + plate));

        ViolationType type = violationTypeRepository.findById(req.violationTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Violation type not found"));
        if (!type.isActive()) {
            throw new BusinessValidationException("This violation type is no longer in use");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime occurredAt = req.occurredAt() == null ? now : req.occurredAt().withSecond(0).withNano(0);
        if (occurredAt.isAfter(now.plusMinutes(5))) {
            throw new BusinessValidationException("The offence time cannot be in the future");
        }
        if (occurredAt.isBefore(now.minusDays(MAX_BACKDATE_DAYS))) {
            throw new BusinessValidationException("Offences must be recorded within " + MAX_BACKDATE_DAYS + " days");
        }

        Driver driver = vehicle.getOwner();
        if (req.driverLicenseNumber() != null && !req.driverLicenseNumber().isBlank()) {
            String license = req.driverLicenseNumber().trim().toUpperCase();
            driver = driverRepository.findByLicenseNumber(license)
                    .orElseThrow(() -> new ResourceNotFoundException("No driver with license " + license));
        }

        Violation violation = violationRepository.save(Violation.builder()
                .referenceNumber(newReference())
                .vehicle(vehicle)
                .driver(driver)
                .officer(officer)
                .type(type)
                .location(req.location().trim())
                .occurredAt(occurredAt)
                .build());

        Fine fine = fineRepository.save(Fine.builder()
                .violation(violation)
                .amount(type.getFineAmount())       // snapshot: later tariff changes do not affect this fine
                .dueDate(LocalDate.now().plusDays(FINE_DUE_DAYS))
                .build());
        violation.setFine(fine);

        if (hasEvidence(req)) {
            evidenceRepository.save(ViolationEvidence.builder()
                    .violationId(violation.getId().toString())
                    .violationReference(violation.getReferenceNumber())
                    .officerBadge(officer.getBadgeNumber())
                    .notes(req.notes())
                    .latitude(req.latitude())
                    .longitude(req.longitude())
                    .photoUrls(req.photoUrls() == null ? List.of() : req.photoUrls())
                    .details(req.details() == null ? java.util.Map.of() : req.details())
                    .build());
        }

        events.publishEvent(NotificationEvent.of(NotificationEvent.VIOLATION_RECORDED, violation, fine,
                "Pay before the due date to avoid a 10% late penalty, or appeal within " + APPEAL_WINDOW_DAYS + " days."));
        auditService.log(user.id(), "RECORD_VIOLATION", "Violation", violation.getId());
        return ViolationResponse.from(violation);
    }

    @Transactional(readOnly = true)
    public PageResponse<ViolationResponse> recordedByMe(AuthUser user, int page, int size) {
        Officer officer = officerRepository.findByUserId(user.id())
                .orElseThrow(() -> new ForbiddenException("No officer profile is linked to this account"));
        return PageResponse.of(violationRepository.findByOfficerId(officer.getId(), pageable(page, size)),
                ViolationResponse::from);
    }

    /** Admin list with optional filters: plate number (partial) or appeal status. */
    @Transactional(readOnly = true)
    public PageResponse<ViolationResponse> search(String plate, AppealStatus appealStatus, int page, int size) {
        Pageable pageable = pageable(page, size);
        if (appealStatus != null) {
            return PageResponse.of(violationRepository.findByAppealStatus(appealStatus, pageable), ViolationResponse::from);
        }
        if (plate != null && !plate.isBlank()) {
            return PageResponse.of(violationRepository.findByVehiclePlateNumberContainingIgnoreCase(
                    VehicleServiceImpl.normalizePlate(plate), pageable), ViolationResponse::from);
        }
        return PageResponse.of(violationRepository.findAll(pageable), ViolationResponse::from);
    }

    /** Driver portal: offences the driver committed and offences on vehicles they own. */
    @Transactional(readOnly = true)
    public List<ViolationResponse> forDriver(AuthUser user) {
        Driver driver = currentDriver(user);
        return violationRepository.findForDriverPortal(driver.getId()).stream().map(ViolationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ViolationDetailResponse get(AuthUser user, UUID id) {
        Violation v = findDetailed(id);
        if (user.is(Role.DRIVER)) {
            ensureDriverCanAccess(user, v);
        }
        EvidenceResponse evidence = evidenceRepository.findByViolationId(v.getId().toString())
                .map(EvidenceResponse::from).orElse(null);
        return new ViolationDetailResponse(ViolationResponse.from(v), evidence);
    }

    /** Driver contests an offence: once, within 14 days, and only while the fine is unpaid. */
    @CacheEvict(cacheNames = "adminStats", allEntries = true)
    @Transactional
    public ViolationResponse appeal(AuthUser user, UUID id, AppealRequest req) {
        Violation v = findDetailed(id);
        ensureDriverCanAccess(user, v);
        if (v.getStatus() != ViolationStatus.ACTIVE) {
            throw new BusinessValidationException("This violation has already been dismissed");
        }
        if (v.getAppealStatus() != AppealStatus.NONE) {
            throw new BusinessValidationException("An appeal was already submitted for this violation");
        }
        if (v.getFine() == null || !v.getFine().isPayable()) {
            throw new BusinessValidationException("Paid or cancelled fines cannot be appealed");
        }
        if (v.getOccurredAt().isBefore(LocalDateTime.now().minusDays(APPEAL_WINDOW_DAYS))) {
            throw new BusinessValidationException("Appeals must be submitted within " + APPEAL_WINDOW_DAYS + " days of the offence");
        }
        v.setAppealStatus(AppealStatus.PENDING);
        v.setAppealReason(req.reason().trim());
        v.setAppealedAt(LocalDateTime.now());
        auditService.log(user.id(), "SUBMIT_APPEAL", "Violation", v.getId());
        return ViolationResponse.from(v);
    }

    /**
     * Admin decides an appeal. Accepted: violation dismissed and fine cancelled.
     * Rejected: the fine stands, with at least 7 more days to pay.
     */
    @CacheEvict(cacheNames = "adminStats", allEntries = true)
    @Transactional
    public ViolationResponse decideAppeal(AuthUser user, UUID id, AppealDecisionRequest req) {
        Violation v = findDetailed(id);
        if (v.getAppealStatus() != AppealStatus.PENDING) {
            throw new BusinessValidationException("There is no pending appeal for this violation");
        }
        Fine fine = v.getFine();
        v.setAppealDecisionNote(req.note().trim());
        v.setAppealDecidedAt(LocalDateTime.now());
        String message;
        if (Boolean.TRUE.equals(req.accepted())) {
            v.setAppealStatus(AppealStatus.ACCEPTED);
            v.setStatus(ViolationStatus.DISMISSED);
            if (fine != null) {
                fine.setStatus(FineStatus.CANCELLED);
            }
            message = "Your appeal was accepted. The fine is cancelled. Note: " + req.note().trim();
        } else {
            v.setAppealStatus(AppealStatus.REJECTED);
            LocalDate minimumDue = LocalDate.now().plusDays(GRACE_DAYS_AFTER_REJECTED_APPEAL);
            if (fine != null && fine.getDueDate().isBefore(minimumDue)) {
                fine.setDueDate(minimumDue);
            }
            message = "Your appeal was rejected. Reason: " + req.note().trim();
        }
        events.publishEvent(NotificationEvent.of(NotificationEvent.APPEAL_DECIDED, v,
                fine != null && fine.isPayable() ? fine : null, message));
        auditService.log(user.id(), v.getAppealStatus() == AppealStatus.ACCEPTED ? "ACCEPT_APPEAL" : "REJECT_APPEAL",
                "Violation", v.getId());
        return ViolationResponse.from(v);
    }

    // ---------- helpers ----------

    private Violation findDetailed(UUID id) {
        return violationRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Violation not found with id: " + id));
    }

    private Driver currentDriver(AuthUser user) {
        return driverRepository.findByUserId(user.id())
                .orElseThrow(() -> new ForbiddenException("Complete your driver profile first"));
    }

    /** A driver may see a violation if they were driving or if they own the vehicle. */
    private void ensureDriverCanAccess(AuthUser user, Violation v) {
        UUID me = user.id();
        boolean wasDriving = v.getDriver().getUser() != null && v.getDriver().getUser().getId().equals(me);
        Driver owner = v.getVehicle().getOwner();
        boolean ownsVehicle = owner != null && owner.getUser() != null && owner.getUser().getId().equals(me);
        if (!wasDriving && !ownsVehicle) {
            throw new ForbiddenException("This violation does not belong to you");
        }
    }

    private String newReference() {
        String reference;
        do {
            reference = ReferenceGenerator.violationReference(LocalDate.now());
        } while (violationRepository.existsByReferenceNumber(reference));
        return reference;
    }

    private static boolean hasEvidence(RecordViolationRequest req) {
        return (req.notes() != null && !req.notes().isBlank())
                || req.latitude() != null || req.longitude() != null
                || (req.photoUrls() != null && !req.photoUrls().isEmpty())
                || (req.details() != null && !req.details().isEmpty());
    }

    private static Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "occurredAt"));
    }
}
