package com.app.priorix.api;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.priorix.api.dto.ApiDtos.UsuarioResponse;

@RestController
@RequestMapping("/api/v1/me")
public class ApiMeController {

    private final UsuarioActual usuarioActual;

    public ApiMeController(UsuarioActual usuarioActual) {
        this.usuarioActual = usuarioActual;
    }

    /** Datos del usuario del token (la app lo usa al abrir para saber el rol y validar la sesión). */
    @GetMapping
    public UsuarioResponse yo(Authentication auth) {
        return UsuarioResponse.from(usuarioActual.de(auth));
    }
}
