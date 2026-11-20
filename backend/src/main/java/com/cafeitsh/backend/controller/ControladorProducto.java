package com.cafeitsh.backend.controller;

import com.cafeitsh.backend.dto.RespuestaProducto;
import com.cafeitsh.backend.dto.SolicitudActualizacionProducto;
import com.cafeitsh.backend.dto.SolicitudProducto;
import com.cafeitsh.backend.service.ServicioProducto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD de productos. Protegido por rol ADMIN en ConfiguracionSeguridad.
 */
@RestController
@RequestMapping("/api/productos")
public class ControladorProducto {

    private final ServicioProducto servicioProducto;

    public ControladorProducto(ServicioProducto servicioProducto) {
        this.servicioProducto = servicioProducto;
    }

    /** Listado completo (incluye no disponibles) para administración. */
    @GetMapping
    public List<RespuestaProducto> listar() {
        return servicioProducto.listar();
    }

    @PostMapping
    public ResponseEntity<RespuestaProducto> crear(
            @Valid @RequestBody SolicitudProducto solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicioProducto.crear(solicitud));
    }

    @PutMapping("/{id}")
    public RespuestaProducto actualizar(
            @PathVariable Long id, @Valid @RequestBody SolicitudActualizacionProducto solicitud) {
        return servicioProducto.actualizar(id, solicitud);
    }

    /** Desactivación lógica: la fila se conserva por los pedidos. Idempotente. */
    @PutMapping("/{id}/desactivar")
    public RespuestaProducto desactivar(@PathVariable Long id) {
        return servicioProducto.desactivar(id);
    }
}
