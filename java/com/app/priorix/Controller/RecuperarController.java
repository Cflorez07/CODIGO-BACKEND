package com.app.priorix.Controller;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.priorix.Repository.OtpCodeRepository;
import com.app.priorix.Repository.UsuarioRepository;
import com.app.priorix.model.entity.OtpCode;
import com.app.priorix.model.entity.Usuario;

@Controller
@RequestMapping("/recuperar")
public class RecuperarController {

    private static final Logger log = LoggerFactory.getLogger(RecuperarController.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private OtpCodeRepository otpCodeRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public String mostrarRecuperar(Model model) {
        model.addAttribute("paso", 1);
        return "recuperar";
    }

    @PostMapping("/solicitar")
    public String solicitarCodigo(@RequestParam("email") String email, Model model) {
        if (email == null || email.isBlank()) {
            model.addAttribute("error", "Debes ingresar tu correo electrónico institucional.");
            model.addAttribute("paso", 1);
            return "recuperar";
        }

        String correo = email.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmail(correo).orElse(null);

        if (usuario != null && !usuario.isActivo()) {
            model.addAttribute("error", "Esta cuenta se encuentra inactiva. Contacta al administrador.");
            model.addAttribute("paso", 1);
            return "recuperar";
        }

        if (usuario != null && usuario.isActivo()) {
            otpCodeRepository.deleteByEmail(correo);
            String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
            OtpCode otp = new OtpCode();
            otp.setEmail(correo);
            otp.setCode(code);
            otp.setExpirationTime(LocalDateTime.now().plusMinutes(15));
            otpCodeRepository.save(otp);

            if (mailSender != null) {
                try {
                    SimpleMailMessage msg = new SimpleMailMessage();
                    msg.setTo(correo);
                    msg.setSubject("Recuperación de contraseña - PRIORIX");
                    msg.setText("Tu código de verificación para restablecer tu contraseña en PRIORIX es: "
                            + code + "\n\nEste código es válido por 15 minutos. Si no solicitaste este cambio, ignora este mensaje.");
                    mailSender.send(msg);
                } catch (Exception e) {
                    log.warn("No se pudo enviar el correo de recuperación a {}: {}", correo, e.getMessage());
                }
            } else {
                log.info("JavaMailSender no disponible. Código OTP para {}: {}", correo, code);
            }
        }

        model.addAttribute("mensaje", "Si el correo está registrado y activo, enviamos un código de verificación.");
        model.addAttribute("email", correo);
        model.addAttribute("paso", 2);
        return "recuperar";
    }

    @PostMapping("/cambiar")
    public String cambiarPassword(@RequestParam("email") String email,
                                  @RequestParam("code") String code,
                                  @RequestParam("nuevaPassword") String nuevaPassword,
                                  @RequestParam("confirmarPassword") String confirmarPassword,
                                  Model model) {
        String correo = email != null ? email.trim().toLowerCase() : "";
        model.addAttribute("email", correo);
        model.addAttribute("paso", 2);

        if (correo.isBlank() || code == null || !code.trim().matches("\\d{6}")) {
            model.addAttribute("error", "El código debe contener 6 dígitos numéricos.");
            return "recuperar";
        }

        if (nuevaPassword == null || nuevaPassword.length() < 6) {
            model.addAttribute("error", "La contraseña debe tener al menos 6 caracteres.");
            return "recuperar";
        }

        if (!nuevaPassword.equals(confirmarPassword)) {
            model.addAttribute("error", "Las contraseñas ingresadas no coinciden.");
            return "recuperar";
        }

        OtpCode otp = otpCodeRepository.findByEmailAndCode(correo, code.trim()).orElse(null);
        if (otp == null) {
            model.addAttribute("error", "Código de verificación incorrecto.");
            return "recuperar";
        }

        if (otp.getExpirationTime().isBefore(LocalDateTime.now())) {
            otpCodeRepository.delete(otp);
            model.addAttribute("error", "El código ha expirado. Solicita uno nuevo.");
            return "recuperar";
        }

        Usuario usuario = usuarioRepository.findByEmail(correo).orElse(null);
        if (usuario == null || !usuario.isActivo()) {
            model.addAttribute("error", "Esta cuenta no existe o se encuentra inactiva. Contacta al administrador.");
            return "recuperar";
        }

        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
        otpCodeRepository.delete(otp);

        return "redirect:/login?recuperado";
    }
}
