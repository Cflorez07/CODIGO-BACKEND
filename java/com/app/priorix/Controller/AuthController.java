package com.app.priorix.Controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.priorix.Dto.AuthResponse;
import com.app.priorix.Dto.LoginRequest;
import com.app.priorix.Repository.UsuarioRepository;
import com.app.priorix.Service.AuthService;
import com.app.priorix.Service.AuthVerificationService;
import com.app.priorix.Service.JwtService;
import com.app.priorix.model.entity.Usuario;

@RequestMapping("/api/v1/auth")
@RestController
public class AuthController {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AuthVerificationService authVerificationService;

    public AuthController(JwtService jwtService, UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder, AuthService authService,
                          AuthVerificationService authVerificationService) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.authVerificationService = authVerificationService;
    }

    /**
     * Login de la app móvil: correo + contraseña -> JWT.
     * El mensaje es el mismo para "no existe" y "clave incorrecta" para no revelar qué correos existen.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "Correo y contraseña son obligatorios");
        }

        String email = request.getEmail().trim();
        Usuario user = usuarioRepository.findByEmail(email)
                .or(() -> usuarioRepository.findByEmail(email.toLowerCase()))
                .or(() -> usuarioRepository.findByNombreUsuario(email))
                .orElse(null);

        if (user == null || user.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return error(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }
        if (!user.isActivo()) {
            return error(HttpStatus.FORBIDDEN, "La cuenta no está activada. Actívala con el código enviado a tu correo.");
        }
        if (user.getRol() == null) {
            return error(HttpStatus.FORBIDDEN, "El usuario no tiene un rol asignado");
        }

        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getEmail(), user.getRol().name()));
    }

    @PostMapping("/enviar-codigo")
    public ResponseEntity<String> enviarCodigo(@RequestParam String email) {
        try {
            authService.registrarYEnviarCodigo(email);
            return ResponseEntity.ok("Código enviado exitosamente");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/verificar")
    public ResponseEntity<?> verificarCodigo(@RequestParam String email, @RequestParam String code) {
        try {
            authVerificationService.activarCuenta(email, code);
            String correo = (email != null) ? email.trim().toLowerCase() : "";
            var user = usuarioRepository.findByEmail(correo)
                    .or(() -> usuarioRepository.findByNombreUsuario(email != null ? email.trim() : ""))
                    .orElseThrow(() -> new IllegalStateException("Usuario no encontrado tras activación"));
            return ResponseEntity.ok(new AuthResponse(
                    jwtService.generateToken(user),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRol() != null ? user.getRol().name() : null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("status", status.value(), "error", mensaje));
    }
}
