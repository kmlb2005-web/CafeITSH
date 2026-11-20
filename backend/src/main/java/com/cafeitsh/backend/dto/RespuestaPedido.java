package com.cafeitsh.backend.dto;

import com.cafeitsh.backend.entity.EstadoPedido;
import com.cafeitsh.backend.entity.MetodoPagoPreferido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de un pedido con su detalle de ?tems.
 */
public record RespuestaPedido(
        Long id,
        EstadoPedido estado,
        LocalDateTime fechaHoraEntrega,
        MetodoPagoPreferido metodoPagoPreferido,
        BigDecimal total,
        RespuestaClientePedido cliente,
        List<RespuestaItemPedido> items,
        LocalDateTime creadoEn
) {
}
