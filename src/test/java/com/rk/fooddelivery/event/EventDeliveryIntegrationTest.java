package com.rk.fooddelivery.event;

import static org.assertj.core.api.Assertions.assertThat;

import static org.awaitility.Awaitility.await;

import com.rk.fooddelivery.config.RabbitConfig;
import com.rk.fooddelivery.event.dto.DomainEvent;
import com.rk.fooddelivery.event.outbox.OutboxService;
import com.rk.fooddelivery.event.outbox.OutboxPublisher;
import com.rk.fooddelivery.support.IntegrationTestSupport;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class EventDeliveryIntegrationTest extends IntegrationTestSupport {

  @Autowired private OutboxService outbox;
  @Autowired private OutboxPublisher publisher;
  @Autowired private RabbitTemplate rabbit;
  @Autowired private PlatformTransactionManager transactions;

  @Test
  void rollsBackTheOutboxEventWithTheBusinessTransaction() {
    UUID orderId = UUID.randomUUID();
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              outbox.append(DomainEvent.orderAccepted(UUID.randomUUID(), orderId, 1, Map.of()));
              status.setRollbackOnly();
            });

    assertThat(jdbc.queryForObject("select count(*) from outbox_events", Long.class)).isZero();
  }

  @Test
  void publishesACommittedEventToEachIndependentNotificationConsumer() {
    UUID eventId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> outbox.append(DomainEvent.orderAccepted(eventId, orderId, 1, Map.of())));

    publisher.publishPending();

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(
                        jdbc.queryForObject(
                            "select count(*) from notification_deliveries where event_id = ?",
                            Long.class,
                            eventId))
                    .isEqualTo(3L));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from outbox_events where id = ? and published_at is not null",
                Long.class,
                eventId))
        .isEqualTo(1L);
  }

  @Test
  void ignoresARepeatDeliveryForEachConsumer() {
    UUID eventId = UUID.randomUUID();
    DomainEvent event = DomainEvent.orderAccepted(eventId, UUID.randomUUID(), 1, Map.of());

    rabbit.convertAndSend(RabbitConfig.EVENT_EXCHANGE, event.type().routingKey(), event);
    rabbit.convertAndSend(RabbitConfig.EVENT_EXCHANGE, event.type().routingKey(), event);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(
                        jdbc.queryForObject(
                            "select count(*) from notification_deliveries where event_id = ?",
                            Long.class,
                            eventId))
                    .isEqualTo(3L));
    await()
        .during(Duration.ofMillis(500))
        .atMost(Duration.ofSeconds(2))
        .untilAsserted(
            () ->
                assertThat(
                        jdbc.queryForObject(
                            "select count(*) from notification_deliveries where event_id = ?",
                            Long.class,
                            eventId))
                    .isEqualTo(3L));
  }
}
