package com.cafeitsh.backend.dto;

/**
 * Resumen de categoría (solo identificador y nombre). Es la forma que usa
 * el menú público y el anidado dentro de un producto: no expone el estado
 * lógico `activa` (spec menu §5/§8).
 */
public record RespuestaCategoriaResumen(Long id, String nombre) {
}
