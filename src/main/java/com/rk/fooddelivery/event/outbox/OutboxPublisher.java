package com.rk.fooddelivery.event.outbox;

import com.rk.fooddelivery.config.RabbitConfig;
import com.rk.fooddelivery.event.dto.DomainEvent;
import com.rk.fooddelivery.event.dto.DomainEventType;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {
  private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
  private final OutboxEventRepository events;
  private final RabbitTemplate rabbit;
  public OutboxPublisher(OutboxEventRepository events, RabbitTemplate rabbit) {
    this.events = events;
    this.rabbit = rabbit;
  }

  @Scheduled(fixedDelayString = "${fooddelivery.events.outbox-delay-ms:1000}")
  public void publishScheduled() {
    publishPending();
  }

  @Transactional
  public void publishPending() {
    for (OutboxEvent stored : events.lockPendingBatch()) {
      try {
        DomainEventType type = DomainEventType.valueOf(stored.eventType());
        DomainEvent event = new DomainEvent(stored.id(), type, stored.aggregateId(), stored.aggregateVersion(), stored.occurredAt(), stored.payload());
        CorrelationData correlation = new CorrelationData(stored.id().toString());
        rabbit.convertAndSend(
            RabbitConfig.EVENT_EXCHANGE,
            type.routingKey(),
            event,
            message -> {
              message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
              message.getMessageProperties().setMessageId(stored.id().toString());
              return message;
            },
            correlation);
        CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
        if (!confirm.isAck()) {
          throw new IllegalStateException("Broker rejected event: " + confirm.getReason());
        }
        stored.publishedAt(Instant.now());
        log.info("eventId={} aggregateId={} version={} status=PUBLISHED", stored.id(), stored.aggregateId(), stored.aggregateVersion());
      } catch (Exception exception) {
        stored.failed(exception.getMessage());
        log.warn("eventId={} aggregateId={} status=PUBLISH_FAILED", stored.id(), stored.aggregateId(), exception);
      }
    }
  }
}
