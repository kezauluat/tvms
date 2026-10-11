package rw.ac.auca.violation.dto;

import rw.ac.auca.violation.domain.ViolationType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

/** Serializable so the list can be cached in Redis. */
public record ViolationTypeResponse(UUID id, String code, String name, String description,
                                    BigDecimal fineAmount, Integer demeritPoints, boolean active) implements Serializable {

    public static ViolationTypeResponse from(ViolationType t) {
        return new ViolationTypeResponse(t.getId(), t.getCode(), t.getName(), t.getDescription(),
                t.getFineAmount(), t.getDemeritPoints(), t.isActive());
    }
}
