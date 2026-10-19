package com.ticketon.ticketon.common.controller;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorCode;
import com.ticket.support.exception.ErrorType;
import com.ticket.support.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class ApiControllerAdvice {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handleAppException(AppException e) {
        printAppExceptionLog(e);
        return toResponse(e.getErrorType(), e.getData());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.warn("[데이터 제약 : 위반]: type={}", e.getMostSpecificCause().getClass().getSimpleName());
        return toResponse(ErrorType.DUPLICATE_RESOURCE, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("[요청 검증 : 실패]: message={}", e.getMessage());
        return toResponse(ErrorType.INVALID_REQUEST, null);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleRequestBindingException(Exception e) {
        log.warn("[요청 파라미터 : 바인딩 실패]: message={}", e.getMessage());
        return toResponse(ErrorType.INVALID_REQUEST, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("[요청 파라미터 : 제약 위반]: message={}", e.getMessage());
        return toResponse(ErrorType.INVALID_REQUEST, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("[미처리 예외 : 시스템 오류]: type={} | message={}", e.getClass().getName(), e.getMessage(), e);
        return toResponse(ErrorType.DEFAULT_ERROR, null);
    }

    private ResponseEntity<ApiResponse<Object>> toResponse(ErrorType errorType, Object data) {
        return ResponseEntity.status(errorType.getStatus()).body(ApiResponse.error(errorType, data));
    }

    private void printAppExceptionLog(AppException e) {
        StackTraceElement origin = e.getStackTrace()[0];
        int status = e.getErrorType().getStatus().value();
        ErrorCode errorCode = e.getErrorType().getErrorCode();

        switch (e.getErrorType().getLogLevel()) {
            case ERROR -> log.error("[AppException]: class={} | method={} | line={} | status={} | errorCode={} | message={} | data={}",
                    origin.getClassName(), origin.getMethodName(), origin.getLineNumber(), status, errorCode, e.getMessage(), e.getData(), e.getCause());
            case WARN -> log.warn("[AppException]: class={} | method={} | line={} | status={} | errorCode={} | message={} | data={}",
                    origin.getClassName(), origin.getMethodName(), origin.getLineNumber(), status, errorCode, e.getMessage(), e.getData());
            default -> log.info("[AppException]: class={} | method={} | line={} | status={} | errorCode={} | message={} | data={}",
                    origin.getClassName(), origin.getMethodName(), origin.getLineNumber(), status, errorCode, e.getMessage(), e.getData());
        }
    }
}
