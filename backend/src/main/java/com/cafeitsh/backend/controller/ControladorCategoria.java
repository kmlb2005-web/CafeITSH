package com.cafeitsh.backend.controller;

import com.cafeitsh.backend.dto.RespuestaCategoria;
import com.cafeitsh.backend.dto.SolicitudActualizacionCategoria;
import com.cafeitsh.backend.dto.SolicitudCategoria;
import com.cafeitsh.backend.service.ServicioCategoria;
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
 * CRUD de categorías. Protegido por rol ADMIN en ConfiguracionSeguridad.
 */
@RestController
@RequestMapping("/api/categorias")
public class ControladorCategoria {

    private final ServicioCategoria servicioCategoria;

    public ControladorCategoria(ServicioCategoria servicioCategoria) {
        this.servicioCategoria = servicioCategoria;
    }

    /** Listado completo (incluye inactivas) para administración. */
    @GetMapping
    public List<RespuestaCategoria> listar() {
        return servicioCategoria.listar();
    }

    @PostMapping
    public ResponseEntity<RespuestaCategoria> crear(
            @Valid @RequestBody SolicitudCategoria solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicioCategoria.crear(solicitud));
    }

    @PutMapping("/{id}")
    public RespuestaCategoria actualizar(
            @PathVariable Long id, @Valid @RequestBody SolicitudActualizacionCategoria solicitud) {
        return servicioCategoria.actualizar(id, solicitud);
    }

    /** Desactivación lógica (nunca borra la fila). Idempotente. */
    @PutMapping("/{id}/desactivar")
    public RespuestaCategoria desactivar(@PathVariable Long id) {
        return servicioCategoria.desactivar(id);
    }
}
