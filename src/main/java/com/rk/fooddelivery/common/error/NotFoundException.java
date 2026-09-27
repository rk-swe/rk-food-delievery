package com.rk.fooddelivery.common.error;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
