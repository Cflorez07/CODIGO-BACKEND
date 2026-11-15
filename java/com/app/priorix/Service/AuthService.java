package com.app.priorix.Service;

import java.time.LocalDateTime;
import java.security.SecureRandom;


import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.app.priorix.Repository.OtpCodeRepository;
import com.app.priorix.Repository.UsuarioRepository;
import com.app.priorix.model.entity.OtpCode;
import com.app.priorix.model.entity.Usuario;

@Service
public class AuthService {

    private final OtpCodeRepository otpCodeRepository;
    private final UsuarioRepository usuarioRepository;
    private final JavaMailSender mailSender;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    

    public AuthService(OtpCodeRepository otpCodeRepository, 
                       UsuarioRepository usuarioRepository, 
                       JavaMailSender mailSender) {
        this.otpCodeRepository = otpCodeRepository;
        this.usuarioRepository = usuarioRepository;
        this.mailSender = mailSender;
    }

    public void registrarYEnviarCodigo(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Debes indicar el correo institucional.");
        }
        String correo = email.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmail(correo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El correo no está autorizado. Solicita al administrador la creación de tu cuenta."));
        if (usuario.isActivo()) {
            throw new IllegalStateException("La cuenta ya está activa. Puedes iniciar sesión.");
        }
        if (usuario.getRol() == null || usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new IllegalStateException("La cuenta aún no está configurada por el administrador.");
        }

        otpCodeRepository.deleteByEmail(correo);
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        OtpCode otp = new OtpCode();
        otp.setEmail(correo);
        otp.setCode(code);
        otp.setExpirationTime(LocalDateTime.now().plusMinutes(10));
        otpCodeRepository.save(otp);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(correo);
        message.setSubject("Código de activación PRIORIX");
        message.setText("Tu código de activación es: " + code + ". Vence en 10 minutos.");
        mailSender.send(message);
    }

    
}
