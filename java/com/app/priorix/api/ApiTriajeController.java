package com.app.priorix.api;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.priorix.Service.ListaEsperaService;
import com.app.priorix.Service.PacienteService;
import com.app.priorix.Service.TriajeService;
import com.app.priorix.api.dto.ApiDtos.TriajeRequest;
import com.app.priorix.api.dto.ApiDtos.TriajeResponse;
import com.app.priorix.model.entity.Paciente;
import com.app.priorix.model.entity.Triaje;
import com.app.priorix.model.entity.Usuario;
import com.app.priorix.model.node.NodoPaciente;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/triajes")
public class ApiTriajeController {

    private final PacienteService pacienteService;
    private final TriajeService triajeService;
    private final ListaEsperaService listaEspera;
    private final UsuarioActual usuarioActual;

    public ApiTriajeController(PacienteService pacienteService, TriajeService triajeService,
                               ListaEsperaService listaEspera, UsuarioActual usuarioActual) {
        this.pacienteService = pacienteService;
        this.triajeService = triajeService;
        this.listaEspera = listaEspera;
        this.usuarioActual = usuarioActual;
    }

    /**
     * Registra el triaje. El nivel lo calcula el servidor con las mismas reglas de la web
     * (inconsciente, SpO2 < 90, fiebre >= 40, y si no, nivel de dolor) y el paciente entra a la lista de espera.
     */
    @PostMapping
    @PreAuthorize("hasRole('ENFERMERO')")
    public ResponseEntity<TriajeResponse> registrar(@Valid @RequestBody TriajeRequest r, Authentication auth) {
        Paciente paciente = pacienteService.obtenerPorId(r.pacienteId());
        if (paciente == null) throw new ApiException(HttpStatus.NOT_FOUND, "Paciente no encontrado");

        Triaje t = new Triaje();
        t.setPaciente(paciente);
        t.setSintomas(r.sintomas().trim());
        t.setFrecuenciaCardiaca(r.frecuenciaCardiaca());
        t.setFrecuenciaRespiratoria(r.frecuenciaRespiratoria());
        t.setPresionArterial(r.presionArterial());
        t.setTemperatura(r.temperatura());
        t.setSaturacionOxigeno(r.saturacionOxigeno());
        t.setEstadoConsciencia(r.estadoConsciencia());
        t.setAlergias(r.alergias());
        t.setEnfermedadesPrevias(r.enfermedadesPrevias());
        t.setNivelDolor(r.nivelDolor());
        t.prePersist();   // fechas + reglas médicas + nivel + prioridad

        if (t.getPrioridad() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No fue posible clasificar el triaje");
        }
        t.setEnfermero(usuarioActual.de(auth));

        Triaje guardado = triajeService.guardar(t);
        listaEspera.agregar(new NodoPaciente(guardado));
        return ResponseEntity.status(HttpStatus.CREATED).body(TriajeResponse.from(guardado));
    }

    /** Triajes registrados por el enfermero del token, del más reciente al más antiguo. */
    @GetMapping("/mios")
    @PreAuthorize("hasRole('ENFERMERO')")
    public List<TriajeResponse> mios(Authentication auth) {
        Usuario enf = usuarioActual.de(auth);
        // Filtro en memoria: es seguro con @DocumentReference y suficiente para el volumen del proyecto
        return triajeService.listar().stream()
                .filter(t -> t.getEnfermero() != null && enf.getId().equals(t.getEnfermero().getId()))
                .sorted(Comparator.comparing(Triaje::getHoraRegistro,
                        Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())))
                .map(TriajeResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ENFERMERO','MEDICO')")
    public TriajeResponse obtener(@PathVariable String id) {
        Triaje t = triajeService.obtenerPorId(id);
        if (t == null) throw new ApiException(HttpStatus.NOT_FOUND, "Triaje no encontrado");
        return TriajeResponse.from(t);
    }
}
