package rw.ac.auca.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import rw.ac.auca.config.RabbitConfig;
import rw.ac.auca.document.NotificationLog;
import rw.ac.auca.document.repository.NotificationLogRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final JavaMailSender mailSender;
    private final NotificationLogRepository notificationLogRepository;

    @RabbitListener(queues = RabbitConfig.EMAIL_QUEUE)
    public void sendEmail(NotificationEvent event) {
        if (event.recipientEmail() == null) {
            return; // driver has no online account yet; SMS still goes out
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom("Traffic Fines <no-reply@tvms.rw>");
            mail.setTo(event.recipientEmail());
            mail.setSubject(subject(event) + " - " + event.reference());
            mail.setText(body(event));
            mailSender.send(mail);
            saveLog(event, "EMAIL", event.recipientEmail(), "SENT");
        } catch (Exception e) {
            saveLog(event, "EMAIL", event.recipientEmail(), "FAILED");
            throw new IllegalStateException("Email failed, will retry", e); // retried, then dead-lettered
        }
    }

    /** Simulated SMS gateway: a real one (e.g. Africa's Talking) needs a paid API key. */
    @RabbitListener(queues = RabbitConfig.SMS_QUEUE)
    public void sendSms(NotificationEvent event) {
        if (event.recipientPhone() == null) {
            return;
        }
        String text = "TVMS: " + subject(event) + " " + event.reference() + " (" + event.plateNumber() + ")"
                + (event.amountDue() != null ? ", amount " + rwf(event.amountDue()) : "")
                + (event.dueDate() != null && !NotificationEvent.FINE_PAID.equals(event.type())
                    ? ", due " + event.dueDate().format(DATE) : "");
        log.info("SMS to {}: {}", event.recipientPhone(), text);
        saveLog(event, "SMS", event.recipientPhone(), "SENT");
    }

    private String subject(NotificationEvent e) {
        return switch (e.type()) {
            case NotificationEvent.VIOLATION_RECORDED -> "Traffic fine issued";
            case NotificationEvent.FINE_PAID -> "Payment received";
            case NotificationEvent.FINE_REMINDER -> "Fine due in 3 days";
            case NotificationEvent.FINE_OVERDUE -> "Fine overdue, penalty added";
            case NotificationEvent.APPEAL_DECIDED -> "Appeal decision";
            default -> "Traffic fine update";
        };
    }

    private String body(NotificationEvent e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hello ").append(e.recipientName()).append(",\n\n");
        sb.append(subject(e)).append(".\n\n");
        sb.append("Reference: ").append(e.reference()).append('\n');
        sb.append("Vehicle: ").append(e.plateNumber()).append('\n');
        sb.append("Offence: ").append(e.violationName()).append('\n');
        if (e.location() != null) sb.append("Location: ").append(e.location()).append('\n');
        if (e.occurredAt() != null) sb.append("Date: ").append(e.occurredAt().format(DATE_TIME)).append('\n');
        if (e.amountDue() != null) sb.append("Amount: ").append(rwf(e.amountDue())).append('\n');
        if (e.dueDate() != null) sb.append("Due date: ").append(e.dueDate().format(DATE)).append('\n');
        if (e.message() != null) sb.append('\n').append(e.message()).append('\n');
        sb.append("\nPay or appeal online in the driver portal.\n\nTraffic Violation Management System");
        return sb.toString();
    }

    private static String rwf(BigDecimal amount) {
        return NumberFormat.getIntegerInstance(Locale.US).format(amount) + " RWF";
    }

    private void saveLog(NotificationEvent e, String channel, String recipient, String status) {
        try {
            notificationLogRepository.save(NotificationLog.builder()
                    .eventType(e.type()).channel(channel).recipient(recipient)
                    .reference(e.reference()).status(status).build());
        } catch (Exception ex) {
            log.warn("Could not save notification log: {}", ex.getMessage());
        }
    }
}
