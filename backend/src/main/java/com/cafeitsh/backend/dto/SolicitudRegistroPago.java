package com.cafeitsh.backend.dto;

import com.cafeitsh.backend.entity.MetodoPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudRegistroPago(
        @NotNull(message = "El método de pago es obligatorio")
        MetodoPago metodoPago,

        @Size(max = 100, message = "La referencia no debe exceder 100 caracteres")
        String referencia
) {
}
