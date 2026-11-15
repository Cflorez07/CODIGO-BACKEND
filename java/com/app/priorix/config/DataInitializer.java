package com.app.priorix.config;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.app.priorix.Repository.AtencionRepository;
import com.app.priorix.Repository.HistoriaClinicaRepository;
import com.app.priorix.Repository.UsuarioRepository;
import com.app.priorix.model.entity.Atencion;
import com.app.priorix.model.entity.HistoriaClinica;
import com.app.priorix.model.entity.Usuario;
import com.app.priorix.model.enumeration.RolUsuario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el administrador inicial. La contraseña sale de la variable de
 * entorno ADMIN_INITIAL_PASSWORD (nunca se escribe en el código).
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private HistoriaClinicaRepository historiaClinicaRepository;

    @Autowired
    private AtencionRepository atencionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${ADMIN_INITIAL_PASSWORD:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        Usuario admin = usuarioRepository.findByNombreUsuario("admin").orElse(null);

        if (admin == null) {
            if (adminPassword.isBlank()) {
                System.out.println("No se creó el admin: define la variable de entorno ADMIN_INITIAL_PASSWORD.");
                repararHistoriasClinicas();
                return;
            }
            admin = new Usuario();
            admin.setNombreUsuario("admin");
            admin.setRol(RolUsuario.ROLE_ADMIN);
            admin.setEmail("adminpriorix@gmail.com");
            admin.setActivo(true);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            usuarioRepository.save(admin);
            System.out.println("Usuario administrador creado: admin");
            repararHistoriasClinicas();
            return;
        }

        // El admin ya existía (creado antes, sin contraseña): se la asigna una sola vez
        if (admin.getPassword() == null || admin.getPassword().isBlank()) {
            if (adminPassword.isBlank()) {
                System.out.println("El admin no tiene contraseña: define ADMIN_INITIAL_PASSWORD y reinicia.");
                repararHistoriasClinicas();
                return;
            }
            admin.setPassword(passwordEncoder.encode(adminPassword));
            usuarioRepository.save(admin);
            System.out.println("Se asignó contraseña al administrador existente.");
        } else {
            System.out.println("Usuario administrador ya existe, no se modifica.");
        }

        repararHistoriasClinicas();
    }

    private void repararHistoriasClinicas() {
        try {
            List<HistoriaClinica> todas = historiaClinicaRepository.findAll();
            for (HistoriaClinica h : todas) {
                boolean modificado = false;
                String oldId = h.getId();

                // Si se guardó con id vacío "" o null
                if (oldId == null || oldId.isBlank()) {
                    try {
                        historiaClinicaRepository.deleteById(oldId == null ? "" : oldId);
                    } catch (Exception ignored) {}
                    h.setId(null);
                    modificado = true;
                }

                Atencion at = null;
                if (h.getAtencion() != null && h.getAtencion().getId() != null) {
                    at = atencionRepository.findById(h.getAtencion().getId()).orElse(null);
                } else if (h.getPaciente() != null && h.getPaciente().getId() != null) {
                    for (Atencion a : atencionRepository.findAll()) {
                        if (a.getPaciente() != null && h.getPaciente().getId().equals(a.getPaciente().getId())) {
                            at = a;
                            break;
                        }
                    }
                }

                if (at != null) {
                    h.setAtencion(at);
                    if (h.getFechaAtencion() == null && at.getFechaAtencion() != null) {
                        h.setFechaAtencion(at.getFechaAtencion());
                        modificado = true;
                    }
                    if (h.getHoraAtencion() == null && at.getHoraAtencion() != null) {
                        h.setHoraAtencion(at.getHoraAtencion());
                        modificado = true;
                    }
                    if (h.getUsuario() == null && at.getMedico() != null) {
                        h.setUsuario(at.getMedico());
                        modificado = true;
                    }
                    if (at.getTriaje() != null) {
                        if ((h.getSintomasIniciales() == null || h.getSintomasIniciales().isBlank()) && at.getTriaje().getSintomas() != null) {
                            h.setSintomasIniciales(at.getTriaje().getSintomas());
                            modificado = true;
                        }
                        if (h.getNivelDolorInicial() == null && at.getTriaje().getNivelDolor() != null) {
                            h.setNivelDolorInicial(at.getTriaje().getNivelDolor());
                            modificado = true;
                        }
                        if (h.getNivelTriaje() == null && at.getTriaje().getNivel() != null) {
                            h.setNivelTriaje(at.getTriaje().getNivel().getColor());
                            modificado = true;
                        }
                    }
                }

                if (h.getFechaAtencion() == null) {
                    h.setFechaAtencion(LocalDate.now());
                    modificado = true;
                }
                if (h.getHoraAtencion() == null) {
                    h.setHoraAtencion(LocalTime.now());
                    modificado = true;
                }

                if (modificado) {
                    HistoriaClinica guardada = historiaClinicaRepository.save(h);
                    if (at != null && (at.getHistoriaClinica() == null || at.getHistoriaClinica().getId() == null)) {
                        at.setHistoriaClinica(guardada);
                        atencionRepository.save(at);
                    }
                    System.out.println("Historia clínica reparada con ID: " + guardada.getId());
                }
            }
        } catch (Exception e) {
            System.out.println("Nota sobre reparación de historias clínicas: " + e.getMessage());
        }
    }
}