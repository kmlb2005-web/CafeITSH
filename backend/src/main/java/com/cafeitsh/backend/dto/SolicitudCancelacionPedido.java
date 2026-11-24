package com.cafeitsh.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudCancelacionPedido(
        @NotBlank(message = "El motivo de cancelación es obligatorio")
        @Size(max = 255, message = "El motivo de cancelación no debe exceder 255 caracteres")
        String motivoCancelacion,

        Boolean requiereReembolso,

        Boolean reembolsoManualRegistrado,

        @Size(max = 255, message = "La observación de reembolso no debe exceder 255 caracteres")
        String observacionReembolso
) {
}
