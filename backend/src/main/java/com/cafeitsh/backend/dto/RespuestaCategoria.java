package com.cafeitsh.backend.dto;

/**
 * Respuesta de categoría. En el menú público solo se devuelven las
 * categorías activas.
 */
public record RespuestaCategoria(Long id, String nombre, boolean activa) {
}
