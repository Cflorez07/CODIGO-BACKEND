package com.app.priorix.Controller;

import java.time.LocalDate;
import java.time.LocalTime;

import com.app.priorix.model.entity.Atencion;
import com.app.priorix.model.entity.HistoriaClinica;
import com.app.priorix.model.entity.Paciente;
import com.app.priorix.Service.AtencionService;
import com.app.priorix.Service.HistoriaClinicaService;
import com.app.priorix.Service.PacienteService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/medico/historia")
public class HistoriaClinicaController {

    @Autowired
    private HistoriaClinicaService historiaClinicaService;

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private AtencionService atencionService;

    @GetMapping("/crear/{idAtencion}")
    public String crear(@PathVariable String idAtencion, Model model) {
        Atencion at = atencionService.obtener(idAtencion);
        if (at == null) return "redirect:/medico/lista-espera";

        HistoriaClinica h = new HistoriaClinica();
        h.setPaciente(at.getPaciente());
        h.setAtencion(at);
        if (at.getMedico() != null) {
            h.setUsuario(at.getMedico());
        }
        if (at.getTriaje() != null) {
            h.setSintomasIniciales(at.getTriaje().getSintomas());
            h.setNivelDolorInicial(at.getTriaje().getNivelDolor());
            if (at.getTriaje().getNivel() != null) {
                h.setNivelTriaje(at.getTriaje().getNivel().getColor());
            }
        }
        
        // Copiar datos de la atención
        h.setFechaAtencion(at.getFechaAtencion() != null ? at.getFechaAtencion() : LocalDate.now());
        h.setHoraAtencion(at.getHoraAtencion() != null ? at.getHoraAtencion() : LocalTime.now());
        h.setMotivoConsulta(at.getMotivoConsulta());
        h.setDiagnostico(at.getDiagnostico());
        h.setTratamiento(at.getTratamiento());
        h.setObservaciones(at.getObservaciones());

        model.addAttribute("historia", h);
        return "medico/historia";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute HistoriaClinica historia) {
        try {
            // 1. Si id viene vacío "", asignamos null para que MongoDB autogenere el ObjectId
            if (historia.getId() != null && historia.getId().isBlank()) {
                historia.setId(null);
            }

            // 2. Asociar paciente
            if (historia.getPaciente() != null && historia.getPaciente().getId() != null) {
                Paciente p = pacienteService.obtenerPorId(historia.getPaciente().getId());
                historia.setPaciente(p);
            }

            // 3. Asociar atención y sincronizar datos faltantes
            Atencion at = null;
            if (historia.getAtencion() != null && historia.getAtencion().getId() != null) {
                at = atencionService.obtener(historia.getAtencion().getId());
                if (at != null) {
                    historia.setAtencion(at);
                    if (historia.getUsuario() == null && at.getMedico() != null) {
                        historia.setUsuario(at.getMedico());
                    }
                    if (at.getTriaje() != null) {
                        if (historia.getSintomasIniciales() == null || historia.getSintomasIniciales().isBlank()) {
                            historia.setSintomasIniciales(at.getTriaje().getSintomas());
                        }
                        if (historia.getNivelDolorInicial() == null) {
                            historia.setNivelDolorInicial(at.getTriaje().getNivelDolor());
                        }
                        if (historia.getNivelTriaje() == null && at.getTriaje().getNivel() != null) {
                            historia.setNivelTriaje(at.getTriaje().getNivel().getColor());
                        }
                    }
                    if (historia.getFechaAtencion() == null) {
                        historia.setFechaAtencion(at.getFechaAtencion() != null ? at.getFechaAtencion() : LocalDate.now());
                    }
                    if (historia.getHoraAtencion() == null) {
                        historia.setHoraAtencion(at.getHoraAtencion() != null ? at.getHoraAtencion() : LocalTime.now());
                    }
                }
            }

            // 4. Asegurar que fecha y hora nunca sean nulas
            if (historia.getFechaAtencion() == null) {
                historia.setFechaAtencion(LocalDate.now());
            }
            if (historia.getHoraAtencion() == null) {
                historia.setHoraAtencion(LocalTime.now());
            }
            
            HistoriaClinica guardada = historiaClinicaService.guardar(historia);

            // 5. Vincular historia clínica en la atención
            if (at != null) {
                at.setHistoriaClinica(guardada);
                atencionService.guardar(at);
            }

            return "redirect:/medico/historias?save=ok";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/medico/lista-espera?error=500";
        }
    }
}
