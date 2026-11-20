package com.cafeitsh.backend.dto;

/**
 * Respuesta del inicio de sesión: token JWT y datos del usuario.
 */
public record RespuestaLogin(String token, String tipo, RespuestaUsuario usuario) {

    public static RespuestaLogin conToken(String token, RespuestaUsuario usuario) {
        return new RespuestaLogin(token, "Bearer", usuario);
    }
}
