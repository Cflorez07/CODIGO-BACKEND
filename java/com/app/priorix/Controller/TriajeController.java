package com.app.priorix.Controller;

import com.app.priorix.model.entity.Paciente;
import com.app.priorix.model.entity.Triaje;
import com.app.priorix.model.entity.Usuario;
import com.app.priorix.model.node.NodoPaciente;
import com.app.priorix.Repository.PacienteRepository;
import com.app.priorix.Service.TriajeService;
import com.app.priorix.Service.PacienteService;
import com.app.priorix.Service.UsuarioService;
import com.app.priorix.Service.ListaEsperaService;
import com.app.priorix.Service.PredictorTriajeService;
import com.app.priorix.model.entity.PrediccionIa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/enfermero")
public class TriajeController {

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private TriajeService triajeService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ListaEsperaService listaEspera;

    @Autowired
    private PredictorTriajeService predictorTriajeService;

    @GetMapping("/triaje")
    public String mostrarTriaje(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("pacientes", pacienteService.listar(q));
        model.addAttribute("triaje", new Triaje());
        return "enfermero/triaje";
    }

    @PostMapping("/pacientes/registrar")
    @ResponseBody
    public ResponseEntity<?> registrarPacienteDesdeTriaje(@ModelAttribute Paciente paciente,
                                                          @RequestParam(value = "fechaNacimiento", required = false) String fechaNacStr) {
        try {
            if (paciente.getFechaNacimiento() == null && fechaNacStr != null && !fechaNacStr.isBlank()) {
                try {
                    paciente.setFechaNacimiento(java.time.LocalDate.parse(fechaNacStr.trim()));
                } catch (Exception ignored) {}
            }

            // Validaciones de campos obligatorios
            if (paciente.getNombre() == null || paciente.getNombre().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre del paciente es obligatorio."));
            }
            if (paciente.getApellido() == null || paciente.getApellido().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El apellido del paciente es obligatorio."));
            }
            if (paciente.getDocumento() == null || paciente.getDocumento().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El número de documento es obligatorio."));
            }
            if (paciente.getTipoDocumento() == null || paciente.getTipoDocumento().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El tipo de documento es obligatorio."));
            }
            if (paciente.getFechaNacimiento() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "La fecha de nacimiento es obligatoria (formato YYYY-MM-DD)."));
            }
            if (paciente.getSexo() == null || paciente.getSexo().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El sexo biológico es obligatorio."));
            }

            // Validar que no exista un paciente con el mismo número de documento
            String docTrimmed = paciente.getDocumento().trim();
            paciente.setDocumento(docTrimmed);
            paciente.setNombre(paciente.getNombre().trim());
            paciente.setApellido(paciente.getApellido().trim());

            java.util.List<Paciente> existentes = pacienteRepository.findByDocumento(docTrimmed);
            if (existentes != null && !existentes.isEmpty()) {
                Paciente dup = existentes.get(0);
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Ya existe un paciente registrado con el documento " + docTrimmed +
                             " (" + dup.getNombre() + " " + dup.getApellido() + ")."
                ));
            }

            // ID en null para autogeneración limpia en MongoDB
            paciente.setId(null);
            Paciente guardado = pacienteService.guardar(paciente);

            String textoSelect = guardado.getNombre() + " " + guardado.getApellido() + " - Cédula: " + guardado.getDocumento();

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("paciente", Map.of(
                "id", guardado.getId(),
                "nombre", guardado.getNombre(),
                "apellido", guardado.getApellido(),
                "documento", guardado.getDocumento(),
                "tipoDocumento", guardado.getTipoDocumento() != null ? guardado.getTipoDocumento() : "CC",
                "textoSelect", textoSelect
            ));
            resp.put("mensaje", "Paciente " + guardado.getNombre() + " " + guardado.getApellido() + " registrado exitosamente.");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error al registrar el paciente: " + e.getMessage()));
        }
    }

    @PostMapping("/api/triaje/predecir")
    @ResponseBody
    public ResponseEntity<?> predecirIa(@RequestBody Triaje triaje, Authentication auth) {
        try {
            Paciente p = null;
            if (triaje.getPaciente() != null && triaje.getPaciente().getId() != null && !triaje.getPaciente().getId().isBlank()) {
                p = pacienteService.obtenerPorId(triaje.getPaciente().getId());
            }
            String username = auth != null ? auth.getName() : "enfermero";
            PrediccionIa pred = predictorTriajeService.predecirYRegistrar(triaje, p, username);

            Map<String, Object> resp = new HashMap<>();
            resp.put("prediccionId", pred.getId());
            resp.put("nivel", pred.getNivelPredicho());
            resp.put("numero", pred.getNivelPredichoNumero());
            resp.put("color", pred.getColorHex());
            resp.put("factores", pred.getFactores());
            resp.put("advertencia", "Esta sugerencia es un apoyo predictivo. La decisión final corresponde siempre al criterio del profesional de enfermería.");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "No se pudo calcular la predicción: " + e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/triaje/guardar")
    public String guardar(@ModelAttribute Triaje triaje,
                          @RequestParam(value = "prediccionIaId", required = false) String prediccionIaId,
                          Authentication auth) {

    // 1. Validar y recuperar el paciente completo de la BD
    if (triaje.getPaciente() == null
            || triaje.getPaciente().getId() == null
            || triaje.getPaciente().getId().isBlank()) {
        return "redirect:/enfermero/triaje?error=paciente";
    }
    Paciente pacienteCompleto = pacienteService.obtenerPorId(triaje.getPaciente().getId());
    if (pacienteCompleto == null) {
        return "redirect:/enfermero/triaje?error=paciente";
    }
    triaje.setPaciente(pacienteCompleto);

    // 2. Asignar fechas, aplicar reglas médicas, nivel y prioridad
    triaje.prePersist();

    // 3. Asignar enfermero
    Usuario enf = usuarioService.buscarPorNombreUsuario(auth.getName());
    triaje.setEnfermero(enf);

    // 4. Guardar triaje
    triajeService.guardar(triaje);

    // 5. Vincular y confirmar la predicción de IA para supervisión clínica
    String nivelConfirmado = triaje.getNivel() != null ? triaje.getNivel().getColor() : "";
    if (prediccionIaId != null && !prediccionIaId.isBlank()) {
        predictorTriajeService.registrarConfirmacion(prediccionIaId, nivelConfirmado, auth.getName());
    } else {
        PrediccionIa pAuto = predictorTriajeService.predecirYRegistrar(triaje, pacienteCompleto, auth.getName());
        predictorTriajeService.registrarConfirmacion(pAuto.getId(), nivelConfirmado, auth.getName());
    }

    // 6. Crear nodo y enviarlo a lista de espera
    NodoPaciente nodo = new NodoPaciente(triaje);
    listaEspera.agregar(nodo);

    return "redirect:/enfermero/triaje?success=ok";
}



    @GetMapping("/triajes-registrados")
    public String triajes(Authentication auth, Model model) {
        Usuario enf = (auth != null) ? usuarioService.buscarPorNombreUsuario(auth.getName()) : null;
        model.addAttribute("triajes", enf != null ? triajeService.listarPorEnfermero(enf.getId()) : java.util.Collections.emptyList());
        return "enfermero/triajes-registrados";
    }

}