package com.app.priorix.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.priorix.Repository.UsuarioRepository;
import com.app.priorix.Service.UsuarioService;
import com.app.priorix.api.dto.ApiDtos.UsuarioRequest;
import com.app.priorix.api.dto.ApiDtos.UsuarioResponse;
import com.app.priorix.model.entity.Usuario;
import com.app.priorix.model.enumeration.RolUsuario;

import jakarta.validation.Valid;

/** Gestión de usuarios: solo administrador. Nunca se devuelve la contraseña. */
@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class ApiUsuarioController {

    private static final int CLAVE_MINIMA = 8;

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioActual usuarioActual;

    public ApiUsuarioController(UsuarioService usuarioService, UsuarioRepository usuarioRepository,
                                PasswordEncoder passwordEncoder, UsuarioActual usuarioActual) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public List<UsuarioResponse> listar(@RequestParam(required = false) String q) {
        String filtro = (q == null || q.isBlank()) ? null : q.trim();
        return usuarioService.listar(filtro).stream().map(UsuarioResponse::from).toList();
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable String id) {
        return UsuarioResponse.from(existente(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest r) {
        if (r.password() == null || r.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria");
        }
        validarClave(r.password());
        validarUnicos(r, null);

        Usuario u = new Usuario();
        u.setNombreUsuario(r.nombreUsuario().trim());
        u.setEmail(r.email().trim().toLowerCase());
        u.setRol(parseRol(r.rol()));
        u.setPassword(passwordEncoder.encode(r.password()));
        u.setActivo(true);   // igual que en la web: lo crea el administrador, queda activo
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(usuarioService.guardar(u)));
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable String id, @Valid @RequestBody UsuarioRequest r) {
        Usuario u = existente(id);
        validarUnicos(r, id);

        u.setNombreUsuario(r.nombreUsuario().trim());
        u.setEmail(r.email().trim().toLowerCase());
        u.setRol(parseRol(r.rol()));
        if (r.password() != null && !r.password().isBlank()) {
            validarClave(r.password());
            u.setPassword(passwordEncoder.encode(r.password()));
        }
        return UsuarioResponse.from(usuarioService.guardar(u));
    }

    /** Activa o desactiva una cuenta: PUT /api/v1/usuarios/{id}/activo?valor=false */
    @PutMapping("/{id}/activo")
    public UsuarioResponse cambiarActivo(@PathVariable String id, @RequestParam boolean valor, Authentication auth) {
        Usuario u = existente(id);
        if (!valor && u.getId().equals(usuarioActual.de(auth).getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No puedes desactivar tu propia cuenta");
        }
        u.setActivo(valor);
        return UsuarioResponse.from(usuarioService.guardar(u));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id, Authentication auth) {
        Usuario u = existente(id);
        if (u.getId().equals(usuarioActual.de(auth).getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No puedes eliminar tu propia cuenta");
        }
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- utilidades ----------

    private Usuario existente(String id) {
        Usuario u = usuarioService.obtenerPorId(id);
        if (u == null) throw new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        return u;
    }

    private RolUsuario parseRol(String texto) {
        String n = texto.trim().toUpperCase();
        if (!n.startsWith("ROLE_")) n = "ROLE_" + n;
        try {
            return RolUsuario.valueOf(n);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Rol inválido. Usa ADMIN, ENFERMERO o MEDICO");
        }
    }

    private void validarClave(String clave) {
        if (clave.length() < CLAVE_MINIMA) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "La contraseña debe tener al menos " + CLAVE_MINIMA + " caracteres");
        }
    }

    private void validarUnicos(UsuarioRequest r, String idActual) {
        usuarioRepository.findByNombreUsuario(r.nombreUsuario().trim()).ifPresent(o -> {
            if (idActual == null || !o.getId().equals(idActual))
                throw new ApiException(HttpStatus.CONFLICT, "Ese nombre de usuario ya existe");
        });
        usuarioRepository.findByEmail(r.email().trim().toLowerCase()).ifPresent(o -> {
            if (idActual == null || !o.getId().equals(idActual))
                throw new ApiException(HttpStatus.CONFLICT, "Ese correo ya está registrado");
        });
    }
}
