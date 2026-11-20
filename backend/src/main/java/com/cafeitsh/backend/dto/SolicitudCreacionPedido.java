package com.cafeitsh.backend.dto;

import com.cafeitsh.backend.entity.MetodoPagoPreferido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Datos de entrada para crear un pedido anticipado.
 */
public record SolicitudCreacionPedido(
        @NotNull(message = "La fecha y hora de entrega es obligatoria")
        LocalDateTime fechaHoraEntrega,

        MetodoPagoPreferido metodoPagoPreferido,

        @NotNull(message = "Los ?tems del pedido son obligatorios")
        @NotEmpty(message = "El pedido debe incluir al menos un producto")
        List<@Valid SolicitudItemPedido> items
) {
}
