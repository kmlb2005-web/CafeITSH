package com.cafeitsh.backend.dto;

import java.math.BigDecimal;

public record RespuestaCanceladosCorte(
        long cantidad,
        BigDecimal montoPagadoCancelado
) {
}
