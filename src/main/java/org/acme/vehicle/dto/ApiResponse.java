package org.acme.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    public boolean success;
    public String message;
    public T data;
    public PageMeta meta;

    public ApiResponse() {}

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public ApiResponse(boolean success, String message, T data, PageMeta meta) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.meta = meta;
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> ok(String message, T data, PageMeta meta) {
        return new ApiResponse<>(true, message, data, meta);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }
}

