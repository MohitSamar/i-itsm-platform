package com.bits.iitsm.config;

import com.bits.iitsm.exception.IITSMException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ApiError(String timestamp, String errorCode, String message) {}

    @ExceptionHandler(IITSMException.class)
    public ResponseEntity<ApiError> handleDomainException(IITSMException ex) {
        ApiError body = new ApiError(LocalDateTime.now().toString(), ex.getErrorCode(), ex.getMessage());
        HttpStatus status = ex.getErrorCode().contains("NOT_FOUND") ? HttpStatus.NOT_FOUND
                : ex.getErrorCode().contains("UNAUTHORIZED") ? HttpStatus.UNAUTHORIZED
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        ApiError body = new ApiError(LocalDateTime.now().toString(), "ERR_INTERNAL_SERVER", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
