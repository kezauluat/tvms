package rw.ac.auca.violation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import rw.ac.auca.TestData;
import rw.ac.auca.audit.AuditService;
import rw.ac.auca.auth.domain.Role;
import rw.ac.auca.auth.domain.User;
import rw.ac.auca.document.repository.ViolationEvidenceRepository;
import rw.ac.auca.driver.domain.Driver;
import rw.ac.auca.driver.repository.DriverRepository;
import rw.ac.auca.exception.BusinessValidationException;
import rw.ac.auca.exception.ForbiddenException;
import rw.ac.auca.fine.domain.Fine;
import rw.ac.auca.fine.domain.FineStatus;
import rw.ac.auca.fine.repository.FineRepository;
import rw.ac.auca.messaging.NotificationEvent;
import rw.ac.auca.officer.domain.Officer;
import rw.ac.auca.officer.repository.OfficerRepository;
import rw.ac.auca.security.AuthUser;
import rw.ac.auca.vehicle.domain.Vehicle;
import rw.ac.auca.vehicle.repository.VehicleRepository;
import rw.ac.auca.violation.domain.AppealStatus;
import rw.ac.auca.violation.domain.Violation;
import rw.ac.auca.violation.domain.ViolationStatus;
import rw.ac.auca.violation.domain.ViolationType;
import rw.ac.auca.violation.dto.AppealDecisionRequest;
import rw.ac.auca.violation.dto.AppealRequest;
import rw.ac.auca.violation.dto.RecordViolationRequest;
import rw.ac.auca.violation.dto.ViolationResponse;
import rw.ac.auca.violation.repository.ViolationRepository;
import rw.ac.auca.violation.repository.ViolationTypeRepository;
import rw.ac.auca.violation.service.ViolationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViolationServiceTest {

    @Mock ViolationRepository violationRepository;
    @Mock ViolationTypeRepository violationTypeRepository;
    @Mock FineRepository fineRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock DriverRepository driverRepository;
    @Mock OfficerRepository officerRepository;
    @Mock ViolationEvidenceRepository evidenceRepository;
    @Mock ApplicationEventPublisher events;
    @Mock AuditService auditService;
    @InjectMocks ViolationService service;

    private User officerUser;
    private User driverUser;
    private Officer officer;
    private Driver driver;
    private Vehicle vehicle;
    private ViolationType speeding;

    @BeforeEach
    void setUp() {
        officerUser = TestData.user(Role.OFFICER);
        driverUser = TestData.user(Role.DRIVER);
        officer = TestData.officer(officerUser);
        driver = TestData.driver(driverUser);
        vehicle = TestData.vehicle(driver);
        speeding = TestData.speeding();
    }

    private AuthUser asOfficer() {
        return new AuthUser(officerUser.getId(), officerUser.getEmail(), Role.OFFICER);
    }

    private AuthUser asDriver() {
        return new AuthUser(driverUser.getId(), driverUser.getEmail(), Role.DRIVER);
    }

    private RecordViolationRequest request(LocalDateTime when) {
        return new RecordViolationRequest("rab 123a", speeding.getId(), "KN 5 Rd", when, null, null, null, null, null, null);
    }

    private void stubLookups() {
        when(officerRepository.findByUserId(officerUser.getId())).thenReturn(Optional.of(officer));
        when(vehicleRepository.findByPlateNumber("RAB123A")).thenReturn(Optional.of(vehicle));
        when(violationTypeRepository.findById(speeding.getId())).thenReturn(Optional.of(speeding));
    }

    @Test
    void recordCreatesFineWithSnapshotAmountAndDueDateAndNotifies() {
        stubLookups();
        when(violationRepository.save(any(Violation.class))).thenAnswer(inv -> {
            Violation v = inv.getArgument(0);
            v.setId(java.util.UUID.randomUUID());
            return v;
        });
        when(fineRepository.save(any(Fine.class))).thenAnswer(inv -> inv.getArgument(0));

        ViolationResponse result = service.record(asOfficer(), request(LocalDateTime.now().minusHours(1)));

        assertThat(result.plateNumber()).isEqualTo("RAB123A");
        assertThat(result.driverName()).isEqualTo(driver.getFullName());     // defaults to the owner
        assertThat(result.fineAmount()).isEqualByComparingTo("25000");
        assertThat(result.dueDate()).isEqualTo(LocalDate.now().plusDays(ViolationService.FINE_DUE_DAYS));
        assertThat(result.fineStatus()).isEqualTo("UNPAID");
        ArgumentCaptor<NotificationEvent> event = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().type()).isEqualTo(NotificationEvent.VIOLATION_RECORDED);
        assertThat(event.getValue().routingKey()).isEqualTo("violation.recorded");
        verify(evidenceRepository, never()).save(any());                    // no evidence sent
    }

    @Test
    void rejectsOffenceTimeInTheFuture() {
        stubLookups();
        assertThatThrownBy(() -> service.record(asOfficer(), request(LocalDateTime.now().plusHours(2))))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("future");
        verify(violationRepository, never()).save(any());
    }

    @Test
    void rejectsInactiveViolationType() {
        stubLookups();
        speeding.setActive(false);
        assertThatThrownBy(() -> service.record(asOfficer(), request(null)))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void accountWithoutOfficerProfileCannotRecord() {
        when(officerRepository.findByUserId(officerUser.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.record(asOfficer(), request(null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void driverCanAppealWithinFourteenDays() {
        Violation v = TestData.violationWithFine(vehicle, driver, officer, LocalDateTime.now().minusDays(2), FineStatus.UNPAID);
        when(violationRepository.findDetailedById(v.getId())).thenReturn(Optional.of(v));

        ViolationResponse result = service.appeal(asDriver(), v.getId(), new AppealRequest("I was not driving this car on that day at all."));

        assertThat(result.appealStatus()).isEqualTo("PENDING");
    }

    @Test
    void appealAfterFourteenDaysIsRejected() {
        Violation v = TestData.violationWithFine(vehicle, driver, officer, LocalDateTime.now().minusDays(20), FineStatus.UNPAID);
        when(violationRepository.findDetailedById(v.getId())).thenReturn(Optional.of(v));

        assertThatThrownBy(() -> service.appeal(asDriver(), v.getId(), new AppealRequest("I was not driving this car on that day at all.")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("14 days");
    }

    @Test
    void anotherDriverCannotAppeal() {
        Violation v = TestData.violationWithFine(vehicle, driver, officer, LocalDateTime.now().minusDays(2), FineStatus.UNPAID);
        when(violationRepository.findDetailedById(v.getId())).thenReturn(Optional.of(v));
        AuthUser stranger = new AuthUser(java.util.UUID.randomUUID(), "x@test.rw", Role.DRIVER);

        assertThatThrownBy(() -> service.appeal(stranger, v.getId(), new AppealRequest("I was not driving this car on that day at all.")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void acceptedAppealDismissesViolationAndCancelsFine() {
        Violation v = TestData.violationWithFine(vehicle, driver, officer, LocalDateTime.now().minusDays(2), FineStatus.UNPAID);
        v.setAppealStatus(AppealStatus.PENDING);
        when(violationRepository.findDetailedById(v.getId())).thenReturn(Optional.of(v));
        AuthUser admin = new AuthUser(java.util.UUID.randomUUID(), "admin@test.rw", Role.ADMIN);

        service.decideAppeal(admin, v.getId(), new AppealDecisionRequest(true, "Camera evidence shows a different car"));

        assertThat(v.getStatus()).isEqualTo(ViolationStatus.DISMISSED);
        assertThat(v.getAppealStatus()).isEqualTo(AppealStatus.ACCEPTED);
        assertThat(v.getFine().getStatus()).isEqualTo(FineStatus.CANCELLED);
        verify(events).publishEvent(any(NotificationEvent.class));
    }

    @Test
    void rejectedAppealGivesAtLeastSevenDaysToPay() {
        Violation v = TestData.violationWithFine(vehicle, driver, officer, LocalDateTime.now().minusDays(2), FineStatus.UNPAID);
        v.setAppealStatus(AppealStatus.PENDING);
        v.getFine().setDueDate(LocalDate.now().plusDays(1));
        when(violationRepository.findDetailedById(v.getId())).thenReturn(Optional.of(v));
        AuthUser admin = new AuthUser(java.util.UUID.randomUUID(), "admin@test.rw", Role.ADMIN);

        service.decideAppeal(admin, v.getId(), new AppealDecisionRequest(false, "Radar reading is valid"));

        assertThat(v.getAppealStatus()).isEqualTo(AppealStatus.REJECTED);
        assertThat(v.getFine().getDueDate()).isEqualTo(LocalDate.now().plusDays(7));
    }
}
