package rw.ac.auca.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import rw.ac.auca.config.RabbitConfig;

/** Sends the event to RabbitMQ only AFTER the database transaction commits. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotificationEvent(NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, event.routingKey(), event);
        } catch (AmqpException e) {
            log.error("Could not publish {} for {}: {}", event.type(), event.reference(), e.getMessage());
        }
    }
}
