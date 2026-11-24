package com.cafeitsh.backend.controller;

import com.cafeitsh.backend.dto.RespuestaPedido;
import com.cafeitsh.backend.dto.SolicitudCreacionPedido;
import com.cafeitsh.backend.service.ServicioPedido;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de pedidos anticipados. La creaci?n es exclusiva de CLIENTE;
 * la consulta se filtra por rol y propiedad en el servicio.
 */
@RestController
@RequestMapping("/api/pedidos")
public class ControladorPedido {

    private final ServicioPedido servicioPedido;

    public ControladorPedido(ServicioPedido servicioPedido) {
        this.servicioPedido = servicioPedido;
    }

    @PostMapping
    public ResponseEntity<RespuestaPedido> crear(
            @AuthenticationPrincipal UserDetails detalles,
            @Valid @RequestBody SolicitudCreacionPedido solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicioPedido.crear(solicitud, detalles.getUsername()));
    }

    @GetMapping
    public List<RespuestaPedido> listar(@AuthenticationPrincipal UserDetails detalles) {
        return servicioPedido.listar(detalles.getUsername());
    }

    @GetMapping("/{id}")
    public RespuestaPedido obtener(@AuthenticationPrincipal UserDetails detalles,
                                   @PathVariable Long id) {
        return servicioPedido.obtenerPorId(id, detalles.getUsername());
    }
}
