package com.app.priorix.config;

import com.app.priorix.Repository.UsuarioRepository;
import com.app.priorix.model.entity.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UsuarioRepository usuarioRepository;

    public GlobalControllerAdvice(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @ModelAttribute("currentUser")
    public Usuario populateCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String username = auth.getName();
        if (username == null || username.isBlank() || "anonymousUser".equalsIgnoreCase(username)) {
            return null;
        }
        return usuarioRepository.findByNombreUsuario(username)
                .or(() -> usuarioRepository.findByEmail(username.trim().toLowerCase()))
                .orElse(null);
    }
}
