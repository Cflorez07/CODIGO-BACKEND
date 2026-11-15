package com.app.priorix.Controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.priorix.Repository.IngresoFallecidoRepository;
import com.app.priorix.Repository.PacienteRepository;
import com.app.priorix.Service.ListaEsperaService;
import com.app.priorix.Service.PacienteService;
import com.app.priorix.Service.TriajeService;
import com.app.priorix.Service.UsuarioService;
import com.app.priorix.model.entity.IngresoFallecido;
import com.app.priorix.model.entity.Paciente;
import com.app.priorix.model.entity.Triaje;
import com.app.priorix.model.entity.Usuario;
import com.app.priorix.model.enumeration.NivelTriaje;
import com.app.priorix.model.node.NodoPaciente;

/**
 * Ingreso de emergencia: atender primero, identificar después.
 *  - VIVO: crea un paciente temporal "N.N." con código automático, le registra un
 *    triaje mínimo y lo mete a la lista de espera.
 *  - FALLECIDO: deja un registro aparte, SIN entrar a la lista de espera.
 */
@Controller
@RequestMapping("/enfermero/emergencia")
public class EmergenciaController {

    private static final String TIPO_NN = "NN";
    private static final DateTimeFormatter FECHA_CODIGO = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired private PacienteRepository pacienteRepository;
    @Autowired private PacienteService pacienteService;
    @Autowired private TriajeService triajeService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private ListaEsperaService listaEspera;
    @Autowired private IngresoFallecidoRepository fallecidoRepository;

    @GetMapping
    public String formulario() {
        return "enfermero/emergencia";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam String tipo,
                          @RequestParam String descripcion,
                          @RequestParam(required = false) Integer edadAproximada,
                          @RequestParam(required = false) String sexo,
                          @RequestParam(required = false, defaultValue = "Inconsciente") String estadoConsciencia,
                          @RequestParam(required = false, defaultValue = "1") Integer nivel,
                          @RequestParam(required = false) String medicoConstata,
                          @RequestParam(required = false, defaultValue = "false") boolean autoridadesNotificadas,
                          @RequestParam(required = false) String trasladadoPor,
                          Authentication auth) {

        if (descripcion == null || descripcion.isBlank()) {
            return "redirect:/enfermero/emergencia?error=descripcion";
        }
        Integer edad = (edadAproximada != null && edadAproximada >= 0 && edadAproximada <= 120)
                ? edadAproximada : null;

        if ("FALLECIDO".equals(tipo)) {
            IngresoFallecido f = new IngresoFallecido();
            f.setCodigo(siguienteCodigo("FALL-", fallecidoRepository.countByCodigoStartingWith(prefijo("FALL-")) + 1));
            f.setFechaIngreso(LocalDateTime.now());
            f.setDescripcion(descripcion.trim());
            f.setEdadAproximada(edad);
            f.setSexoAparente(sexo);
            f.setMedicoConstata(medicoConstata);
            f.setAutoridadesNotificadas(autoridadesNotificadas);
            f.setRegistradoPor(auth.getName());
            f.setTrasladadoPor(trasladadoPor);
            fallecidoRepository.save(f);
            return "redirect:/enfermero/emergencia?success=fallecido&codigo=" + f.getCodigo();
        }

        // ---- Paciente vivo sin identificar ----
        String codigo = siguienteCodigo("NN-", pacienteRepository.countByDocumentoStartingWith(prefijo("NN-")) + 1);

        Paciente p = new Paciente();
        p.setTipoDocumento(TIPO_NN);
        p.setNombre("N.N.");
        p.setApellido(codigo);
        p.setDocumento(codigo);
        p.setSexo(sexo);
        if (trasladadoPor != null && !trasladadoPor.isBlank()) {
            p.setNombreContactoEmergencia("Trasladado por: " + trasladadoPor.trim());
        }
        // Fecha de nacimiento APROXIMADA a partir de la edad estimada; se corrige al identificar.
        if (edad != null) p.setFechaNacimiento(LocalDate.now().minusYears(edad));
        p = pacienteService.guardar(p);

        int n = Math.max(1, Math.min(5, nivel == null ? 1 : nivel));
        Usuario enf = usuarioService.buscarPorNombreUsuario(auth.getName());

        Triaje t = new Triaje();
        t.setPaciente(p);
        t.setEnfermero(enf);
        t.setSintomas(descripcion.trim());
        t.setEstadoConsciencia(estadoConsciencia);
        t.setNivel(NivelTriaje.values()[n - 1]);   // el nivel lo decide la enfermera
        t.setFechaRegistro(LocalDate.now());
        t.setHoraRegistro(LocalDateTime.now());
        t.calcularPrioridad();
        t = triajeService.guardar(t);

        listaEspera.agregar(new NodoPaciente(t));
        return "redirect:/enfermero/emergencia?success=nn&codigo=" + codigo;
    }

