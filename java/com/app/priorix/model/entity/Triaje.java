package com.app.priorix.model.entity;

import com.app.priorix.model.enumeration.NivelTriaje;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "triajes")
public class Triaje {

    @Id
    private String id;

    @DocumentReference(lazy = true)
    private Paciente paciente;

    @DocumentReference(lazy = true)
    private Usuario enfermero;

    @DocumentReference(lazy = true)
    private Atencion atencion;

    @DocumentReference(lazy = true)
    private HistoriaClinica historiaClinica;

    
    @NotBlank(message = "Los síntomas son obligatorios")
    private String sintomas;

    @Field("frecuencia_cardiaca")
    private Integer frecuenciaCardiaca;

    @Field("frecuencia_respiratoria")
    private Integer frecuenciaRespiratoria;

    @Field("presion_arterial")
    private String presionArterial; 

    private Double temperatura;

    @Field("saturacion_oxigeno")
    private Integer saturacionOxigeno;

    @Field("estado_consciencia")
    private String estadoConsciencia; 

    private String alergias;

    @Field("enfermedades_previas")
    private String enfermedadesPrevias;

    @Field("nivel_dolor")
    private Integer nivelDolor;

    private LocalDate fechaRegistro;
    private LocalDateTime horaRegistro;
     private NivelTriaje nivel;
     

    @Field("prioridad")
    private Integer prioridad;

    // ==============================
    //      MÉTODOS DE LÓGICA
    // ==============================

    // Asigna nivel según el dolor reportado
    public void clasificarPorDolor() {
        if (nivelDolor == null) return;

        if (nivelDolor >= 8) nivel = NivelTriaje.NIVEL_1_ROJO;
        else if (nivelDolor >= 6) nivel = NivelTriaje.NIVEL_2_NARANJA;
        else if (nivelDolor >= 4) nivel = NivelTriaje.NIVEL_3_AMARILLO;
        else if (nivelDolor >= 2) nivel = NivelTriaje.NIVEL_4_AZUL;
        else nivel = NivelTriaje.NIVEL_5_VERDE;
    }

    // Convierte nivel de triaje en prioridad numérica para la cola
    public void calcularPrioridad() {
        if (nivel == null) return;

        switch (nivel) {
            case NIVEL_1_ROJO -> prioridad = 1;
            case NIVEL_2_NARANJA -> prioridad = 2;
            case NIVEL_3_AMARILLO -> prioridad = 3;
            case NIVEL_4_AZUL -> prioridad = 4;
            case NIVEL_5_VERDE -> prioridad = 5;
        }
    }

    //@PrePersist
    public void prePersist() {
        if (this.fechaRegistro == null) this.fechaRegistro = LocalDate.now();
        if (this.horaRegistro == null) this.horaRegistro = LocalDateTime.now();

        // Primero aplica las reglas médicas (consciencia, oxígeno, fiebre)
        // y, si ninguna aplica, clasifica por dolor
        evaluarPrioridadMedica();

        // Luego genera prioridad numérica a partir del nivel
        calcularPrioridad();
    }

    public void evaluarPrioridadMedica() {
        // Regla de Oro: Si está inconsciente, es ROJO (Nivel 1)
        if ("Inconsciente".equals(this.estadoConsciencia)) {
            this.nivel = NivelTriaje.NIVEL_1_ROJO;
            return;
        }

        // Regla de Oxígeno: Saturación menor a 90% es emergencia grave
        if (this.saturacionOxigeno != null && this.saturacionOxigeno < 90) {
            this.nivel = NivelTriaje.NIVEL_1_ROJO;
            return;
        }

        // Regla de Fiebre alta
        if (this.temperatura != null && this.temperatura >= 40.0) {
            this.nivel = NivelTriaje.NIVEL_2_NARANJA;
            return;
        }

        // Si nada es crítico, entonces usamos el nivel de dolor como fallback
        clasificarPorDolor();
    }

    // ==============================
    //      GETTERS & SETTERS
    // ==============================

    public String getId() { return id; }

    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }

    public Usuario getEnfermero() { return enfermero; }
    public void setEnfermero(Usuario enfermero) { this.enfermero = enfermero; }

    public Atencion getAtencion() { return atencion; }
    public void setAtencion(Atencion atencion) { this.atencion = atencion; }

    public HistoriaClinica getHistoriaClinica() { return historiaClinica; }
    public void setHistoriaClinica(HistoriaClinica historiaClinica) { this.historiaClinica = historiaClinica; }

    public NivelTriaje getNivel() { return nivel; }
    public void setNivel(NivelTriaje nivel) { this.nivel = nivel; }

    public String getSintomas() { return sintomas; }
    public void setSintomas(String sintomas) { this.sintomas = sintomas; }

    public Integer getNivelDolor() { return nivelDolor; }
    public void setNivelDolor(Integer nivelDolor) { this.nivelDolor = nivelDolor; }

    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public LocalDateTime getHoraRegistro() { return horaRegistro; }
    public void setHoraRegistro(LocalDateTime horaRegistro) { this.horaRegistro = horaRegistro; }

    public Integer getPrioridad() { return prioridad; }
    public void setPrioridad(Integer prioridad) { this.prioridad = prioridad; }

    public Integer getFrecuenciaCardiaca() { return frecuenciaCardiaca; }
    public void setFrecuenciaCardiaca(Integer frecuenciaCardiaca) { this.frecuenciaCardiaca = frecuenciaCardiaca; }

    public Integer getFrecuenciaRespiratoria() { return frecuenciaRespiratoria; }
    public void setFrecuenciaRespiratoria(Integer frecuenciaRespiratoria) { this.frecuenciaRespiratoria = frecuenciaRespiratoria; }

    public String getPresionArterial() { return presionArterial; }
    public void setPresionArterial(String presionArterial) { this.presionArterial = presionArterial; }

    public Double getTemperatura() { return temperatura; }
    public void setTemperatura(Double temperatura) { this.temperatura = temperatura; }

    public Integer getSaturacionOxigeno() { return saturacionOxigeno; }
    public void setSaturacionOxigeno(Integer saturacionOxigeno) { this.saturacionOxigeno = saturacionOxigeno; }

    public String getEstadoConsciencia() { return estadoConsciencia; }
    public void setEstadoConsciencia(String estadoConsciencia) { this.estadoConsciencia = estadoConsciencia; }

    public String getAlergias() { return alergias; }
    public void setAlergias(String alergias) { this.alergias = alergias; }

    public String getEnfermedadesPrevias() { return enfermedadesPrevias; }
    public void setEnfermedadesPrevias(String enfermedadesPrevias) { this.enfermedadesPrevias = enfermedadesPrevias; }

    
}