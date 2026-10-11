package rw.ac.auca.messaging;

import rw.ac.auca.fine.domain.Fine;
import rw.ac.auca.violation.domain.Violation;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Message sent through RabbitMQ. Carries everything the email/SMS consumers need. */
public record NotificationEvent(String type,
                                String reference,
                                String recipientName,
                                String recipientEmail,
                                String recipientPhone,
                                String plateNumber,
                                String violationName,
                                String location,
                                LocalDateTime occurredAt,
                                BigDecimal amountDue,
                                LocalDate dueDate,
                                String message) implements Serializable {

    public static final String VIOLATION_RECORDED = "VIOLATION_RECORDED";
    public static final String FINE_PAID = "FINE_PAID";
    public static final String FINE_REMINDER = "FINE_REMINDER";
    public static final String FINE_OVERDUE = "FINE_OVERDUE";
    public static final String APPEAL_DECIDED = "APPEAL_DECIDED";

    /** Must be called inside a transaction: it reads lazy associations. */
    public static NotificationEvent of(String type, Violation v, Fine f, String message) {
        var driver = v.getDriver();
        String email = driver.getUser() != null ? driver.getUser().getEmail() : null;
        return new NotificationEvent(type, v.getReferenceNumber(), driver.getFullName(), email,
                driver.getPhoneNumber(), v.getVehicle().getPlateNumber(), v.getType().getName(), v.getLocation(),
                v.getOccurredAt(), f == null ? null : f.totalDue(), f == null ? null : f.getDueDate(), message);
    }

    /** VIOLATION_RECORDED -> violation.recorded */
    public String routingKey() {
        return type.toLowerCase().replaceFirst("_", ".");
    }
}
