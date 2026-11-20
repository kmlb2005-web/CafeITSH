package com.cafeitsh.backend.config;

import com.cafeitsh.backend.security.FiltroJwt;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuración de seguridad: API sin sesiones (JWT), autorización por rol
 * verificada en el backend. La autorización es la única fuente de verdad;
 * el frontend solo oculta botones.
 */
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    private final FiltroJwt filtroJwt;

    public ConfiguracionSeguridad(FiltroJwt filtroJwt) {
        this.filtroJwt = filtroJwt;
    }

    @Bean
    public SecurityFilterChain cadenaFiltros(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(fuenteCors()))
            .sessionManagement(ses -> ses.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Públicos
                .requestMatchers("/api/auth/login", "/api/auth/registro").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/prueba").permitAll()
                // Menú público: solo consultas GET de categorías y productos
                .requestMatchers(HttpMethod.GET, "/api/menu/**").permitAll()
                // Pedidos: creación solo CLIENTE, consulta para usuarios autenticados
                .requestMatchers(HttpMethod.POST, "/api/pedidos").hasRole("CLIENTE")
                .requestMatchers(HttpMethod.GET, "/api/pedidos", "/api/pedidos/**").authenticated()
                // Gestión de usuarios: solo ADMIN
                .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                // Gestión de menú: solo ADMIN
                .requestMatchers("/api/categorias/**", "/api/productos/**").hasRole("ADMIN")
                // Todo lo demás exige autenticación
                .anyRequest().authenticated())
            .exceptionHandling(err -> err
                // 401 sin token o token inválido; 403 con token pero rol insuficiente
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"mensaje\":\"Se requiere autenticación válida\"}");
                })
                .accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(403);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"mensaje\":\"No tienes permiso para realizar esta operación\"}");
                }))
            .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Orígenes permitidos: solo entorno de desarrollo (Flutter web en el
     * navegador del mismo equipo). Restringir antes de producción.
     */
    @Bean
    public CorsConfigurationSource fuenteCors() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:8080",
                "http://localhost:8081"));
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }

    @Bean
    public PasswordEncoder codificadorContrasenas() {
        return new BCryptPasswordEncoder();
    }
}
