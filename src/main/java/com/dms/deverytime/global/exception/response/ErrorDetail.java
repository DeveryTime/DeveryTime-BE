package com.dms.deverytime.global.exception.response;

public record ErrorDetail(
        String field,
        String message
) {
    public static ErrorDetail of(String field, String message){
        return new ErrorDetail(field, message);
    }
}
