package com.hrgenius.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consistent JSON error response shape for all API errors.
 */
@Getter
@Builder
public class ApiError {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private int status;
    private String error;
    private String message;
    private String path;
    private List<FieldError> fieldErrors;

    @Getter
    @Builder
    public static class FieldError {
        private String field;
        private String message;
    }
}