    // ---------- Identificar después ----------

    @GetMapping("/pendientes")
    public String pendientes(Model model) {
        model.addAttribute("pacientes", pacienteRepository.findByTipoDocumento(TIPO_NN));
        return "enfermero/emergencia-pendientes";
    }

    @GetMapping("/identificar/{id}")
    public String formIdentificar(@PathVariable String id, Model model) {
        Paciente p = pacienteService.obtenerPorId(id);
        if (p == null || !TIPO_NN.equals(p.getTipoDocumento())) {
            return "redirect:/enfermero/emergencia/pendientes?error=notfound";
        }
        model.addAttribute("paciente", p);
        return "enfermero/emergencia-identificar";
    }

    @PostMapping("/identificar/{id}")
    public String identificar(@PathVariable String id,
                              @RequestParam String nombre,
                              @RequestParam String apellido,
                              @RequestParam String tipoDocumento,
                              @RequestParam String documento,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaNacimiento,
                              @RequestParam(required = false) String sexo,
                              @RequestParam(required = false) String telefono,
                              @RequestParam(required = false) String nombreContacto,
                              @RequestParam(required = false) String telefonoContacto) {

        Paciente p = pacienteService.obtenerPorId(id);
        if (p == null || !TIPO_NN.equals(p.getTipoDocumento())) {
            return "redirect:/enfermero/emergencia/pendientes?error=notfound";
        }
        if (nombre.isBlank() || apellido.isBlank() || documento.isBlank()
                || TIPO_NN.equalsIgnoreCase(tipoDocumento)) {
            return "redirect:/enfermero/emergencia/identificar/" + id + "?error=datos";
        }
        // Si ya existe otro paciente con ese documento, no se duplica
        boolean duplicado = pacienteRepository.findByDocumento(documento.trim()).stream()
                .anyMatch(o -> !o.getId().equals(id));
        if (duplicado) {
            return "redirect:/enfermero/emergencia/identificar/" + id + "?error=duplicado";
        }

        p.setNombre(nombre.trim());
        p.setApellido(apellido.trim());
        p.setTipoDocumento(tipoDocumento);
        p.setDocumento(documento.trim());
        if (fechaNacimiento != null) p.setFechaNacimiento(fechaNacimiento);
        if (sexo != null && !sexo.isBlank()) p.setSexo(sexo);
        p.setTelefono(telefono);
        p.setNombreContactoEmergencia(nombreContacto);
        p.setTelefonoContactoEmergencia(telefonoContacto);
        final Paciente actualizado = pacienteService.guardar(p);

        // La lista de espera vive en memoria: se actualiza el paciente si aún está esperando
        NodoPaciente nodo = listaEspera.obtenerLista();
        while (nodo != null) {
            if (nodo.getPaciente() != null && id.equals(nodo.getPaciente().getId())) {
                nodo.setPaciente(actualizado);
                if (nodo.getTriaje() != null) nodo.getTriaje().setPaciente(actualizado);
            }
            nodo = nodo.getSiguiente();
        }
        return "redirect:/enfermero/emergencia/pendientes?success=identificado";
    }

    // ---------- Utilidades ----------

    private String prefijo(String base) {
        return base + LocalDate.now().format(FECHA_CODIGO) + "-";
    }

    private String siguienteCodigo(String base, long numero) {
        return prefijo(base) + String.format("%03d", numero);
    }
}
