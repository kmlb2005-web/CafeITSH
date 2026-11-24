package com.cafeitsh.backend.controller;

import com.cafeitsh.backend.dto.RespuestaPedido;
import com.cafeitsh.backend.service.ServicioCocina;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ControladorCocina {

    private final ServicioCocina servicioCocina;

    public ControladorCocina(ServicioCocina servicioCocina) {
        this.servicioCocina = servicioCocina;
    }

    @GetMapping("/cocina/pedidos")
    public List<RespuestaPedido> listarPendientes(@AuthenticationPrincipal UserDetails detalles) {
        return servicioCocina.listarPendientes(detalles.getUsername());
    }

    @PostMapping("/pedidos/{id}/en-preparacion")
    public RespuestaPedido iniciarPreparacion(@AuthenticationPrincipal UserDetails detalles,
                                              @PathVariable Long id) {
        return servicioCocina.iniciarPreparacion(id, detalles.getUsername());
    }

    @PostMapping("/pedidos/{id}/listo")
    public RespuestaPedido marcarListo(@AuthenticationPrincipal UserDetails detalles,
                                       @PathVariable Long id) {
        return servicioCocina.marcarListo(id, detalles.getUsername());
    }

    @PostMapping("/pedidos/{id}/entregado")
    public RespuestaPedido marcarEntregado(@AuthenticationPrincipal UserDetails detalles,
                                           @PathVariable Long id) {
        return servicioCocina.marcarEntregado(id, detalles.getUsername());
    }
}
