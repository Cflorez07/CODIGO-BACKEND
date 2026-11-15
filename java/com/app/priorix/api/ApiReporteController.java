package com.app.priorix.api;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.priorix.Service.AtencionService;
import com.app.priorix.api.dto.ApiDtos.ReporteItem;
import com.app.priorix.api.dto.ApiDtos.ReporteResponse;
import com.app.priorix.model.entity.Atencion;
import com.app.priorix.model.enumeration.NivelTriaje;

@RestController
@RequestMapping("/api/v1/reportes")
@PreAuthorize("hasRole('ADMIN')")
public class ApiReporteController {

    private final AtencionService atencionService;

    public ApiReporteController(AtencionService atencionService) {
        this.atencionService = atencionService;
    }

    /**
     * Reporte de atenciones con filtros opcionales:
     *   ?fechaInicio=2026-11-01&fechaFin=2026-11-30&nivel=1   (nivel: 1 a 5, o el color: rojo, naranja...)
     * Devuelve totales por nivel y por estado, y el detalle. Los registros con datos incompletos se omiten.
     */
    @GetMapping
    public ReporteResponse reporte(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String nivel) {

        if (fechaInicio != null && fechaFin != null && fechaFin.isBefore(fechaInicio)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha final no puede ser anterior a la inicial");
        }
        Integer nivelFiltro = parseNivel(nivel);

        List<ReporteItem> items = atencionService.listar().stream()
                .map(this::aItem)
                .filter(Objects::nonNull)
                .filter(i -> fechaInicio == null || (i.fecha() != null && !i.fecha().isBefore(fechaInicio)))
                .filter(i -> fechaFin == null || (i.fecha() != null && !i.fecha().isAfter(fechaFin)))
                .filter(i -> nivelFiltro == null || nivelFiltro.equals(i.nivel()))
                .toList();

        Map<String, Long> porNivel = new LinkedHashMap<>();
        for (NivelTriaje n : NivelTriaje.values()) {
            porNivel.put(n.getColor(), items.stream().filter(i -> Integer.valueOf(n.getNivel()).equals(i.nivel())).count());
        }
        Map<String, Long> porEstado = items.stream().collect(
                Collectors.groupingBy(i -> String.valueOf(i.estado()), LinkedHashMap::new, Collectors.counting()));

        return new ReporteResponse(items.size(), porNivel, porEstado, items);
    }

    private ReporteItem aItem(Atencion a) {
        try {
            if (a.getTriaje() == null || a.getTriaje().getNivel() == null) return null;
            NivelTriaje n = a.getTriaje().getNivel();
            String paciente = a.getPaciente() == null ? "—"
                    : (a.getPaciente().getNombre() + " " + a.getPaciente().getApellido()).trim();
            return new ReporteItem(paciente, n.getNivel(), n.getColor(),
                    a.getEstado() == null ? null : a.getEstado().name(),
                    a.getMedico() == null ? null : a.getMedico().getUsername(),
                    a.getTriaje().getFechaRegistro());
        } catch (RuntimeException e) {
            return null;   // referencia rota (documento borrado): se omite en lugar de romper todo el reporte
        }
    }

    private Integer parseNivel(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String t = texto.trim();
        for (NivelTriaje n : NivelTriaje.values()) {
            if (String.valueOf(n.getNivel()).equals(t) || n.getColor().equalsIgnoreCase(t)) return n.getNivel();
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Nivel inválido. Usa 1 a 5 o el color (rojo, naranja, amarillo, azul, verde)");
    }
}
