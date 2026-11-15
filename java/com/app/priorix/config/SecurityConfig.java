package com.app.priorix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import com.app.priorix.Service.CustomOAuth2UserService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    // Order(2): la cadena de la API (/api/**, ApiSecurityConfig) va primero; esta atiende la web con sesión
    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/landing", "/login", "/css/**", "/js/**", "/img/**", "/error", "/activar", "/activar/**", "/recuperar", "/recuperar/**", "/oauth2/**", "/login/oauth2/**", "/perfil/foto/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/enfermero/**").hasRole("ENFERMERO")
                .requestMatchers("/medico/**").hasRole("MEDICO")
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/redirectByRole", true)
                .failureUrl("/login?error")
                .permitAll()
            )

            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .defaultSuccessUrl("/redirectByRole", true)
                .failureUrl("/login?oauth2error")
            )

            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .sessionManagement(session -> session
                .invalidSessionUrl("/login?invalid-session")
                .maximumSessions(1)
                .expiredUrl("/login?session-expired")
            );

        return http.build();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
