package com.app.priorix.api;

import org.springframework.http.HttpStatus;

/** Error de negocio de la API: se convierte en JSON con el código HTTP indicado. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
