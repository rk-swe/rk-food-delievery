package com.rk.fooddelivery.common.error;

public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
