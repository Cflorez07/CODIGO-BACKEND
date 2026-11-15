package com.app.priorix.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.app.priorix.exception.RecursoNoEncontradoException;
import com.app.priorix.exception.ValidacionException;

/**
 * Respuestas de error en JSON para /api/**. Tiene prioridad sobre GlobalExceptionHandler
 * (que devuelve vistas HTML) pero solo para los controladores de este paquete.
 *
 * Formato: {"status":400,"error":"mensaje","campos":{"nombre":"es obligatorio"}}
 */
@RestControllerAdvice(basePackages = "com.app.priorix.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> api(ApiException ex) {
        return cuerpo(ex.getStatus(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return cuerpo(HttpStatus.BAD_REQUEST, "Hay campos inválidos", campos);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> cuerpoInvalido(Exception ex) {
        return cuerpo(HttpStatus.BAD_REQUEST, "La petición no tiene un formato válido", null);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException ex) {
        return cuerpo(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<Map<String, Object>> validacionNegocio(ValidacionException ex) {
        return cuerpo(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> prohibido(AccessDeniedException ex) {
        return cuerpo(HttpStatus.FORBIDDEN, "No tienes permiso para esta acción.", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception ex) {
        log.error("Error inesperado en la API", ex);
        return cuerpo(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intenta de nuevo.", null);
    }

    private ResponseEntity<Map<String, Object>> cuerpo(HttpStatus status, String mensaje, Map<String, String> campos) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", mensaje);
        if (campos != null) body.put("campos", campos);
        return ResponseEntity.status(status).body(body);
    }
}
