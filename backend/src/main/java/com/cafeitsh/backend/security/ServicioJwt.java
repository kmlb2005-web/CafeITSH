package com.cafeitsh.backend.security;

import com.cafeitsh.backend.entity.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Generación y validación de tokens JWT.
 *
 * El secreto de firma llega desde la variable de entorno {@code JWT_SECRET}
 * (nunca se escribe en el repositorio) y debe tener al menos 32 caracteres.
 */
@Service
public class ServicioJwt {

    /** Largo mínimo recomendado por JJWT para HMAC-SHA. */
    private static final int LONGITUD_MINIMA_SECRETO = 32;

    private final SecretKey clave;
    private final long expiracionMinutos;

    public ServicioJwt(
            @Value("${jwt.secreto}") String secreto,
            @Value("${jwt.expiracion-minutos}") long expiracionMinutos) {
        if (secreto == null || secreto.length() < LONGITUD_MINIMA_SECRETO) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos " + LONGITUD_MINIMA_SECRETO + " caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMinutos = expiracionMinutos;
    }

    public String generarToken(String correo, Rol rol) {
        Date ahora = new Date();
        Date vencimiento = new Date(ahora.getTime() + expiracionMinutos * 60_000);
        return Jwts.builder()
                .subject(correo)
                .claim("rol", rol.name())
                .issuedAt(ahora)
                .expiration(vencimiento)
                .signWith(clave)
                .compact();
    }

    /**
     * Extrae el correo del token. Lanza {@link JwtException} si la firma es
     * inválida, está expirado o no es un JWT de este servidor.
     */
    public String obtenerCorreo(String token) {
        Claims reclamos = Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return reclamos.getSubject();
    }

    public boolean esValido(String token) {
        try {
            obtenerCorreo(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
