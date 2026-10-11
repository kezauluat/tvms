package rw.ac.auca.violation.domain;

import jakarta.persistence.*;
import lombok.*;
import rw.ac.auca.base.BaseEntity;

import java.math.BigDecimal;

/** Catalogue of offences managed by the admin, e.g. SPEEDING = 25,000 RWF + 4 demerit points. */
@Entity
@Table(name = "violation_types",
        check = @CheckConstraint(name = "ck_violation_type_values",
                constraint = "fine_amount > 0 AND demerit_points BETWEEN 0 AND 12"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViolationType extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "fine_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal fineAmount;

    @Column(name = "demerit_points", nullable = false)
    private Integer demeritPoints;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
