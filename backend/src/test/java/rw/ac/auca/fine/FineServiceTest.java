package rw.ac.auca.fine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import rw.ac.auca.TestData;
import rw.ac.auca.audit.AuditService;
import rw.ac.auca.auth.domain.Role;
import rw.ac.auca.auth.domain.User;
import rw.ac.auca.exception.BusinessValidationException;
import rw.ac.auca.exception.ConflictException;
import rw.ac.auca.exception.ForbiddenException;
import rw.ac.auca.fine.domain.Fine;
import rw.ac.auca.fine.domain.FineStatus;
import rw.ac.auca.fine.dto.PayFineRequest;
import rw.ac.auca.fine.dto.PaymentResponse;
import rw.ac.auca.fine.repository.FineRepository;
import rw.ac.auca.fine.service.FineService;
import rw.ac.auca.messaging.NotificationEvent;
import rw.ac.auca.security.AuthUser;
import rw.ac.auca.violation.domain.AppealStatus;
import rw.ac.auca.violation.domain.Violation;
import rw.ac.auca.violation.repository.ViolationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FineServiceTest {

    @Mock FineRepository fineRepository;
    @Mock ViolationRepository violationRepository;
    @Mock ApplicationEventPublisher events;
    @Mock AuditService auditService;
    @InjectMocks FineService service;

    private User driverUser;
    private Violation violation;
    private Fine fine;
    private final PayFineRequest momo = new PayFineRequest("0788300400");

    @BeforeEach
    void setUp() {
        driverUser = TestData.user(Role.DRIVER);
        var driver = TestData.driver(driverUser);
        var officer = TestData.officer(TestData.user(Role.OFFICER));
        violation = TestData.violationWithFine(TestData.vehicle(driver), driver, officer, LocalDateTime.now().minusDays(1), FineStatus.UNPAID);
        fine = violation.getFine();
    }

    private AuthUser asDriver() {
        return new AuthUser(driverUser.getId(), driverUser.getEmail(), Role.DRIVER);
    }

    @Test
    void payingUnpaidFineMarksItPaidAndNotifies() {
        when(fineRepository.findDetailedById(fine.getId())).thenReturn(Optional.of(fine));

        PaymentResponse receipt = service.pay(asDriver(), fine.getId(), momo);

        assertThat(receipt.status()).isEqualTo("PAID");
        assertThat(receipt.paymentReference()).startsWith("MOMO-");
        assertThat(receipt.amountPaid()).isEqualByComparingTo("25000");
        assertThat(fine.getPayerPhone()).isEqualTo("0788300400");
        verify(fineRepository).saveAndFlush(fine);
        verify(events).publishEvent(any(NotificationEvent.class));
    }

    @Test
    void cannotPaySomeoneElsesFine() {
        when(fineRepository.findDetailedById(fine.getId())).thenReturn(Optional.of(fine));
        AuthUser stranger = new AuthUser(UUID.randomUUID(), "x@test.rw", Role.DRIVER);

        assertThatThrownBy(() -> service.pay(stranger, fine.getId(), momo)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cannotPayTwice() {
        fine.setStatus(FineStatus.PAID);
        fine.setPaymentReference("MOMO-OLD");
        when(fineRepository.findDetailedById(fine.getId())).thenReturn(Optional.of(fine));

        assertThatThrownBy(() -> service.pay(asDriver(), fine.getId(), momo)).isInstanceOf(ConflictException.class);
        verify(fineRepository, never()).saveAndFlush(any());
    }

    @Test
    void pendingAppealBlocksPayment() {
        violation.setAppealStatus(AppealStatus.PENDING);
        when(fineRepository.findDetailedById(fine.getId())).thenReturn(Optional.of(fine));

        assertThatThrownBy(() -> service.pay(asDriver(), fine.getId(), momo)).isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void overdueJobAddsTenPercentPenalty() {
        fine.setDueDate(LocalDate.now().minusDays(1));
        when(fineRepository.findOverdueCandidates(eq(FineStatus.UNPAID), any(LocalDate.class), eq(AppealStatus.PENDING)))
                .thenReturn(List.of(fine));

        int count = service.markOverdueFines();

        assertThat(count).isEqualTo(1);
        assertThat(fine.getStatus()).isEqualTo(FineStatus.OVERDUE);
        assertThat(fine.getPenaltyAmount()).isEqualByComparingTo("2500");
        assertThat(fine.totalDue()).isEqualByComparingTo("27500");
    }
}
