package rw.ac.auca.admin.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/** Serializable so it can be cached in Redis. */
public record AdminStatsResponse(long totalViolations, long pendingAppeals, long unpaidFines, long overdueFines,
                                 BigDecimal outstandingAmount, BigDecimal collectedAmount,
                                 long drivers, long vehicles, long officers,
                                 List<TypeCount> violationsByType) implements Serializable {

    public record TypeCount(String name, long count) implements Serializable {}
}
