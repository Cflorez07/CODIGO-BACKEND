package com.app.priorix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.app.priorix.Service.JwtService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Cadena de seguridad SOLO para la API móvil (/api/**): sin sesión, sin CSRF, con JWT.
 * La web (Thymeleaf) sigue usando SecurityConfig con sesión y formulario (Order 2).
 * Los permisos por rol se declaran en cada endpoint con @PreAuthorize.
 */
@Configuration
@EnableMethodSecurity
public class ApiSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http,
                                              JwtService jwtService,
                                              UserDetailsService userDetailsService) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) ->
                        escribirError(res, HttpServletResponse.SC_UNAUTHORIZED,
                                "No autenticado o sesión vencida. Inicia sesión de nuevo."))
                .accessDeniedHandler((req, res, ex) ->
                        escribirError(res, HttpServletResponse.SC_FORBIDDEN,
                                "No tienes permiso para esta acción."))
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void escribirError(HttpServletResponse res, int status, String mensaje) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"status\":" + status + ",\"error\":\"" + mensaje + "\"}");
    }
}
