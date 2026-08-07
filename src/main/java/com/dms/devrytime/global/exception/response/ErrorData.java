package com.dms.devrytime.global.exception.response;

import com.dms.devrytime.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorData(
        String code,
        String message,
        List<ErrorDetail> details
) {
    public static ErrorData from(ErrorCode errorCode){
        return new ErrorData(errorCode.getCode(), errorCode.getMessage(),null);
    }

    public static ErrorData of(ErrorCode errorCode, List<ErrorDetail> details) {
        return new ErrorData(errorCode.getCode(), errorCode.getMessage(), details);
    }
}
