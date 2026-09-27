package com.bankflow.dto.response;

import java.time.Instant;
import java.util.Map;

/**
 * The single error shape for every failure the API returns. The frontend's Axios
 * interceptor is written against exactly these fields.
 */
public record ApiError(
        String code,
        String message,
        Map<String, String> fieldErrors,
        String path,
        Instant timestamp) {

    public static ApiError of(String code, String message, String path) {
        return new ApiError(code, message, null, path, Instant.now());
    }

    public static ApiError withFields(
            String code, String message, Map<String, String> fieldErrors, String path) {
        return new ApiError(code, message, fieldErrors, path, Instant.now());
    }
}
