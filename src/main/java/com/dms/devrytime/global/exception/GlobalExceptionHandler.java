package com.dms.devrytime.global.exception;

import com.dms.devrytime.global.exception.response.ErrorData;
import com.dms.devrytime.global.exception.response.ErrorDetail;
import com.dms.devrytime.global.exception.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DevryTimeException.class)
    public ResponseEntity<ErrorResponse> handleDevryTimeException(DevryTimeException e){

       ErrorCode errorCode = e.getErrorCode();
       ErrorResponse response = ErrorResponse.of(ErrorData.from(errorCode));

       return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidException(MethodArgumentNotValidException e){

        List<ErrorDetail> details = e.getBindingResult()
                .getFieldErrors().stream()
                .map(error -> ErrorDetail.of(error.getField(), error.getDefaultMessage()))
                .toList();

        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        ErrorResponse response = ErrorResponse.of(ErrorData.of(errorCode, details));

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException e){

        ErrorCode errorCode = ErrorCode.INVALID_PARAMETER_TYPE;
        ErrorResponse response = ErrorResponse.of(ErrorData.from(errorCode));

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<ErrorResponse> handleMailException(MailException e){

        ErrorCode errorCode = ErrorCode.SERVICE_UNAVAILABLE;
        ErrorResponse response = ErrorResponse.of(ErrorData.from(errorCode));

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e){

        log.error("예상하지 못한 예외가 발생했습니다.", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        ErrorResponse response = ErrorResponse.of(ErrorData.from(errorCode));

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }
}
