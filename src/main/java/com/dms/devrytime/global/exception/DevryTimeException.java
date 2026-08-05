package com.dms.devrytime.global.exception;

import lombok.Getter;

@Getter
public class DevryTimeException extends RuntimeException {

    private final ErrorCode errorCode;

    public DevryTimeException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
