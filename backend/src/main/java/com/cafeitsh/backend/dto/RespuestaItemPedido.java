package com.cafeitsh.backend.dto;

import java.math.BigDecimal;

/**
 * ?tem de pedido con precio vigente capturado al momento de la creaci?n.
 */
public record RespuestaItemPedido(
        Long productoId,
        String nombreProducto,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
}
