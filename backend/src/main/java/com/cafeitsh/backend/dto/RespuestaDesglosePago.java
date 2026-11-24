package com.cafeitsh.backend.dto;

import com.cafeitsh.backend.entity.MetodoPago;
import java.math.BigDecimal;

public record RespuestaDesglosePago(
        MetodoPago metodoPago,
        long cantidad,
        BigDecimal monto
) {
}
