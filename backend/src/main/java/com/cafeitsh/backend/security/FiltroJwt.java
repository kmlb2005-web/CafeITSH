package com.cafeitsh.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Interceptor JWT: si la cabecera Authorization trae un token válido,
 * establece el usuario autenticado en el contexto de seguridad.
 * Tokens inválidos o ausentes se ignoran (la autorización responde 401).
 */
@Component
public class FiltroJwt extends OncePerRequestFilter {

    private final ServicioJwt servicioJwt;
    private final ServicioDetallesUsuario servicioDetallesUsuario;

    public FiltroJwt(ServicioJwt servicioJwt, ServicioDetallesUsuario servicioDetallesUsuario) {
        this.servicioJwt = servicioJwt;
        this.servicioDetallesUsuario = servicioDetallesUsuario;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain cadena) throws ServletException, IOException {

        String cabecera = request.getHeader("Authorization");
        if (cabecera == null || !cabecera.startsWith("Bearer ")) {
            cadena.doFilter(request, response);
            return;
        }

        String token = cabecera.substring(7);
        if (servicioJwt.esValido(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String correo = servicioJwt.obtenerCorreo(token);
            UserDetails detalles = servicioDetallesUsuario.loadUserByUsername(correo);
            if (detalles.isEnabled()) {
                UsernamePasswordAuthenticationToken autenticacion =
                        new UsernamePasswordAuthenticationToken(
                                detalles, null, detalles.getAuthorities());
                autenticacion.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            }
        }

        cadena.doFilter(request, response);
    }
}
