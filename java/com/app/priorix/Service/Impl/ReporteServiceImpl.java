package com.app.priorix.Service.Impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.priorix.Repository.AtencionRepository;
import com.app.priorix.Service.ReporteService;
import com.app.priorix.model.entity.Atencion;
import com.app.priorix.model.enumeration.NivelTriaje;

@Service
public class ReporteServiceImpl implements ReporteService {

    @Autowired
    private AtencionRepository atencionRepository;

    @Override
    public List<Map<String, Object>> generarReporteGeneral() {
        return filtrarReportes(null, null, null, null);
    }

    @Override
    public List<Map<String, Object>> filtrarReportes(String fechaInicio, String fechaFin, String nivel, String medico) {
        LocalDate desde = fecha(fechaInicio);
        LocalDate hasta = fecha(fechaFin);
        Integer n = parseNivel(nivel);
        String m = medico == null ? "" : medico.trim().toLowerCase();

        List<Map<String, Object>> filas = new ArrayList<>();
        for (Atencion a : atencionRepository.findAll()) {
            if (a.getTriaje() == null || a.getPaciente() == null || a.getMedico() == null) continue;

            LocalDate f = a.getTriaje().getFechaRegistro();
            if (desde != null && (f == null || f.isBefore(desde))) continue;
            if (hasta != null && (f == null || f.isAfter(hasta))) continue;

            NivelTriaje nv = a.getTriaje().getNivel();
            if (n != null && (nv == null || nv.getNivel() != n)) continue;

            String med = a.getMedico().getUsername();
            if (!m.isEmpty() && (med == null || !med.toLowerCase().contains(m))) continue;

            Map<String, Object> r = new HashMap<>();
            r.put("paciente", a.getPaciente().getNombre() + " " + a.getPaciente().getApellido());
            r.put("medico", med);
            r.put("nivel", nv == null ? "-" : nv.getColor());
            r.put("nivelNumero", nv == null ? 5 : nv.getNivel());
            r.put("estado", a.getEstado() == null ? "-" : a.getEstado().name());
            r.put("fecha", f);
            r.put("tiempoEspera", espera(a));
            filas.add(r);
        }
        filas.sort(Comparator.comparing((Map<String, Object> r) -> (LocalDate) r.get("fecha"),
                Comparator.nullsLast(Comparator.<LocalDate>reverseOrder())));
        return filas;
    }

    // Minutos entre el registro del triaje y el momento en que el médico tomó al paciente
    private Long espera(Atencion a) {
        if (a.getFechaAtencion() == null || a.getHoraAtencion() == null
                || a.getTriaje().getHoraRegistro() == null) return null;
        LocalDateTime inicio = a.getTriaje().getHoraRegistro();
        LocalDateTime fin = LocalDateTime.of(a.getFechaAtencion(), a.getHoraAtencion());
        long min = Duration.between(inicio, fin).toMinutes();
        return min < 0 ? null : min;
    }

    private LocalDate fecha(String s) {
        try { return (s == null || s.isBlank()) ? null : LocalDate.parse(s); }
        catch (Exception e) { return null; }
    }

    private Integer parseNivel(String nivel) {
        if (nivel == null || nivel.isBlank()) return null;
        try {
            return Integer.valueOf(nivel.trim());
        } catch (NumberFormatException e) {
            for (NivelTriaje nt : NivelTriaje.values()) {
                if (nt.getColor().equalsIgnoreCase(nivel.trim()) || nt.name().equalsIgnoreCase(nivel.trim())) {
                    return nt.getNivel();
                }
            }
            return null;
        }
    }
}
