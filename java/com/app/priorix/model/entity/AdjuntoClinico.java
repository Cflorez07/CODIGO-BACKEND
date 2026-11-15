package com.app.priorix.model.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "adjuntos_clinicos")
public class AdjuntoClinico {

    @Id
    private String id;

    private String nombreOriginal;
    private String tipoContenido; // e.g. image/jpeg, application/pdf
    private long tamano; // bytes
    private LocalDateTime fechaSubida = LocalDateTime.now();
    private String subidoPor; // username

    private String pacienteId;
    private String triajeId;
    private String atencionId;
    private String historiaClinicaId;

    private String descripcion;
    private String rutaArchivo; // Ruta local relativa o absoluta

    public AdjuntoClinico() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombreOriginal() { return nombreOriginal; }
    public void setNombreOriginal(String nombreOriginal) { this.nombreOriginal = nombreOriginal; }

    public String getTipoContenido() { return tipoContenido; }
    public void setTipoContenido(String tipoContenido) { this.tipoContenido = tipoContenido; }

    public long getTamano() { return tamano; }
    public void setTamano(long tamano) { this.tamano = tamano; }

    public LocalDateTime getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDateTime fechaSubida) { this.fechaSubida = fechaSubida; }

    public String getSubidoPor() { return subidoPor; }
    public void setSubidoPor(String subidoPor) { this.subidoPor = subidoPor; }

    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }

    public String getTriajeId() { return triajeId; }
    public void setTriajeId(String triajeId) { this.triajeId = triajeId; }

    public String getAtencionId() { return atencionId; }
    public void setAtencionId(String atencionId) { this.atencionId = atencionId; }

    public String getHistoriaClinicaId() { return historiaClinicaId; }
    public void setHistoriaClinicaId(String historiaClinicaId) { this.historiaClinicaId = historiaClinicaId; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getRutaArchivo() { return rutaArchivo; }
    public void setRutaArchivo(String rutaArchivo) { this.rutaArchivo = rutaArchivo; }
}
