package rw.ac.auca.fine.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(UUID fineId, String violationReference, String paymentReference,
                              BigDecimal amountPaid, LocalDateTime paidAt, String status) {}
