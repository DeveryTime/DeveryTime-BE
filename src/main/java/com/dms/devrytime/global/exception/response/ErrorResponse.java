package com.dms.devrytime.global.exception.response;

public record ErrorResponse(
        boolean success,
        int status,
        ErrorData error
) {
    public static ErrorResponse of(int status, ErrorData error){
        return new ErrorResponse(false, status, error);
    }
}
