package com.cafeitsh.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RespuestaCorteCaja(
        LocalDate fecha,
        String zonaHoraria,
        BigDecimal totalCobrado,
        long cantidadPagos,
        List<RespuestaDesglosePago> desglosePorMetodo,
        RespuestaCanceladosCorte cancelados
) {
}
