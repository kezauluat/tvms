package rw.ac.auca.violation.domain;

import jakarta.persistence.*;
import lombok.*;
import rw.ac.auca.base.BaseEntity;
import rw.ac.auca.driver.domain.Driver;
import rw.ac.auca.fine.domain.Fine;
import rw.ac.auca.officer.domain.Officer;
import rw.ac.auca.vehicle.domain.Vehicle;

import java.time.LocalDateTime;

/** One traffic offence recorded by an officer against a vehicle and the driver responsible. */
@Entity
@Table(name = "violations", indexes = {
        @Index(name = "ix_violation_vehicle", columnList = "vehicle_id"),
        @Index(name = "ix_violation_driver_time", columnList = "driver_id, occurred_at"),
        @Index(name = "ix_violation_officer_time", columnList = "officer_id, occurred_at"),
        @Index(name = "ix_violation_appeal_status", columnList = "appeal_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Violation extends BaseEntity {

    @Column(name = "reference_number", nullable = false, unique = true, length = 20)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    /** The driver at the wheel; defaults to the vehicle owner. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "officer_id", nullable = false)
    private Officer officer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "violation_type_id", nullable = false)
    private ViolationType type;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ViolationStatus status = ViolationStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "appeal_status", nullable = false, length = 20)
    @Builder.Default
    private AppealStatus appealStatus = AppealStatus.NONE;

    @Column(name = "appeal_reason", length = 1000)
    private String appealReason;

    @Column(name = "appealed_at")
    private LocalDateTime appealedAt;

    @Column(name = "appeal_decision_note", length = 500)
    private String appealDecisionNote;

    @Column(name = "appeal_decided_at")
    private LocalDateTime appealDecidedAt;

    @OneToOne(mappedBy = "violation", fetch = FetchType.LAZY)
    private Fine fine;
}
