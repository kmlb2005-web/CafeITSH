package com.cafeitsh.backend.controller;

import com.cafeitsh.backend.dto.RespuestaCorteCaja;
import com.cafeitsh.backend.dto.RespuestaPedido;
import com.cafeitsh.backend.dto.SolicitudCancelacionPedido;
import com.cafeitsh.backend.dto.SolicitudRegistroPago;
import com.cafeitsh.backend.service.ServicioCaja;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de caja: pago, cancelación y corte básico.
 */
@RestController
@RequestMapping("/api")
public class ControladorCaja {

    private final ServicioCaja servicioCaja;

    public ControladorCaja(ServicioCaja servicioCaja) {
        this.servicioCaja = servicioCaja;
    }

    @PostMapping("/pedidos/{id}/pago")
    public RespuestaPedido registrarPago(@AuthenticationPrincipal UserDetails detalles,
                                         @PathVariable Long id,
                                         @Valid @RequestBody SolicitudRegistroPago solicitud) {
        return servicioCaja.registrarPago(id, solicitud, detalles.getUsername());
    }

    @PostMapping("/pedidos/{id}/cancelar")
    public RespuestaPedido cancelar(@AuthenticationPrincipal UserDetails detalles,
                                    @PathVariable Long id,
                                    @Valid @RequestBody SolicitudCancelacionPedido solicitud) {
        return servicioCaja.cancelar(id, solicitud, detalles.getUsername());
    }

    @GetMapping("/caja/corte")
    public RespuestaCorteCaja corte(@RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return servicioCaja.obtenerCorte(fecha);
    }
}
