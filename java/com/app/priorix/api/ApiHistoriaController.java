package com.app.priorix.api;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.priorix.Service.AtencionService;
import com.app.priorix.Service.HistoriaClinicaService;
import com.app.priorix.api.dto.ApiDtos.HistoriaResponse;
import com.app.priorix.model.entity.Atencion;
import com.app.priorix.model.entity.HistoriaClinica;
import com.app.priorix.model.entity.Triaje;
import com.app.priorix.model.entity.Usuario;
import com.app.priorix.model.enumeration.EstadoAtencion;

@RestController
@RequestMapping("/api/v1/historias")
public class ApiHistoriaController {

    private final HistoriaClinicaService historiaService;
    private final AtencionService atencionService;
    private final UsuarioActual usuarioActual;

    public ApiHistoriaController(HistoriaClinicaService historiaService, AtencionService atencionService,
                                 UsuarioActual usuarioActual) {
        this.historiaService = historiaService;
        this.atencionService = atencionService;
        this.usuarioActual = usuarioActual;
    }

    /**
     * Crea la historia clínica a partir de una atención ya finalizada. El servidor copia los datos del triaje
     * (síntomas, dolor, nivel) y de la atención (motivo, diagnóstico, tratamiento, observaciones), así no
     * dependen de lo que mande el cliente.
     */
    @PostMapping("/desde-atencion/{idAtencion}")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<HistoriaResponse> crearDesdeAtencion(@PathVariable String idAtencion, Authentication auth) {
        Usuario medico = usuarioActual.de(auth);
        Atencion at = atencionService.obtener(idAtencion);
        if (at == null) throw new ApiException(HttpStatus.NOT_FOUND, "Atención no encontrada");
        if (at.getMedico() == null || !medico.getId().equals(at.getMedico().getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Esta atención pertenece a otro médico");
        }
        if (at.getEstado() != EstadoAtencion.ATENDIDO) {
            throw new ApiException(HttpStatus.CONFLICT, "Finaliza la atención antes de crear la historia clínica");
        }
        if (at.getHistoriaClinica() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "Esta atención ya tiene historia clínica");
        }

        HistoriaClinica h = new HistoriaClinica();
        h.setPaciente(at.getPaciente());
        h.setAtencion(at);
        h.setUsuario(medico);

        Triaje t = at.getTriaje();
        if (t != null) {
            h.setSintomasIniciales(t.getSintomas());
            h.setNivelDolorInicial(t.getNivelDolor());
            h.setNivelTriaje(t.getNivel() == null ? null : t.getNivel().getColor());
        }
        h.prePersist();   // copia fecha, hora, motivo, diagnóstico, tratamiento y observaciones de la atención

        HistoriaClinica guardada = historiaService.guardar(h);
        at.setHistoriaClinica(guardada);
        atencionService.guardar(at);
        return ResponseEntity.status(HttpStatus.CREATED).body(HistoriaResponse.from(guardada));
    }

    /** Historias de un paciente, de la más reciente a la más antigua. */
    @GetMapping
    @PreAuthorize("hasAnyRole('MEDICO','ADMIN')")
    public List<HistoriaResponse> porPaciente(@RequestParam String pacienteId) {
        return historiaService.findAll().stream()
                .filter(h -> h.getPaciente() != null && pacienteId.equals(h.getPaciente().getId()))
                .sorted(Comparator.comparing(HistoriaClinica::getFechaAtencion,
                        Comparator.nullsLast(Comparator.<LocalDate>reverseOrder())))
                .map(HistoriaResponse::from)
                .toList();
    }
}
