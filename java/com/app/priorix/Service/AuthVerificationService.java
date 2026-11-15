package com.app.priorix.Service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.app.priorix.Repository.OtpCodeRepository;
import com.app.priorix.Repository.UsuarioRepository;
import com.app.priorix.model.entity.OtpCode;
import com.app.priorix.model.entity.Usuario;

@Service
public class AuthVerificationService {

    private final OtpCodeRepository otpCodeRepository;
    private final UsuarioRepository usuarioRepository;

    public AuthVerificationService(OtpCodeRepository otpCodeRepository, UsuarioRepository usuarioRepository) {
        this.otpCodeRepository = otpCodeRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public void activarCuenta(String email, String code) {
        if (email == null || email.isBlank() || code == null || !code.matches("\\d{6}")) {
            throw new RuntimeException("Correo o código inválido");
        }
        String correo = email.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmail(correo)
                .orElseThrow(() -> new RuntimeException("La cuenta no existe. Solicita al administrador su creación."));
        if (usuario.getRol() == null || usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new RuntimeException("La cuenta no está configurada por el administrador.");
        }
        if (usuario.isActivo()) {
            throw new RuntimeException("La cuenta ya se encuentra activa.");
        }
        // 1. Buscar el código en MongoDB
        OtpCode otp = otpCodeRepository.findByEmailAndCode(correo, code)
                .orElseThrow(() -> new RuntimeException("Código inválido o correo incorrecto"));

        // 2. Verificar si expiró
        if (otp.getExpirationTime().isBefore(LocalDateTime.now())) {
            otpCodeRepository.delete(otp); // Borramos el código inservible
            throw new RuntimeException("El código ha expirado");
        }

        // 3. Buscar al usuario y activarlo
        usuario.setActivo(true); // ¡Aquí ocurre la magia de la activación!
        usuarioRepository.save(usuario);

        // 4. Eliminar el código usado
        otpCodeRepository.delete(otp);
    }
}
