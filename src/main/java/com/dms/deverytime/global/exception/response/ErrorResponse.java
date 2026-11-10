package com.dms.deverytime.global.exception.response;

public record ErrorResponse(
        boolean success,
        ErrorData error
) {
    public static ErrorResponse of(ErrorData error){
        return new ErrorResponse(false, error);
    }
}
