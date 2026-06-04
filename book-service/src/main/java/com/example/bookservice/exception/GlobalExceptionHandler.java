package com.example.bookservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return error(ex.getStatus(), ex.getDescription(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        return error(500, "Serverda kutilmagan xato yuz berdi. Iltimos, qayta urinib ko'ring", request.getRequestURI());
    }

    private ResponseEntity<Map<String, Object>> error(int status, String description, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("error", errorCode(status));
        body.put("description", description);
        body.put("path", path);
        return ResponseEntity.status(status).body(body);
    }

    private String errorCode(int status) {
        if (status >= 500) return "SERVER_ERROR";
        if (status == 404) return "NOT_FOUND";
        if (status == 401 || status == 403) return "FORBIDDEN";
        return "BAD_REQUEST";
    }
}
