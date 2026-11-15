package com.app.priorix.model.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Registro simple de una persona que llega fallecida. No entra a la lista de
 * espera de triaje: solo deja constancia del ingreso para que la clínica siga
 * su procedimiento (constatación médica, aviso a autoridades, identificación).
 */
@Document(collection = "ingresos_fallecidos")
public class IngresoFallecido {

    @Id
    private String id;

    private String codigo;                 // FALL-yyyyMMdd-001
    private LocalDateTime fechaIngreso;
    private String descripcion;            // circunstancias / descripción física
    private Integer edadAproximada;
    private String sexoAparente;           // M, F o vacío
    private String medicoConstata;         // médico que constató el fallecimiento (si ya se hizo)
    private boolean autoridadesNotificadas;
    private String nombre;                 // se completa cuando se identifica
    private String documento;              // se completa cuando se identifica
    private String registradoPor;          // usuario que hizo el registro
    private String trasladadoPor;          // móvil, institución o acompañante

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public LocalDateTime getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(LocalDateTime fechaIngreso) { this.fechaIngreso = fechaIngreso; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getEdadAproximada() { return edadAproximada; }
    public void setEdadAproximada(Integer edadAproximada) { this.edadAproximada = edadAproximada; }

    public String getSexoAparente() { return sexoAparente; }
    public void setSexoAparente(String sexoAparente) { this.sexoAparente = sexoAparente; }

    public String getMedicoConstata() { return medicoConstata; }
    public void setMedicoConstata(String medicoConstata) { this.medicoConstata = medicoConstata; }

    public boolean isAutoridadesNotificadas() { return autoridadesNotificadas; }
    public void setAutoridadesNotificadas(boolean autoridadesNotificadas) { this.autoridadesNotificadas = autoridadesNotificadas; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }

    public String getTrasladadoPor() { return trasladadoPor; }
    public void setTrasladadoPor(String trasladadoPor) { this.trasladadoPor = trasladadoPor; }
}
