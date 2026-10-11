package rw.ac.auca.fine.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** What anyone can see by plate number: no names, licenses or phone numbers. */
public record PublicFineResponse(String referenceNumber, String plateNumber, String violationName,
                                 String location, LocalDateTime occurredAt, BigDecimal totalDue,
                                 LocalDate dueDate, String fineStatus, String appealStatus) {}
