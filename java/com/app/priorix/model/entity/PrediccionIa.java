package com.app.priorix.model.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "predicciones_ia")
public class PrediccionIa {

    @Id
    private String id;

    private LocalDateTime fechaHora = LocalDateTime.now();

    private String pacienteId;
    private String pacienteNombre;
    private Integer edad;
    private String sexo;

    private Integer frecuenciaCardiaca;
    private Integer frecuenciaRespiratoria;
    private String presionArterial;
    private Double temperatura;
    private Integer saturacionOxigeno;
    private String estadoConsciencia;
    private Integer nivelDolor;
    private String sintomas;

    private String nivelPredicho;
    private int nivelPredichoNumero;
    private String colorHex;
    private List<String> factores = new ArrayList<>();

    private String nivelAsignado;
    private Integer nivelAsignadoNumero;
    private Boolean coincide;
    private String enfermeroUsername;

    public PrediccionIa() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }

    public String getPacienteNombre() { return pacienteNombre; }
    public void setPacienteNombre(String pacienteNombre) { this.pacienteNombre = pacienteNombre; }

    public Integer getEdad() { return edad; }
    public void setEdad(Integer edad) { this.edad = edad; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

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

    public Integer getNivelDolor() { return nivelDolor; }
    public void setNivelDolor(Integer nivelDolor) { this.nivelDolor = nivelDolor; }

    public String getSintomas() { return sintomas; }
    public void setSintomas(String sintomas) { this.sintomas = sintomas; }

    public String getNivelPredicho() { return nivelPredicho; }
    public void setNivelPredicho(String nivelPredicho) { this.nivelPredicho = nivelPredicho; }

    public int getNivelPredichoNumero() { return nivelPredichoNumero; }
    public void setNivelPredichoNumero(int nivelPredichoNumero) { this.nivelPredichoNumero = nivelPredichoNumero; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public List<String> getFactores() { return factores; }
    public void setFactores(List<String> factores) { this.factores = factores; }

    public String getNivelAsignado() { return nivelAsignado; }
    public void setNivelAsignado(String nivelAsignado) { this.nivelAsignado = nivelAsignado; }

    public Integer getNivelAsignadoNumero() { return nivelAsignadoNumero; }
    public void setNivelAsignadoNumero(Integer nivelAsignadoNumero) { this.nivelAsignadoNumero = nivelAsignadoNumero; }

    public Boolean getCoincide() { return coincide; }
    public void setCoincide(Boolean coincide) { this.coincide = coincide; }

    public String getEnfermeroUsername() { return enfermeroUsername; }
    public void setEnfermeroUsername(String enfermeroUsername) { this.enfermeroUsername = enfermeroUsername; }
}
