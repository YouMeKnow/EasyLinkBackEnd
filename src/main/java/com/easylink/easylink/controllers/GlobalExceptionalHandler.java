package com.easylink.easylink.controllers;

import com.easylink.easylink.exceptions.*;
import com.easylink.easylink.vibe_service.infrastructure.exception.OfferUpdateException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionalHandler {

    private boolean isSse(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String ct = request.getContentType();
        return (accept != null && accept.contains("text/event-stream"))
                || (ct != null && ct.contains("text/event-stream"))
                || request.getRequestURI().contains("/notifications/stream");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        if (isSse(request)) return null;

        String messageKey = ex.getReason() != null ? ex.getReason() : "unknown_error";
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                "message", messageKey,
                "status", ex.getStatusCode().value(),
                "timestamp", LocalDateTime.now(),
                "error", "Request Error"
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllOtherErrors(
            Exception ex,
            HttpServletRequest request
    ) {
        if (isSse(request)) return null;

        return buildErrorResponse(request, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Error", ex.getMessage());
    }

    @ExceptionHandler(UserLockedException.class)
    public ResponseEntity<Map<String, Object>> handleUserLocked(UserLockedException ex, HttpServletRequest request) {
        return buildErrorResponse(request, HttpStatus.LOCKED, "Account Locked", ex.getMessage());
    }

    @ExceptionHandler(IncorrectAnswerException.class)
    public ResponseEntity<Map<String, Object>> handleIncorrectAnswer(IncorrectAnswerException ex, HttpServletRequest request) {
        return buildErrorResponse(request, HttpStatus.UNAUTHORIZED, "Incorrect answer(s)", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildErrorResponse(request, HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(OfferUpdateException.class)
    public ResponseEntity<Map<String, Object>> handleOfferException(OfferUpdateException ex, HttpServletRequest request) {
        return buildErrorResponse(request, HttpStatus.BAD_REQUEST, "Offer update error", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        return buildErrorResponse(request, HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(DuplicateAccountException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateAccount(DuplicateAccountException ex, HttpServletRequest request) {
        return buildErrorResponse(request, HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleLimit(RateLimitExceededException ex, HttpServletRequest request) {
        return buildErrorResponse(request, HttpStatus.TOO_MANY_REQUESTS, "Rate limit", ex.getMessage());
    }

    @ExceptionHandler(OfferLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleOfferLimit(OfferLimitExceededException ex, HttpServletRequest request) {
        return buildErrorResponse(request, HttpStatus.TOO_MANY_REQUESTS, "Offer limit", ex.getMessage());
    }

    @ExceptionHandler(VibeLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleVibeLimit(VibeLimitExceededException ex, HttpServletRequest request) {
        return buildErrorResponse(request, HttpStatus.TOO_MANY_REQUESTS, "Vibe limit", ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpServletRequest request,
            HttpStatus status,
            String error,
            String message
    ) {
        if (isSse(request)) return null;

        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", status.value(),
                "error", error,
                "message", message
        ));
    }
}
