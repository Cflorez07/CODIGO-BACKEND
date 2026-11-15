package com.app.priorix.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.app.priorix.Service.UsuarioService;
import com.app.priorix.model.entity.Usuario;

/** Devuelve el Usuario (entidad) detrás del token de la petición actual. */
@Component
public class UsuarioActual {

    private final UsuarioService usuarioService;

    public UsuarioActual(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    public Usuario de(Authentication auth) {
        Usuario u = (auth == null) ? null : usuarioService.buscarPorNombreUsuario(auth.getName());
        if (u == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado");
        return u;
    }
}
