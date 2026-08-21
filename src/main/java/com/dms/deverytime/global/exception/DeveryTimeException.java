package com.dms.deverytime.global.exception;

import lombok.Getter;

@Getter
public class DeveryTimeException extends RuntimeException {

    private final ErrorCode errorCode;

    public DeveryTimeException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
