package com.app.priorix.model.entity;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "pacientes")
public class Paciente {

    @Id
    private String id;

    @Field("tipo_documento")
    private String tipoDocumento;

    private String nombre;
    private String apellido;
    private String documento;
    private String telefono;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaNacimiento;

    private String sexo;
    private String direccion;
    private String eps;

    // --- CONTACTO DE EMERGENCIA ---
    @Field("nombre_contacto_emergencia")
    private String nombreContactoEmergencia;
    
    @Field("telefono_contacto_emergencia")
    private String telefonoContactoEmergencia;
   
    @DocumentReference(lazy = true)
private List<Triaje> triajes;

@DocumentReference(lazy = true)
private List<Atencion> atenciones;

@DocumentReference(lazy = true)
private HistoriaClinica historiaClinica;

    @Transient
    public Integer getEdad() {
        if (fechaNacimiento == null) return null;
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }


    // Getters y Setters 

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getEps() { return eps; }
    public void setEps(String eps) { this.eps = eps; }

    public String getNombreContactoEmergencia() { return nombreContactoEmergencia; }
    public void setNombreContactoEmergencia(String nombreContactoEmergencia) { this.nombreContactoEmergencia = nombreContactoEmergencia; }

    public String getTelefonoContactoEmergencia() { return telefonoContactoEmergencia; }
    public void setTelefonoContactoEmergencia(String telefonoContactoEmergencia) { this.telefonoContactoEmergencia = telefonoContactoEmergencia; }

    public List<Triaje> getTriajes() { return triajes; }
    public void setTriajes(List<Triaje> triajes) { this.triajes = triajes; }

    public List<Atencion> getAtenciones() { return atenciones; }
    public void setAtenciones(List<Atencion> atenciones) { this.atenciones = atenciones; }

   public HistoriaClinica getHistoriaClinica() {
    return historiaClinica;
    
}

public void setHistoriaClinica(HistoriaClinica historiaClinica) {
    this.historiaClinica = historiaClinica;
}
}