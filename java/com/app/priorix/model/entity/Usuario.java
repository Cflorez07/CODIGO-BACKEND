package com.app.priorix.model.entity;

import com.app.priorix.model.enumeration.RolUsuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.List;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Document(collection = "usuarios")
public class Usuario implements UserDetails{

    @Id
    private String id;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String nombreUsuario;

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    @NotBlank(message = "El email es obligatorio")
    private String email;

    
    @NotNull(message = "El rol es obligatorio")
    private RolUsuario rol;

    @NotBlank (message = "La contraseña es obligatoria")
    private String password;

    private String nombreCompleto;
    private String especialidad;
    private String telefono;
    private String fotoUrl;

    // Relaciones
    @DocumentReference(lazy = true)
    private List<Triaje> triajesRealizados;

    @DocumentReference(lazy = true)
    private List<Atencion> atencionesRealizadas;

   

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public RolUsuario getRol() { return rol; }
    public void setRol(RolUsuario rol) { this.rol = rol; }

    @Override
    public String getPassword() {
    return password;
    }

    public List<Triaje> getTriajesRealizados() { return triajesRealizados; }
    public void setTriajesRealizados(List<Triaje> triajesRealizados) { this.triajesRealizados = triajesRealizados; }

    public List<Atencion> getAtencionesRealizadas() { return atencionesRealizadas; }
    public void setAtencionesRealizadas(List<Atencion> atencionesRealizadas) { this.atencionesRealizadas = atencionesRealizadas; }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.rol == null) {
            return List.of();
        }
        // rol.name() ya trae el prefijo ROLE_ (ROLE_ADMIN, ROLE_MEDICO...)
        return List.of(new SimpleGrantedAuthority(this.rol.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        // ¿Para qué sirve?: Indica si la cuenta del usuario ha expirado.
        // ¿Por qué retornar true?: Para que la cuenta esté siempre vigente a menos que implementes lógica de expiración.
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // ¿Para qué sirve?: Indica si el usuario está bloqueado (ej. por intentos fallidos).
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // ¿Para qué sirve?: Indica si la contraseña ha caducado.
        return true;
    }

    private boolean activo = false;

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    // 3. Modifica este método de Spring Security:
    public void setPassword(String password) {
    this.password = password;
}

    @Override
    public boolean isEnabled() {
        return this.activo; // Ahora Spring Security bloqueará a los inactivos automáticamente
    }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getIniciales() {
        if (nombreCompleto != null && !nombreCompleto.isBlank()) {
            String[] parts = nombreCompleto.trim().split("\\s+");
            if (parts.length >= 2) {
                return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
            } else if (parts.length == 1 && !parts[0].isEmpty()) {
                return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
            }
        }
        if (nombreUsuario != null && !nombreUsuario.isBlank()) {
            return nombreUsuario.substring(0, Math.min(2, nombreUsuario.length())).toUpperCase();
        }
        return "PX";
    }
}