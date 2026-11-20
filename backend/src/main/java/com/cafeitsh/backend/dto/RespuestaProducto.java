package com.cafeitsh.backend.dto;

import java.math.BigDecimal;

/**
 * Respuesta de producto, con su categoría resumida (id y nombre).
 * Es el mismo formato para el menú público y para administración.
 */
public record RespuestaProducto(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        boolean disponible,
        RespuestaCategoriaResumen categoria
) {
}
