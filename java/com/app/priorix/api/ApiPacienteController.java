package com.app.priorix.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.priorix.Repository.PacienteRepository;
import com.app.priorix.Service.PacienteService;
import com.app.priorix.api.dto.ApiDtos.PacienteRequest;
import com.app.priorix.api.dto.ApiDtos.PacienteResponse;
import com.app.priorix.model.entity.Paciente;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/pacientes")
public class ApiPacienteController {

    private static final String TIPO_NN = "NN";

    private final PacienteService pacienteService;
    private final PacienteRepository pacienteRepository;

    public ApiPacienteController(PacienteService pacienteService, PacienteRepository pacienteRepository) {
        this.pacienteService = pacienteService;
        this.pacienteRepository = pacienteRepository;
    }

    /** Buscar por nombre o documento (?q=). Lo usan enfermería (elegir paciente) y administración. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ENFERMERO','MEDICO')")
    public List<PacienteResponse> listar(@RequestParam(required = false) String q) {
        String filtro = (q == null || q.isBlank()) ? null : q.trim();
        return pacienteService.listar(filtro).stream().map(PacienteResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ENFERMERO','MEDICO')")
    public PacienteResponse obtener(@PathVariable String id) {
        return PacienteResponse.from(existente(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PacienteResponse> crear(@Valid @RequestBody PacienteRequest r) {
        validarDocumento(r, null);
        Paciente p = new Paciente();
        aplicar(p, r);
        return ResponseEntity.status(HttpStatus.CREATED).body(PacienteResponse.from(pacienteService.guardar(p)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PacienteResponse actualizar(@PathVariable String id, @Valid @RequestBody PacienteRequest r) {
        Paciente p = existente(id);
        validarDocumento(r, id);
        aplicar(p, r);
        return PacienteResponse.from(pacienteService.guardar(p));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        existente(id);
        pacienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- utilidades ----------

    private Paciente existente(String id) {
        Paciente p = pacienteService.obtenerPorId(id);
        if (p == null) throw new ApiException(HttpStatus.NOT_FOUND, "Paciente no encontrado");
        return p;
    }

    private void validarDocumento(PacienteRequest r, String idActual) {
        if (TIPO_NN.equalsIgnoreCase(r.tipoDocumento().trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "El tipo NN es solo para ingresos de emergencia sin identificar");
        }
        boolean duplicado = pacienteRepository.findByDocumento(r.documento().trim()).stream()
                .anyMatch(o -> idActual == null || !o.getId().equals(idActual));
        if (duplicado) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un paciente con ese documento");
        }
    }

    private void aplicar(Paciente p, PacienteRequest r) {
        p.setTipoDocumento(r.tipoDocumento().trim());
        p.setNombre(r.nombre().trim());
        p.setApellido(r.apellido().trim());
        p.setDocumento(r.documento().trim());
        p.setTelefono(r.telefono());
        p.setFechaNacimiento(r.fechaNacimiento());
        p.setSexo(r.sexo());
        p.setDireccion(r.direccion());
        p.setEps(r.eps());
        p.setNombreContactoEmergencia(r.nombreContactoEmergencia());
        p.setTelefonoContactoEmergencia(r.telefonoContactoEmergencia());
    }
}
