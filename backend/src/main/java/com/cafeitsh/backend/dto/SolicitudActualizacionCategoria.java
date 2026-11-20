package com.cafeitsh.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para actualizar una categoría (nombre y/o estado).
 */
public record SolicitudActualizacionCategoria(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        String nombre,

        @NotNull(message = "El estado activo es obligatorio")
        Boolean activa
) {
}
