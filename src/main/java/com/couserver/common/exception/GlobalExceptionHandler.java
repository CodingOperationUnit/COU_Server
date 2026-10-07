package com.couserver.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;



@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // 1) 우리가 던진 비즈니스 예외 -> 에러코드에 정의된 status / code / message 그대로 응답
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode ec = e.getErrorCode();
        return ResponseEntity.status(ec.getStatus()).body(ErrorResponse.of(ec));
    }

    // 2) 예상하지 못한 모든 예외 -> 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("예상하지 못한 오류", e);
        return ResponseEntity
                .status(CommonErrorCode.INTERNAL_ERROR.getStatus())
                .body(ErrorResponse.of(CommonErrorCode.INTERNAL_ERROR));
    }

    // 3) @Valid 검증 실패-> 400 VALIDATION_FAILED
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        FieldError first = ex.getBindingResult().getFieldError();
        String message = first != null ? first.getDefaultMessage()
                : CommonErrorCode.VALIDATION_FAILED.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponse(400, CommonErrorCode.VALIDATION_FAILED.getCode(), message));
    }

    // 4) 나머지 스프링 표준 예외(404 없는 경로, 405 잘못된 메서드 등)와 ResponseStatusException
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        String message = (ex instanceof ResponseStatusException rse && rse.getReason() != null)
                ? rse.getReason() : ex.getMessage();
        HttpStatus resolved = HttpStatus.resolve(statusCode.value());
        String code = resolved != null ? resolved.name() : "HTTP_" + statusCode.value();
        return ResponseEntity.status(statusCode)
                .headers(headers)
                .body(new ErrorResponse(statusCode.value(), code, message));
    }
}
