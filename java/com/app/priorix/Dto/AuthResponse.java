package com.app.priorix.Dto;

public class AuthResponse {

    private String token;
    private String tipo = "Bearer";
    private String nombreUsuario;
    private String email;
    private String rol;          // ROLE_ADMIN, ROLE_ENFERMERO o ROLE_MEDICO

    public AuthResponse(String token) { this.token = token; }

    public AuthResponse(String token, String nombreUsuario, String email, String rol) {
        this.token = token;
        this.nombreUsuario = nombreUsuario;
        this.email = email;
        this.rol = rol;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getTipo() { return tipo; }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getEmail() { return email; }
    public String getRol() { return rol; }
}
