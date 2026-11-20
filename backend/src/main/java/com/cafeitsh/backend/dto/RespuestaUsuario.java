package com.cafeitsh.backend.dto;

import com.cafeitsh.backend.entity.Rol;

/**
 * Respuesta pública de un usuario. Nunca incluye la contraseña.
 */
public record RespuestaUsuario(Long id, String nombre, String correo, Rol rol, boolean activo) {
}
