package com.app.priorix.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.priorix.Service.AtencionService;
import com.app.priorix.Service.ListaEsperaService;
import com.app.priorix.api.dto.ApiDtos.AtencionRequest;
import com.app.priorix.api.dto.ApiDtos.AtencionResponse;
import com.app.priorix.model.entity.Atencion;
import com.app.priorix.model.entity.Usuario;
import com.app.priorix.model.enumeration.EstadoAtencion;
import com.app.priorix.model.node.NodoPaciente;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/atenciones")
@PreAuthorize("hasRole('MEDICO')")
public class ApiAtencionController {

    private final ListaEsperaService listaEspera;
    private final AtencionService atencionService;
    private final UsuarioActual usuarioActual;

    public ApiAtencionController(ListaEsperaService listaEspera, AtencionService atencionService,
                                 UsuarioActual usuarioActual) {
        this.listaEspera = listaEspera;
        this.atencionService = atencionService;
        this.usuarioActual = usuarioActual;
    }

    /**
     * El médico toma un paciente de la lista de espera. El paciente sale de la fila y se crea una atención EN_PROCESO.
     * Si otro médico ya lo tomó responde 409.
     */
    @PostMapping("/iniciar/{idTriaje}")
    public ResponseEntity<AtencionResponse> iniciar(@PathVariable String idTriaje, Authentication auth) {
        Usuario medico = usuarioActual.de(auth);

        NodoPaciente nodo;
        synchronized (listaEspera) {   // buscar + quitar como una sola operación: dos médicos no toman al mismo paciente
            nodo = listaEspera.buscarPorIdTriaje(idTriaje);
            if (nodo == null) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "El paciente ya no está en la lista de espera (otro médico pudo haberlo tomado)");
            }
            listaEspera.eliminar(idTriaje);
        }

        Atencion at = new Atencion();
        at.setPaciente(nodo.getPaciente());
        at.setTriaje(nodo.getTriaje());
        at.setMedico(medico);
        at.setEstado(EstadoAtencion.EN_PROCESO);
        at.prePersist();   // fecha y hora de inicio (conserva el estado EN_PROCESO)

        Atencion guardada;
        try {
            guardada = atencionService.guardar(at);
        } catch (RuntimeException e) {
            // Si falla el guardado el paciente vuelve a la fila, para que no se pierda
            nodo.setSiguiente(null);
            synchronized (listaEspera) {
                listaEspera.agregar(nodo);
            }
            throw e;
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(AtencionResponse.from(guardada));
    }

    /** Atenciones EN_PROCESO del médico (para retomar una si la app se cerró a la mitad). */
    @GetMapping("/en-proceso")
    public List<AtencionResponse> enProceso(Authentication auth) {
        Usuario medico = usuarioActual.de(auth);
        return atencionService.listar().stream()
                .filter(a -> a.getEstado() == EstadoAtencion.EN_PROCESO
                        && a.getMedico() != null && medico.getId().equals(a.getMedico().getId()))
                .map(AtencionResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public AtencionResponse obtener(@PathVariable String id, Authentication auth) {
        return AtencionResponse.from(propia(id, auth));
    }

    /** Guarda diagnóstico y tratamiento y marca la atención como ATENDIDO. */
    @PutMapping("/{id}/finalizar")
    public AtencionResponse finalizar(@PathVariable String id, @Valid @RequestBody AtencionRequest r,
                                      Authentication auth) {
        Atencion at = propia(id, auth);
        if (at.getEstado() == EstadoAtencion.ATENDIDO) {
            throw new ApiException(HttpStatus.CONFLICT, "Esta atención ya fue finalizada");
        }

        String motivo = r.motivoConsulta();
        if ((motivo == null || motivo.isBlank()) && at.getTriaje() != null) {
            motivo = at.getTriaje().getSintomas();   // por defecto, el motivo es lo que registró enfermería
        }
        at.setMotivoConsulta(motivo);
        at.setDiagnostico(r.diagnostico().trim());
        at.setTratamiento(r.tratamiento());
        at.setObservaciones(r.observaciones());
        at.setEstado(EstadoAtencion.ATENDIDO);
        return AtencionResponse.from(atencionService.guardar(at));
    }

    /** Busca la atención y verifica que pertenezca al médico del token. */
    private Atencion propia(String id, Authentication auth) {
        Atencion at = atencionService.obtener(id);
        if (at == null) throw new ApiException(HttpStatus.NOT_FOUND, "Atención no encontrada");
        Usuario medico = usuarioActual.de(auth);
        if (at.getMedico() == null || !medico.getId().equals(at.getMedico().getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Esta atención pertenece a otro médico");
        }
        return at;
    }
}
