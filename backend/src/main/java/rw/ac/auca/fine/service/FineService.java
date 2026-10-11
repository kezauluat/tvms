package rw.ac.auca.fine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.audit.AuditService;
import rw.ac.auca.base.ReferenceGenerator;
import rw.ac.auca.driver.domain.Driver;
import rw.ac.auca.exception.BusinessValidationException;
import rw.ac.auca.exception.ConflictException;
import rw.ac.auca.exception.ForbiddenException;
import rw.ac.auca.exception.ResourceNotFoundException;
import rw.ac.auca.fine.domain.Fine;
import rw.ac.auca.fine.domain.FineStatus;
import rw.ac.auca.fine.dto.PayFineRequest;
import rw.ac.auca.fine.dto.PaymentResponse;
import rw.ac.auca.fine.dto.PublicFineResponse;
import rw.ac.auca.fine.repository.FineRepository;
import rw.ac.auca.messaging.NotificationEvent;
import rw.ac.auca.security.AuthUser;
import rw.ac.auca.vehicle.service.VehicleServiceImpl;
import rw.ac.auca.violation.domain.AppealStatus;
import rw.ac.auca.violation.domain.Violation;
import rw.ac.auca.violation.domain.ViolationStatus;
import rw.ac.auca.violation.repository.ViolationRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FineService {

    /** 10% late penalty, added once when a fine becomes overdue. */
    public static final BigDecimal LATE_PENALTY_RATE = new BigDecimal("0.10");
    public static final int REMINDER_DAYS_BEFORE_DUE = 3;

    private final FineRepository fineRepository;
    private final ViolationRepository violationRepository;
    private final ApplicationEventPublisher events;
    private final AuditService auditService;

    /**
     * Simulated Mobile Money payment. Only the responsible driver or the vehicle owner can pay,
     * a pending appeal blocks payment, and the @Version column stops double payment.
     */
    @CacheEvict(cacheNames = "adminStats", allEntries = true)
    @Transactional
    public PaymentResponse pay(AuthUser user, UUID fineId, PayFineRequest req) {
        Fine fine = fineRepository.findDetailedById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine not found with id: " + fineId));
        Violation v = fine.getViolation();
        if (!belongsTo(v, user.id())) {
            throw new ForbiddenException("This fine does not belong to you");
        }
        if (fine.getStatus() == FineStatus.PAID) {
            throw new ConflictException("This fine is already paid (reference " + fine.getPaymentReference() + ")");
        }
        if (!fine.isPayable()) {
            throw new BusinessValidationException("This fine was cancelled and does not need to be paid");
        }
        if (v.getAppealStatus() == AppealStatus.PENDING) {
            throw new BusinessValidationException("Your appeal is being reviewed. Wait for the decision before paying.");
        }
        BigDecimal amountPaid = fine.totalDue();
        fine.setStatus(FineStatus.PAID);
        fine.setPaidAt(LocalDateTime.now());
        fine.setPayerPhone(req.phoneNumber());
        fine.setPaymentReference(ReferenceGenerator.paymentReference());
        fineRepository.saveAndFlush(fine);

        events.publishEvent(NotificationEvent.of(NotificationEvent.FINE_PAID, v, fine,
                "Payment reference: " + fine.getPaymentReference() + ". Thank you for paying on time."));
        auditService.log(user.id(), "PAY_FINE", "Fine", fine.getId());
        return new PaymentResponse(fine.getId(), v.getReferenceNumber(), fine.getPaymentReference(),
                amountPaid, fine.getPaidAt(), fine.getStatus().name());
    }

    /** Daily job: unpaid fines past their due date become OVERDUE with a 10% penalty. */
    @CacheEvict(cacheNames = "adminStats", allEntries = true)
    @Transactional
    public int markOverdueFines() {
        List<Fine> candidates = fineRepository.findOverdueCandidates(FineStatus.UNPAID, LocalDate.now(), AppealStatus.PENDING);
        for (Fine fine : candidates) {
            fine.setStatus(FineStatus.OVERDUE);
            fine.setPenaltyAmount(fine.getAmount().multiply(LATE_PENALTY_RATE).setScale(2, RoundingMode.HALF_UP));
            events.publishEvent(NotificationEvent.of(NotificationEvent.FINE_OVERDUE, fine.getViolation(), fine,
                    "A 10% late penalty was added. Pay now to avoid further action."));
        }
        if (!candidates.isEmpty()) {
            log.info("Marked {} fines as overdue", candidates.size());
        }
        return candidates.size();
    }

    /** Daily job: remind drivers 3 days before the due date. */
    @Transactional(readOnly = true)
    public int sendDueReminders() {
        List<Fine> due = fineRepository.findByStatusAndDueDate(FineStatus.UNPAID,
                LocalDate.now().plusDays(REMINDER_DAYS_BEFORE_DUE));
        due.forEach(f -> events.publishEvent(NotificationEvent.of(NotificationEvent.FINE_REMINDER, f.getViolation(), f,
                "Your fine is due in " + REMINDER_DAYS_BEFORE_DUE + " days.")));
        return due.size();
    }

    /** Public lookup by plate: outstanding fines only, no personal data. */
    @Transactional(readOnly = true)
    public List<PublicFineResponse> outstandingByPlate(String plateNumber) {
        String plate = VehicleServiceImpl.normalizePlate(plateNumber);
        return violationRepository.findByVehiclePlateNumberAndStatusOrderByOccurredAtDesc(plate, ViolationStatus.ACTIVE)
                .stream()
                .filter(v -> v.getFine() != null && v.getFine().isPayable())
                .map(v -> new PublicFineResponse(v.getReferenceNumber(), plate, v.getType().getName(), v.getLocation(),
                        v.getOccurredAt(), v.getFine().totalDue(), v.getFine().getDueDate(),
                        v.getFine().getStatus().name(), v.getAppealStatus().name()))
                .toList();
    }

    private static boolean belongsTo(Violation v, UUID userId) {
        Driver driver = v.getDriver();
        Driver owner = v.getVehicle().getOwner();
        return (driver.getUser() != null && driver.getUser().getId().equals(userId))
                || (owner != null && owner.getUser() != null && owner.getUser().getId().equals(userId));
    }
}
