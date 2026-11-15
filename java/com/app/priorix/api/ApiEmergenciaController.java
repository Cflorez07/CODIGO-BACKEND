package com.app.priorix.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

import com.app.priorix.Repository.IngresoFallecidoRepository;
import com.app.priorix.Repository.PacienteRepository;
import com.app.priorix.Service.ListaEsperaService;
import com.app.priorix.Service.PacienteService;
import com.app.priorix.Service.TriajeService;
import com.app.priorix.api.dto.ApiDtos.EmergenciaRequest;
import com.app.priorix.api.dto.ApiDtos.EmergenciaResponse;
import com.app.priorix.api.dto.ApiDtos.IdentificarRequest;
import com.app.priorix.api.dto.ApiDtos.PacienteResponse;
import com.app.priorix.model.entity.IngresoFallecido;
import com.app.priorix.model.entity.Paciente;
import com.app.priorix.model.entity.Triaje;
import com.app.priorix.model.enumeration.NivelTriaje;
import com.app.priorix.model.node.NodoPaciente;

import jakarta.validation.Valid;

/**
 * Ingreso de emergencia (atender primero, identificar después). Misma lógica que EmergenciaController (web):
 *  - VIVO: crea un paciente temporal "N.N." con código, le registra un triaje mínimo y lo mete a la lista de espera.
 *  - FALLECIDO: deja un registro aparte, sin entrar a la lista de espera.
 */
@RestController
@RequestMapping("/api/v1/emergencias")
@PreAuthorize("hasRole('ENFERMERO')")
public class ApiEmergenciaController {

    private static final String TIPO_NN = "NN";
    private static final DateTimeFormatter FECHA_CODIGO = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PacienteRepository pacienteRepository;
    private final PacienteService pacienteService;
    private final TriajeService triajeService;
    private final ListaEsperaService listaEspera;
    private final IngresoFallecidoRepository fallecidoRepository;
    private final UsuarioActual usuarioActual;

    public ApiEmergenciaController(PacienteRepository pacienteRepository, PacienteService pacienteService,
                                   TriajeService triajeService, ListaEsperaService listaEspera,
                                   IngresoFallecidoRepository fallecidoRepository, UsuarioActual usuarioActual) {
        this.pacienteRepository = pacienteRepository;
        this.pacienteService = pacienteService;
        this.triajeService = triajeService;
        this.listaEspera = listaEspera;
        this.fallecidoRepository = fallecidoRepository;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping
    public ResponseEntity<EmergenciaResponse> ingresar(@Valid @RequestBody EmergenciaRequest r, Authentication auth) {
        String tipo = r.tipo().trim().toUpperCase();
        if (!tipo.equals("VIVO") && !tipo.equals("FALLECIDO")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El tipo debe ser VIVO o FALLECIDO");
        }
        String descripcion = r.descripcion().trim();
        String sexo = (r.sexo() == null || r.sexo().isBlank()) ? null : r.sexo();

        if (tipo.equals("FALLECIDO")) {
            IngresoFallecido f = new IngresoFallecido();
            f.setCodigo(siguienteCodigo("FALL-", fallecidoRepository.countByCodigoStartingWith(prefijo("FALL-")) + 1));
            f.setFechaIngreso(LocalDateTime.now());
            f.setDescripcion(descripcion);
            f.setEdadAproximada(r.edadAproximada());
            f.setSexoAparente(sexo);
            f.setMedicoConstata(r.medicoConstata());
            f.setAutoridadesNotificadas(Boolean.TRUE.equals(r.autoridadesNotificadas()));
            f.setRegistradoPor(auth.getName());
            fallecidoRepository.save(f);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new EmergenciaResponse("FALLECIDO", f.getCodigo(), null, null));
        }

        // ---- Paciente vivo sin identificar ----
        String codigo = siguienteCodigo("NN-", pacienteRepository.countByDocumentoStartingWith(prefijo("NN-")) + 1);

        Paciente p = new Paciente();
        p.setTipoDocumento(TIPO_NN);
        p.setNombre("N.N.");
        p.setApellido(codigo);
        p.setDocumento(codigo);
        p.setSexo(sexo);
        if (r.edadAproximada() != null) p.setFechaNacimiento(LocalDate.now().minusYears(r.edadAproximada()));
        p = pacienteService.guardar(p);

        int n = (r.nivel() == null) ? 1 : r.nivel();   // el nivel lo decide el enfermero (1 = rojo por defecto)

        Triaje t = new Triaje();
        t.setPaciente(p);
        t.setEnfermero(usuarioActual.de(auth));
        t.setSintomas(descripcion);
        t.setEstadoConsciencia((r.estadoConsciencia() == null || r.estadoConsciencia().isBlank())
                ? "Inconsciente" : r.estadoConsciencia());
        t.setNivel(NivelTriaje.values()[n - 1]);
        t.setFechaRegistro(LocalDate.now());
        t.setHoraRegistro(LocalDateTime.now());
        t.calcularPrioridad();
        t = triajeService.guardar(t);

        listaEspera.agregar(new NodoPaciente(t));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new EmergenciaResponse("VIVO", codigo, p.getId(), t.getId()));
    }

    /** Pacientes N.N. pendientes de identificar. */
    @GetMapping("/pendientes")
    public List<PacienteResponse> pendientes() {
        return pacienteRepository.findByTipoDocumento(TIPO_NN).stream().map(PacienteResponse::from).toList();
    }

    /** Completa los datos reales de un N.N. cuando se conocen. */
    @PostMapping("/{id}/identificar")
    public PacienteResponse identificar(@PathVariable String id, @Valid @RequestBody IdentificarRequest r) {
        Paciente p = pacienteService.obtenerPorId(id);
        if (p == null || !TIPO_NN.equals(p.getTipoDocumento())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No hay un paciente N.N. con ese id");
        }
        if (TIPO_NN.equalsIgnoreCase(r.tipoDocumento().trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Indica el tipo de documento real");
        }
        String documento = r.documento().trim();
        boolean duplicado = pacienteRepository.findByDocumento(documento).stream()
                .anyMatch(o -> !o.getId().equals(id));
        if (duplicado) throw new ApiException(HttpStatus.CONFLICT, "Ya existe un paciente con ese documento");

        p.setNombre(r.nombre().trim());
        p.setApellido(r.apellido().trim());
        p.setTipoDocumento(r.tipoDocumento().trim());
        p.setDocumento(documento);
        if (r.fechaNacimiento() != null) p.setFechaNacimiento(r.fechaNacimiento());
        if (r.sexo() != null && !r.sexo().isBlank()) p.setSexo(r.sexo());
        p.setTelefono(r.telefono());
        p.setNombreContactoEmergencia(r.nombreContacto());
        p.setTelefonoContactoEmergencia(r.telefonoContacto());
        final Paciente actualizado = pacienteService.guardar(p);

        // La lista de espera vive en memoria: se actualiza el paciente si aún está esperando
        synchronized (listaEspera) {
            NodoPaciente nodo = listaEspera.obtenerLista();
            while (nodo != null) {
                if (nodo.getPaciente() != null && id.equals(nodo.getPaciente().getId())) {
                    nodo.setPaciente(actualizado);
                    if (nodo.getTriaje() != null) nodo.getTriaje().setPaciente(actualizado);
                }
                nodo = nodo.getSiguiente();
            }
        }
        return PacienteResponse.from(actualizado);
    }

    private String prefijo(String base) {
        return base + LocalDate.now().format(FECHA_CODIGO) + "-";
    }

    private String siguienteCodigo(String base, long numero) {
        return prefijo(base) + String.format("%03d", numero);
    }
}
