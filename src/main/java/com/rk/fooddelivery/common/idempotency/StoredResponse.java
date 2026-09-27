package com.rk.fooddelivery.common.idempotency;

public record StoredResponse(int status, String body, String location) {}
