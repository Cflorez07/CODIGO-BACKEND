package com.app.priorix.Controller;

import com.app.priorix.model.entity.Usuario;
import com.app.priorix.Service.UsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioService usuarioService, PasswordEncoder passwordEncoder) {
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
    }

    @ModelAttribute("roles")
    public String[] roles() {
        return new String[]{"ROLE_ADMIN", "ROLE_MEDICO", "ROLE_ENFERMERO"};
    }

    @GetMapping
    public String listarUsuarios(@RequestParam(value = "q", required = false) String q, Model model) {
        List<Usuario> usuarios = usuarioService.listar(q);
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("query", q);
        return "admin/usuarios";
    }

    @GetMapping("/nuevo")
    public String nuevoUsuario(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "admin/usuario-form";
    }

    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario form) {
        boolean esNuevo = form.getId() == null || form.getId().isBlank();
        Usuario usuario;

        if (esNuevo) {
            usuario = form;
            usuario.setId(null);
            if (form.getEmail() != null) usuario.setEmail(form.getEmail().trim().toLowerCase());
        } else {
            // Edición: se parte del usuario guardado para no perder sus relaciones
            usuario = usuarioService.obtenerPorId(form.getId());
            if (usuario == null) return "redirect:/admin/usuarios?error=notfound";
            usuario.setNombreUsuario(form.getNombreUsuario());
            usuario.setEmail(form.getEmail() == null ? null : form.getEmail().trim().toLowerCase());
            usuario.setRol(form.getRol());
        }

        String claveNueva = form.getPassword();
        if (claveNueva != null && !claveNueva.isBlank()) {
            usuario.setPassword(passwordEncoder.encode(claveNueva));
        } else if (esNuevo) {
            return "redirect:/admin/usuarios/nuevo?error=password";
        }
        // Si es edición y no escribió clave, se conserva la anterior

        if (esNuevo) {
            // El administrador crea la cuenta; el usuario completa la activación con OTP.
            usuario.setActivo(false);
        }
        usuarioService.guardar(usuario);
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/editar/{id}")
    public String editarUsuario(@PathVariable String id, Model model) {
        Usuario u = usuarioService.obtenerPorId(id);
        if (u == null) return "redirect:/admin/usuarios?error=notfound";
        model.addAttribute("usuario", u);
        return "admin/usuario-form";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable String id) {
        usuarioService.eliminar(id);
        return "redirect:/admin/usuarios";
    }
}