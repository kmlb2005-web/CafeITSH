package com.cafeitsh.backend.dto;

/**
 * Datos resumidos del cliente asociado a un pedido.
 */
public record RespuestaClientePedido(Long id, String nombre, String correo) {
}
