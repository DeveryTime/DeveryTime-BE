package com.dms.deverytime.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiResponse<T>(
        boolean success,
        T data,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String message
) {
    public static <T> ApiResponse<T> success(T data, String message){
        return new ApiResponse<>(true, data, message);
    }

    public static <T> ApiResponse<T> success(T data){
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> successMessage(String message){
        return new ApiResponse<>(true, null, message);
    }
}
