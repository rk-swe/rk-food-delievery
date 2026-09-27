package com.rk.fooddelivery.event.notification;

import com.rk.fooddelivery.config.RabbitConfig;
import com.rk.fooddelivery.event.dto.DomainEvent;
import com.rk.fooddelivery.event.inbox.InboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationListener {
  private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);
  private final InboxService inbox;
  private final NotificationDeliveryRepository deliveries;

  public NotificationListener(InboxService inbox, NotificationDeliveryRepository deliveries) {
    this.inbox = inbox;
    this.deliveries = deliveries;
  }

  @RabbitListener(
      queues = RabbitConfig.CUSTOMER_QUEUE,
      containerFactory = "eventRabbitListenerContainerFactory")
  public void customer(DomainEvent event) {
    consume("customer", event);
  }

  @RabbitListener(
      queues = RabbitConfig.RESTAURANT_QUEUE,
      containerFactory = "eventRabbitListenerContainerFactory")
  public void restaurant(DomainEvent event) {
    consume("restaurant", event);
  }

  @RabbitListener(
      queues = RabbitConfig.PARTNER_QUEUE,
      containerFactory = "eventRabbitListenerContainerFactory")
  public void partner(DomainEvent event) {
    consume("partner", event);
  }

  @Transactional
  void consume(String recipient, DomainEvent event) {
    if (!inbox.recordOnce("notification-" + recipient, event.id())) {
      log.info(
          "eventId={} aggregateId={} recipient={} status=DUPLICATE",
          event.id(),
          event.aggregateId(),
          recipient);
      return;
    }
    deliveries.save(
        new NotificationDelivery(
            recipient,
            event.id(),
            event.aggregateId(),
            event.aggregateVersion(),
            event.type().name()));
    log.info(
        "eventId={} aggregateId={} version={} recipient={} status=DELIVERED",
        event.id(),
        event.aggregateId(),
        event.aggregateVersion(),
        recipient);
  }
}
