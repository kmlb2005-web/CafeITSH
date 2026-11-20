package com.cafeitsh.backend.controller;

import com.cafeitsh.backend.dto.RespuestaUsuario;
import com.cafeitsh.backend.dto.SolicitudActualizacionUsuario;
import com.cafeitsh.backend.dto.SolicitudCreacionUsuario;
import com.cafeitsh.backend.service.ServicioUsuario;
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
 * Gestión de usuarios. Protegido por rol ADMIN en ConfiguracionSeguridad.
 * No hay borrado físico: la desactivación se hace con {@code activo = false}.
 */
@RestController
@RequestMapping("/api/usuarios")
public class ControladorUsuario {

    private final ServicioUsuario servicioUsuario;

    public ControladorUsuario(ServicioUsuario servicioUsuario) {
        this.servicioUsuario = servicioUsuario;
    }

    @GetMapping
    public List<RespuestaUsuario> listar() {
        return servicioUsuario.listar();
    }

    @PostMapping
    public ResponseEntity<RespuestaUsuario> crear(
            @Valid @RequestBody SolicitudCreacionUsuario solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicioUsuario.crear(solicitud));
    }

    @PutMapping("/{id}")
    public RespuestaUsuario actualizar(
            @PathVariable Long id, @Valid @RequestBody SolicitudActualizacionUsuario solicitud) {
        return servicioUsuario.actualizar(id, solicitud);
    }
}
