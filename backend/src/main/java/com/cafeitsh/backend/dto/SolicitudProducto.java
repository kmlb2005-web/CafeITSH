package com.cafeitsh.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Datos de entrada para crear un producto. La categoría debe existir y
 * estar activa (se valida en el servicio → 400 con detalle de campos).
 * {@code disponible} es opcional: si no se envía, el producto se crea
 * disponible.
 */
public record SolicitudProducto(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El precio admite máximo 2 decimales")
        BigDecimal precio,

        Boolean disponible,

        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId
) {
}
