package com.sivet.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maneja globalmente las excepciones de los controladores REST.
 * Devuelve respuestas JSON uniformes sin exponer detalles internos.
 */
@RestControllerAdvice(basePackages = "com.sivet.controller.api")
public class GlobalExceptionHandler {

    /**
     * Maneja errores de validación de DTO anotados con @Valid.
     * Devuelve HTTP 400 y los errores de cada campo.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> manejarErroresValidacion(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errores = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.putIfAbsent(
                        error.getField(),
                        error.getDefaultMessage()
                )
        );

        ApiErrorResponse respuesta = crearRespuesta(
                HttpStatus.BAD_REQUEST,
                "Hay campos inválidos en la solicitud",
                request.getRequestURI(),
                errores
        );

        return ResponseEntity.badRequest().body(respuesta);
    }

    /**
     * Maneja errores de validación de parámetros de métodos.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> manejarRestricciones(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        Map<String, String> errores = new LinkedHashMap<>();

        ex.getConstraintViolations().forEach(violacion ->
                errores.put(
                        violacion.getPropertyPath().toString(),
                        violacion.getMessage()
                )
        );

        ApiErrorResponse respuesta = crearRespuesta(
                HttpStatus.BAD_REQUEST,
                "Uno o más parámetros son inválidos",
                request.getRequestURI(),
                errores
        );

        return ResponseEntity.badRequest().body(respuesta);
    }

    /**
     * Maneja solicitudes JSON mal formadas o con tipos incorrectos.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> manejarJsonIncorrecto(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        ApiErrorResponse respuesta = crearRespuesta(
                HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud contiene JSON inválido o datos incompatibles",
                request.getRequestURI(),
                Map.of()
        );

        return ResponseEntity.badRequest().body(respuesta);
    }

    /**
     * Maneja identificadores de ruta con un formato incorrecto.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> manejarTipoIncorrecto(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        ApiErrorResponse respuesta = crearRespuesta(
                HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un formato inválido",
                request.getRequestURI(),
                Map.of()
        );

        return ResponseEntity.badRequest().body(respuesta);
    }

    /**
     * Maneja recursos que no existen.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> manejarNoEncontrado(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        ApiErrorResponse respuesta = crearRespuesta(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI(),
                Map.of()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
    }

    /**
     * Maneja errores de reglas de negocio, como nombres duplicados.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> manejarArgumentoInvalido(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        ApiErrorResponse respuesta = crearRespuesta(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI(),
                Map.of()
        );

        return ResponseEntity.badRequest().body(respuesta);
    }

    /**
     * Maneja cualquier error inesperado sin revelar detalles internos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> manejarErrorInterno(
            Exception ex,
            HttpServletRequest request) {

        ApiErrorResponse respuesta = crearRespuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno. Intenta nuevamente más tarde.",
                request.getRequestURI(),
                Map.of()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(respuesta);
    }

    /**
     * Construye una respuesta de error con los datos comunes.
     */
    private ApiErrorResponse crearRespuesta(
            HttpStatus estado,
            String mensaje,
            String ruta,
            Map<String, String> erroresValidacion) {

        return new ApiErrorResponse(
                LocalDateTime.now(),
                estado.value(),
                estado.getReasonPhrase(),
                mensaje,
                ruta,
                erroresValidacion
        );
    }
}