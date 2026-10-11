package rw.ac.auca.fine.domain;

import jakarta.persistence.*;
import lombok.*;
import rw.ac.auca.base.BaseEntity;
import rw.ac.auca.violation.domain.Violation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** The money owed for one violation. The amount is copied from the violation type at issue time. */
@Entity
@Table(name = "fines",
        indexes = @Index(name = "ix_fine_status_due", columnList = "status, due_date"),
        check = @CheckConstraint(name = "ck_fine_amounts", constraint = "amount > 0 AND penalty_amount >= 0"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fine extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "violation_id", nullable = false, unique = true)
    private Violation violation;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** Late payment penalty added when the fine becomes overdue. */
    @Column(name = "penalty_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FineStatus status = FineStatus.UNPAID;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_reference", unique = true, length = 40)
    private String paymentReference;

    @Column(name = "payer_phone", length = 15)
    private String payerPhone;

    /** Optimistic lock: two simultaneous payments of the same fine cannot both succeed. */
    @Version
    private Long version;

    public BigDecimal totalDue() {
        return amount.add(penaltyAmount == null ? BigDecimal.ZERO : penaltyAmount);
    }

    public boolean isPayable() {
        return status == FineStatus.UNPAID || status == FineStatus.OVERDUE;
    }
}
