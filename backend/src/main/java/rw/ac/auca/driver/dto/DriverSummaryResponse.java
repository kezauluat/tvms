package rw.ac.auca.driver.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Driver dashboard header: what they owe and how close they are to the demerit-point limit. */
public record DriverSummaryResponse(UUID driverId, String fullName, String licenseNumber, String phoneNumber,
                                    int vehicleCount, int unpaidFineCount, BigDecimal outstandingAmount,
                                    long demeritPoints, int demeritPointLimit, boolean licenseAtRisk) {}
