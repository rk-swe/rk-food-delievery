package com.rk.fooddelivery.payment.controller;

import com.rk.fooddelivery.payment.service.PaymentWebhookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Payment Webhooks")
@RequestMapping("/api/payment-webhooks")
public class PaymentWebhookController {
  private final PaymentWebhookService service;

  public PaymentWebhookController(PaymentWebhookService service) {
    this.service = service;
  }

  @PostMapping("/mock")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void receive(
      @RequestBody byte[] body, @RequestHeader("X-Payment-Signature") String signature) {
    service.handle(body, signature);
  }
}
