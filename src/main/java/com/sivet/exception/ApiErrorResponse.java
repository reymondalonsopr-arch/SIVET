package com.sivet.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Respuesta uniforme para los errores de la API REST.
 */
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
}