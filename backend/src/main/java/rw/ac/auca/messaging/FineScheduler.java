package rw.ac.auca.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rw.ac.auca.fine.service.FineService;

/** Time-based business rules. Each run publishes RabbitMQ events for the affected drivers. */
@Component
@RequiredArgsConstructor
public class FineScheduler {

    private final FineService fineService;

    @Scheduled(cron = "0 0 1 * * *")
    public void markOverdue() {
        fineService.markOverdueFines();
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void remindDue() {
        fineService.sendDueReminders();
    }
}
