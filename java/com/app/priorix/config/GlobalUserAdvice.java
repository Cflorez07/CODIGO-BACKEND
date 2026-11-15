package com.app.priorix.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.app.priorix.Repository.UsuarioRepository;
import com.app.priorix.model.entity.Usuario;

@ControllerAdvice
public class GlobalUserAdvice {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @ModelAttribute("currentUser")
    public Usuario populateCurrentUser(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return usuarioRepository.findByNombreUsuario(auth.getName()).orElse(null);
        }
        return null;
    }
}
