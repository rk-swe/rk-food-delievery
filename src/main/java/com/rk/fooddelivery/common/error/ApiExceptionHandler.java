package com.rk.fooddelivery.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String REQUEST_ID_ATTRIBUTE = ApiExceptionHandler.class.getName() + ".requestId";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
            .toList();
        return ResponseEntity.badRequest().body(new ApiError(
            HttpStatus.BAD_REQUEST.value(),
            "VALIDATION_FAILED",
            "Request validation failed",
            requestId(request),
            fieldErrors));
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiError> handleDomain(DomainException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(
            HttpStatus.CONFLICT.value(),
            "CONFLICT",
            exception.getMessage(),
            requestId(request),
            List.of()));
    }

    private String requestId(HttpServletRequest request) {
        Object existing = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (existing instanceof String value) {
            return value;
        }
        String generated = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, generated);
        return generated;
    }

    record ApiError(int status, String code, String message, String requestId, List<FieldError> fieldErrors) {
    }

    record FieldError(String field, String message) {
    }
}
