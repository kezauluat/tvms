package rw.ac.auca.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * tvms.events (topic) --*.*--> notifications.email
 *                     --*.*--> notifications.sms
 * Messages that still fail after 3 attempts --> tvms.dlx --> notifications.dlq
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "tvms.events";
    public static final String EMAIL_QUEUE = "notifications.email";
    public static final String SMS_QUEUE = "notifications.sms";
    public static final String DLX = "tvms.dlx";
    public static final String DLQ = "notifications.dlq";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE).deadLetterExchange(DLX).deadLetterRoutingKey("dead").build();
    }

    @Bean
    public Queue smsQueue() {
        return QueueBuilder.durable(SMS_QUEUE).deadLetterExchange(DLX).deadLetterRoutingKey("dead").build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue()).to(eventsExchange()).with("*.*");
    }

    @Bean
    public Binding smsBinding() {
        return BindingBuilder.bind(smsQueue()).to(eventsExchange()).with("*.*");
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("dead");
    }

    @Bean
    public MessageConverter messageConverter() {
        SimpleMessageConverter converter = new SimpleMessageConverter();
        converter.setAllowedListPatterns(List.of("rw.ac.auca.*", "java.time.*", "java.lang.*", "java.math.*", "java.util.*"));
        return converter;
    }
}
