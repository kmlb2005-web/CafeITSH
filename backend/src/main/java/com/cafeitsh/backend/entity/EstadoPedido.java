package com.cafeitsh.backend.entity;

/**
 * Estados del pedido. Cat?logo fijo definido en docs/AGENTS.md:
 * no modificar ni agregar estados sin autorizaci?n.
 */
public enum EstadoPedido {
    PENDIENTE,
    PAGADO,
    EN_PREPARACION,
    LISTO,
    ENTREGADO,
    CANCELADO
}
