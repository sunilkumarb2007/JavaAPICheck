package com.codeguardian.orderservice;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String timestamp,
        String requestId,
        int status,
        String errorCode,
        String message,
        String service,
        String path,
        String exception,
        Map<String, Object> source
) {
    public static ErrorResponse of(String requestId, int status, String errorCode, String message, String service, String path, String exception, String file, int line) {
        return new ErrorResponse(
                Instant.now().toString(),
                requestId != null ? requestId : "req-unknown",
                status,
                errorCode,
                message,
                service,
                path,
                exception,
                Map.of("file", file, "line", line)
        );
    }
}
