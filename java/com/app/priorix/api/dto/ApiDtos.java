package com.app.priorix.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import com.app.priorix.model.entity.Atencion;
import com.app.priorix.model.entity.HistoriaClinica;
import com.app.priorix.model.entity.Paciente;
import com.app.priorix.model.entity.Triaje;
import com.app.priorix.model.entity.Usuario;
import com.app.priorix.model.enumeration.NivelTriaje;
import com.app.priorix.model.node.NodoPaciente;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

/** Todos los cuerpos de petición/respuesta de la API. Las entidades nunca se exponen directamente. */
public final class ApiDtos {

    private ApiDtos() { }

    static String nombreCompleto(Paciente p) {
        if (p == null) return null;
        String n = p.getNombre() == null ? "" : p.getNombre();
        String a = p.getApellido() == null ? "" : p.getApellido();
        return (n + " " + a).trim();
    }

    // ======================= PACIENTES =======================

    public record PacienteRequest(
            @NotBlank(message = "El tipo de documento es obligatorio") String tipoDocumento,
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            @NotBlank(message = "El apellido es obligatorio") String apellido,
            @NotBlank(message = "El documento es obligatorio") String documento,
            String telefono,
            @Past(message = "La fecha de nacimiento debe ser pasada") LocalDate fechaNacimiento,
            String sexo,
            String direccion,
            String eps,
            String nombreContactoEmergencia,
            String telefonoContactoEmergencia) { }

    public record PacienteResponse(
            String id, String tipoDocumento, String nombre, String apellido, String documento,
            String telefono, LocalDate fechaNacimiento, Integer edad, String sexo, String direccion,
            String eps, String nombreContactoEmergencia, String telefonoContactoEmergencia) {

        public static PacienteResponse from(Paciente p) {
            return new PacienteResponse(p.getId(), p.getTipoDocumento(), p.getNombre(), p.getApellido(),
                    p.getDocumento(), p.getTelefono(), p.getFechaNacimiento(), p.getEdad(), p.getSexo(),
                    p.getDireccion(), p.getEps(), p.getNombreContactoEmergencia(),
                    p.getTelefonoContactoEmergencia());
        }
    }

    // ======================= USUARIOS =======================

    public record UsuarioRequest(
            @NotBlank(message = "El nombre de usuario es obligatorio") String nombreUsuario,
            @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo inválido") String email,
            @NotBlank(message = "El rol es obligatorio (ADMIN, ENFERMERO o MEDICO)") String rol,
            String password) { }

