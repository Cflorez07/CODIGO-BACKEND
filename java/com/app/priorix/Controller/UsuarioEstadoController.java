package com.app.priorix.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.app.priorix.Service.UsuarioService;
import com.app.priorix.model.entity.Usuario;

/** Activa o desactiva una cuenta desde el botón de Estado de la lista de usuarios. */
@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioEstadoController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/estado/{id}")
    public String cambiarEstado(@PathVariable String id, Authentication auth, RedirectAttributes redirectAttrs) {
        Usuario u = usuarioService.obtenerPorId(id);
        if (u == null) {
            redirectAttrs.addFlashAttribute("error", "No se encontró el usuario solicitado.");
            return "redirect:/admin/usuarios";
        }

        // Evita que el administrador desactive su propia cuenta y se quede sin acceso
        if (auth != null && auth.getName() != null && auth.getName().equalsIgnoreCase(u.getUsername())) {
            redirectAttrs.addFlashAttribute("error", "Por seguridad institucional, no puedes desactivar tu propia cuenta de administrador.");
            return "redirect:/admin/usuarios";
        }

        boolean nuevoEstado = !u.isActivo();
        u.setActivo(nuevoEstado);
        usuarioService.guardar(u);

        String estadoTexto = nuevoEstado ? "Activada" : "Inactivada";
        redirectAttrs.addFlashAttribute("mensaje", "La cuenta de @" + u.getUsername() + " fue " + estadoTexto + " exitosamente.");
        return "redirect:/admin/usuarios";
    }
}
