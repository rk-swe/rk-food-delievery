package com.rk.fooddelivery.config;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class RabbitConfig {
  public static final String EVENT_EXCHANGE = "fooddelivery.events";
  public static final String DEAD_LETTER_EXCHANGE = "fooddelivery.events.dlx";
  public static final String CUSTOMER_QUEUE = "fooddelivery.notifications.customer";
  public static final String RESTAURANT_QUEUE = "fooddelivery.notifications.restaurant";
  public static final String PARTNER_QUEUE = "fooddelivery.notifications.partner";
  public static final String ASSIGNMENT_QUEUE = "fooddelivery.assignment";
  public static final String REFUND_QUEUE = "fooddelivery.refund";

  @Bean TopicExchange eventExchange() { return new TopicExchange(EVENT_EXCHANGE, true, false); }
  @Bean DirectExchange deadLetterExchange() { return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false); }

  @Bean Queue customerNotificationQueue() { return retryQueue(CUSTOMER_QUEUE); }
  @Bean Queue restaurantNotificationQueue() { return retryQueue(RESTAURANT_QUEUE); }
  @Bean Queue partnerNotificationQueue() { return retryQueue(PARTNER_QUEUE); }
  @Bean Queue assignmentQueue() { return retryQueue(ASSIGNMENT_QUEUE); }
  @Bean Queue refundQueue() { return retryQueue(REFUND_QUEUE); }

  @Bean Queue customerNotificationDlq() { return QueueBuilder.durable(CUSTOMER_QUEUE + ".dlq").build(); }
  @Bean Queue restaurantNotificationDlq() { return QueueBuilder.durable(RESTAURANT_QUEUE + ".dlq").build(); }
  @Bean Queue partnerNotificationDlq() { return QueueBuilder.durable(PARTNER_QUEUE + ".dlq").build(); }
  @Bean Queue assignmentDlq() { return QueueBuilder.durable(ASSIGNMENT_QUEUE + ".dlq").build(); }
  @Bean Queue refundDlq() { return QueueBuilder.durable(REFUND_QUEUE + ".dlq").build(); }

  @Bean Binding customerNotificationBinding() { return bindAll(CUSTOMER_QUEUE); }
  @Bean Binding restaurantNotificationBinding() { return bindAll(RESTAURANT_QUEUE); }
  @Bean Binding partnerNotificationBinding() { return bindAll(PARTNER_QUEUE); }
  @Bean Binding assignmentAcceptedBinding() { return BindingBuilder.bind(assignmentQueue()).to(eventExchange()).with("order.accepted"); }
  @Bean Binding assignmentRequestedBinding() { return BindingBuilder.bind(assignmentQueue()).to(eventExchange()).with("delivery.assignment.requested"); }
  @Bean Binding refundBinding() { return BindingBuilder.bind(refundQueue()).to(eventExchange()).with("payment.refund.requested"); }

  @Bean Binding customerDlqBinding() { return dlq(CUSTOMER_QUEUE); }
  @Bean Binding restaurantDlqBinding() { return dlq(RESTAURANT_QUEUE); }
  @Bean Binding partnerDlqBinding() { return dlq(PARTNER_QUEUE); }
  @Bean Binding assignmentDlqBinding() { return dlq(ASSIGNMENT_QUEUE); }
  @Bean Binding refundDlqBinding() { return dlq(REFUND_QUEUE); }

  @Bean
  SimpleRabbitListenerContainerFactory eventRabbitListenerContainerFactory(
      ConnectionFactory connectionFactory, MessageConverter eventMessageConverter) {
    var factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setDefaultRequeueRejected(false);
    factory.setMessageConverter(eventMessageConverter);
    Advice retry =
        RetryInterceptorBuilder.stateless()
            .maxRetries(2)
            .backOffOptions(100, 2.0, 1_000)
            .recoverer(new RejectAndDontRequeueRecoverer())
            .build();
    factory.setAdviceChain(retry);
    return factory;
  }

  @Bean
  MessageConverter eventMessageConverter() {
    return new JacksonJsonMessageConverter("com.rk.fooddelivery.event.dto");
  }

  private Queue retryQueue(String name) {
    return QueueBuilder.durable(name)
        .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
        .withArgument("x-dead-letter-routing-key", name)
        .build();
  }

  private Binding bindAll(String queue) { return BindingBuilder.bind(new Queue(queue)).to(eventExchange()).with("#"); }
  private Binding dlq(String queue) { return BindingBuilder.bind(new Queue(queue + ".dlq")).to(deadLetterExchange()).with(queue); }
}