    public record UsuarioResponse(String id, String nombreUsuario, String email, String rol, boolean activo) {
        public static UsuarioResponse from(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getUsername(), u.getEmail(),
                    u.getRol() == null ? null : u.getRol().name(), u.isActivo());
        }
    }

    // ======================= TRIAJE =======================

    public record TriajeRequest(
            @NotBlank(message = "El paciente es obligatorio") String pacienteId,
            @NotBlank(message = "Los síntomas son obligatorios") String sintomas,
            @Min(value = 30, message = "FC mínima 30") @Max(value = 250, message = "FC máxima 250") Integer frecuenciaCardiaca,
            @Min(value = 5, message = "FR mínima 5") @Max(value = 60, message = "FR máxima 60") Integer frecuenciaRespiratoria,
            @Pattern(regexp = "^\\d{2,3}/\\d{2,3}$", message = "Usa el formato 120/80") String presionArterial,
            @DecimalMin(value = "30.0", message = "Temperatura mínima 30") @DecimalMax(value = "45.0", message = "Temperatura máxima 45") Double temperatura,
            @Min(value = 50, message = "SpO2 mínima 50") @Max(value = 100, message = "SpO2 máxima 100") Integer saturacionOxigeno,
            String estadoConsciencia,
            String alergias,
            String enfermedadesPrevias,
            @NotNull(message = "El nivel de dolor es obligatorio") @Min(value = 0, message = "Dolor mínimo 0") @Max(value = 10, message = "Dolor máximo 10") Integer nivelDolor) { }

    public record TriajeResponse(
            String id, String pacienteId, String pacienteNombre, String pacienteDocumento,
            String sintomas, Integer frecuenciaCardiaca, Integer frecuenciaRespiratoria,
            String presionArterial, Double temperatura, Integer saturacionOxigeno,
            String estadoConsciencia, String alergias, String enfermedadesPrevias, Integer nivelDolor,
            Integer nivel, String nivelColor, Integer prioridad,
            LocalDate fechaRegistro, LocalDateTime horaRegistro, String enfermero) {

        public static TriajeResponse from(Triaje t) {
            if (t == null) return null;
            Paciente p = t.getPaciente();
            Usuario e = t.getEnfermero();
            NivelTriaje n = t.getNivel();
            return new TriajeResponse(t.getId(),
                    p == null ? null : p.getId(), nombreCompleto(p), p == null ? null : p.getDocumento(),
                    t.getSintomas(), t.getFrecuenciaCardiaca(), t.getFrecuenciaRespiratoria(),
                    t.getPresionArterial(), t.getTemperatura(), t.getSaturacionOxigeno(),
                    t.getEstadoConsciencia(), t.getAlergias(), t.getEnfermedadesPrevias(), t.getNivelDolor(),
                    n == null ? null : n.getNivel(), n == null ? null : n.getColor(), t.getPrioridad(),
                    t.getFechaRegistro(), t.getHoraRegistro(), e == null ? null : e.getUsername());
        }
    }

    // ======================= EMERGENCIA (N.N. / fallecido) =======================

    public record EmergenciaRequest(
            @NotBlank(message = "El tipo es obligatorio (VIVO o FALLECIDO)") String tipo,
            @NotBlank(message = "La descripción es obligatoria") String descripcion,
            @Min(0) @Max(120) Integer edadAproximada,
            String sexo,
            String estadoConsciencia,
            @Min(1) @Max(5) Integer nivel,
            String medicoConstata,
            Boolean autoridadesNotificadas) { }

    public record EmergenciaResponse(String tipo, String codigo, String pacienteId, String triajeId) { }

    public record IdentificarRequest(
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            @NotBlank(message = "El apellido es obligatorio") String apellido,
            @NotBlank(message = "El tipo de documento es obligatorio") String tipoDocumento,
            @NotBlank(message = "El documento es obligatorio") String documento,
            @Past LocalDate fechaNacimiento,
            String sexo, String telefono, String nombreContacto, String telefonoContacto) { }

    // ======================= LISTA DE ESPERA =======================

    public record EsperaItem(
            int posicion, String idTriaje, String pacienteId, String pacienteNombre, String pacienteDocumento,
            Integer edad, Integer nivel, String nivelColor, Integer prioridad, String motivo,
            LocalDateTime horaRegistro) {

        public static EsperaItem from(int posicion, NodoPaciente n) {
            Paciente p = n.getPaciente();
            Triaje t = n.getTriaje();
            NivelTriaje nv = (t == null) ? null : t.getNivel();
            return new EsperaItem(posicion, n.getIdTriaje(),
                    p == null ? null : p.getId(), nombreCompleto(p), p == null ? null : p.getDocumento(),
                    p == null ? null : p.getEdad(),
                    nv == null ? null : nv.getNivel(), nv == null ? null : nv.getColor(), n.getPrioridad(),
                    t == null ? null : t.getSintomas(), n.getHoraRegistro());
        }
    }

    // ======================= ATENCIÓN =======================

    public record AtencionRequest(
            String motivoConsulta,
            @NotBlank(message = "El diagnóstico es obligatorio") String diagnostico,
            String tratamiento,
            String observaciones) { }

    public record AtencionResponse(
            String id, String estado, LocalDate fecha, LocalTime hora, String medico,
            String pacienteId, String pacienteNombre, TriajeResponse triaje,
            String motivoConsulta, String diagnostico, String tratamiento, String observaciones,
            String historiaClinicaId) {

        public static AtencionResponse from(Atencion a) {
            Paciente p = a.getPaciente();
            Usuario m = a.getMedico();
            HistoriaClinica h = a.getHistoriaClinica();
            return new AtencionResponse(a.getId(), a.getEstado() == null ? null : a.getEstado().name(),
                    a.getFechaAtencion(), a.getHoraAtencion(), m == null ? null : m.getUsername(),
                    p == null ? null : p.getId(), nombreCompleto(p), TriajeResponse.from(a.getTriaje()),
                    a.getMotivoConsulta(), a.getDiagnostico(), a.getTratamiento(), a.getObservaciones(),
                    h == null ? null : h.getId());
        }
    }

    // ======================= HISTORIA CLÍNICA =======================

    public record HistoriaResponse(
            String id, String pacienteId, String atencionId, String sintomasIniciales,
            Integer nivelDolorInicial, String nivelTriaje, String motivoConsulta, String diagnostico,
            String tratamiento, String observaciones, LocalDate fechaAtencion, LocalTime horaAtencion,
            String medico) {

        public static HistoriaResponse from(HistoriaClinica h) {
            Paciente p = h.getPaciente();
            Atencion a = h.getAtencion();
            Usuario u = h.getUsuario();
            return new HistoriaResponse(h.getId(), p == null ? null : p.getId(), a == null ? null : a.getId(),
                    h.getSintomasIniciales(), h.getNivelDolorInicial(), h.getNivelTriaje(),
                    h.getMotivoConsulta(), h.getDiagnostico(), h.getTratamiento(), h.getObservaciones(),
                    h.getFechaAtencion(), h.getHoraAtencion(), u == null ? null : u.getUsername());
        }
    }

    // ======================= REPORTES =======================

    public record ReporteItem(String paciente, Integer nivel, String nivelColor, String estado,
                              String medico, LocalDate fecha) { }

    public record ReporteResponse(int total, Map<String, Long> porNivel, Map<String, Long> porEstado,
                                  List<ReporteItem> items) { }
}
