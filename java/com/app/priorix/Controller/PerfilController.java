package com.app.priorix.Controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.app.priorix.Repository.UsuarioRepository;
import com.app.priorix.model.entity.Usuario;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private static final long MAX_PHOTO_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    private final Path folderPerfiles = Paths.get("uploads", "perfiles").toAbsolutePath().normalize();

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public PerfilController() {
        try {
            Files.createDirectories(folderPerfiles);
        } catch (IOException e) {
            System.err.println("No se pudo inicializar la carpeta de perfiles: " + e.getMessage());
        }
    }

    @GetMapping
    public String mostrarPerfil(Authentication auth, Model model) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioRepository.findByNombreUsuario(auth.getName()).orElse(null);
        if (usuario == null) {
            return "redirect:/login";
        }

        String rolLayout = "enfermero";
        if (usuario.getRol() != null) {
            switch (usuario.getRol()) {
                case ROLE_ADMIN -> rolLayout = "admin";
                case ROLE_MEDICO -> rolLayout = "medico";
                case ROLE_ENFERMERO -> rolLayout = "enfermero";
            }
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("rolLayout", rolLayout);
        return "usuario/perfil";
    }

    @PostMapping("/actualizar")
    public String actualizarDatos(@RequestParam(required = false) String nombreCompleto,
                                  @RequestParam(required = false) String especialidad,
                                  @RequestParam(required = false) String telefono,
                                  Authentication auth) {
        if (auth == null) return "redirect:/login";

        Usuario usuario = usuarioRepository.findByNombreUsuario(auth.getName()).orElse(null);
        if (usuario == null) return "redirect:/login";

        // Actualización estricta de campos permitidos únicamente
        usuario.setNombreCompleto(nombreCompleto != null ? nombreCompleto.trim() : null);
        usuario.setEspecialidad(especialidad != null ? especialidad.trim() : null);
        usuario.setTelefono(telefono != null ? telefono.trim() : null);

        usuarioRepository.save(usuario);
        return "redirect:/perfil?actualizado";
    }

    @PostMapping("/foto")
    public Object subirFoto(@RequestParam("foto") MultipartFile foto,
                            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                            @RequestHeader(value = "Accept", required = false) String acceptHeader,
                            Authentication auth) {
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(requestedWith) 
                || (acceptHeader != null && acceptHeader.contains("application/json"));

        if (auth == null || !auth.isAuthenticated()) {
            return isAjax ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("error", "No autenticado"))
                          : "redirect:/login";
        }

        Usuario usuario = usuarioRepository.findByNombreUsuario(auth.getName()).orElse(null);
        if (usuario == null) {
            return isAjax ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("error", "Usuario no encontrado"))
                          : "redirect:/login";
        }

        if (foto == null || foto.isEmpty()) {
            return isAjax ? ResponseEntity.badRequest().body(java.util.Map.of("error", "Por favor selecciona una imagen válida antes de guardar."))
                          : "redirect:/perfil?errorFoto=vacio";
        }

        if (foto.getSize() > MAX_PHOTO_SIZE) {
            return isAjax ? ResponseEntity.badRequest().body(java.util.Map.of("error", "La imagen supera el tamaño máximo permitido de 5 MB."))
                          : "redirect:/perfil?errorFoto=tamano";
        }

        String contentType = foto.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            return isAjax ? ResponseEntity.badRequest().body(java.util.Map.of("error", "Formato no permitido. Solo se aceptan archivos JPG, PNG o WEBP."))
                          : "redirect:/perfil?errorFoto=formato";
        }

        try {
            Files.createDirectories(folderPerfiles);

            String orig = foto.getOriginalFilename();
            String extension = ".jpg";
            if (orig != null && orig.contains(".")) {
                extension = orig.substring(orig.lastIndexOf(".")).toLowerCase();
            }

            String filename = "usuario_" + usuario.getId() + extension;
            Path target = folderPerfiles.resolve(filename);

            Files.copy(foto.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String url = "/perfil/foto/" + usuario.getId() + "?v=" + System.currentTimeMillis();
            usuario.setFotoUrl(url);
            usuarioRepository.save(usuario);

            if (isAjax) {
                java.util.Map<String, Object> resp = new java.util.HashMap<>();
                resp.put("success", true);
                resp.put("fotoUrl", url);
                resp.put("mensaje", "Fotografía de perfil actualizada exitosamente.");
                return ResponseEntity.ok(resp);
            }
            return "redirect:/perfil?fotoActualizada";
        } catch (IOException e) {
            return isAjax ? ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of("error", "Error interno al guardar la imagen."))
                          : "redirect:/perfil?errorFoto=error";
        }
    }

    @GetMapping("/foto/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> obtenerFoto(@PathVariable String id) {
        if (id == null || id.isBlank()) return ResponseEntity.notFound().build();

        try {
            // Buscar archivo correspondiente al usuario
            File dir = folderPerfiles.toFile();
            if (!dir.exists()) return ResponseEntity.notFound().build();

            File[] matching = dir.listFiles((d, name) -> name.startsWith("usuario_" + id + "."));
            if (matching == null || matching.length == 0) {
                return ResponseEntity.notFound().build();
            }

            File photoFile = matching[0];
            byte[] bytes = Files.readAllBytes(photoFile.toPath());

            String mime = Files.probeContentType(photoFile.toPath());
            if (mime == null) mime = "image/jpeg";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(mime));
            headers.setCacheControl("max-age=3600, must-revalidate");

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/foto/eliminar")
    public Object eliminarFoto(@RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                               @RequestHeader(value = "Accept", required = false) String acceptHeader,
                               Authentication auth) {
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(requestedWith) 
                || (acceptHeader != null && acceptHeader.contains("application/json"));

        if (auth == null) {
            return isAjax ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).build() : "redirect:/login";
        }

        Usuario usuario = usuarioRepository.findByNombreUsuario(auth.getName()).orElse(null);
        if (usuario == null) {
            return isAjax ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).build() : "redirect:/login";
        }

        usuario.setFotoUrl(null);
        usuarioRepository.save(usuario);

        // Opcionalmente borrar archivo en disco
        try {
            File dir = folderPerfiles.toFile();
            if (dir.exists()) {
                File[] matching = dir.listFiles((d, name) -> name.startsWith("usuario_" + usuario.getId() + "."));
                if (matching != null) {
                    for (File f : matching) f.delete();
                }
            }
        } catch (Exception ignored) {}

        if (isAjax) {
            java.util.Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("success", true);
            resp.put("iniciales", usuario.getIniciales());
            resp.put("mensaje", "Fotografía de perfil eliminada correctamente.");
            return ResponseEntity.ok(resp);
        }
        return "redirect:/perfil?fotoEliminada";
    }

    @PostMapping("/cambiar-clave")
    public String cambiarClave(@RequestParam("claveActual") String claveActual,
                               @RequestParam("nuevaClave") String nuevaClave,
                               @RequestParam("confirmarClave") String confirmarClave,
                               Authentication auth,
                               Model model) {
        if (auth == null) return "redirect:/login";

        Usuario usuario = usuarioRepository.findByNombreUsuario(auth.getName()).orElse(null);
        if (usuario == null) return "redirect:/login";

        String rolLayout = "enfermero";
        if (usuario.getRol() != null) {
            switch (usuario.getRol()) {
                case ROLE_ADMIN -> rolLayout = "admin";
                case ROLE_MEDICO -> rolLayout = "medico";
                case ROLE_ENFERMERO -> rolLayout = "enfermero";
            }
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("rolLayout", rolLayout);

        if (!passwordEncoder.matches(claveActual, usuario.getPassword())) {
            model.addAttribute("errorClave", "La contraseña actual ingresada es incorrecta.");
            return "usuario/perfil";
        }

        if (nuevaClave == null || nuevaClave.length() < 6) {
            model.addAttribute("errorClave", "La nueva contraseña debe tener al menos 6 caracteres.");
            return "usuario/perfil";
        }

        if (!nuevaClave.equals(confirmarClave)) {
            model.addAttribute("errorClave", "Las nuevas contraseñas no coinciden.");
            return "usuario/perfil";
        }

        usuario.setPassword(passwordEncoder.encode(nuevaClave));
        usuarioRepository.save(usuario);

        return "redirect:/perfil?claveCambiada";
    }
}
