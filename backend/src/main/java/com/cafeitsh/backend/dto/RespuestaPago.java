package com.cafeitsh.backend.dto;

import com.cafeitsh.backend.entity.MetodoPago;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RespuestaPago(
        Long id,
        MetodoPago metodoPago,
        BigDecimal monto,
        LocalDateTime fechaPago,
        RespuestaClientePedido registradoPor,
        String referencia
) {
}
