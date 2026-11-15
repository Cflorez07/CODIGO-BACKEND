package com.app.priorix.Controller;

import com.app.priorix.Service.AuthVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ActivacionController {

    @Autowired
    private AuthVerificationService authVerificationService;

    @Autowired
    private com.app.priorix.Service.AuthService authService;

    // Muestra la página web
    @GetMapping("/activar")
    public String mostrarFormulario() {
        return "activar"; // Busca el archivo activar.html
    }

    @PostMapping("/activar/enviar-codigo")
    public String enviarCodigo(@RequestParam String email, Model model) {
        try {
            authService.registrarYEnviarCodigo(email);
            model.addAttribute("mensaje", "Si la cuenta fue creada por el administrador y aún está pendiente, enviamos un código válido por 10 minutos.");
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("emailIngresado", email);
        return "activar";
    }

    // Procesa los datos cuando le dan a "Activar Cuenta"
    @PostMapping("/activar")
    public String procesarActivacion(@RequestParam String email, @RequestParam String code, Model model) {
        try {
            authVerificationService.activarCuenta(email, code);
            // Si todo sale bien, lo manda al login con un mensaje de éxito
            return "redirect:/login?activado"; 
        } catch (RuntimeException e) {
            // Si el código es falso o expiró, recarga la página mostrando el error
            model.addAttribute("error", e.getMessage());
            return "activar";
        }
    }
}
