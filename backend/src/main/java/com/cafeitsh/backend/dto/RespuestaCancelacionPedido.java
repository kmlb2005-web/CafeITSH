package com.cafeitsh.backend.dto;

import java.time.LocalDateTime;

public record RespuestaCancelacionPedido(
        LocalDateTime canceladoEn,
        RespuestaClientePedido canceladoPor,
        String motivoCancelacion,
        boolean requiereReembolso,
        boolean reembolsoManualRegistrado,
        String observacionReembolso
) {
}
