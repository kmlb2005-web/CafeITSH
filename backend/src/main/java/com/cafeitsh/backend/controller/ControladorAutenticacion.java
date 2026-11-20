package com.cafeitsh.backend.controller;

import com.cafeitsh.backend.dto.RespuestaLogin;
import com.cafeitsh.backend.dto.RespuestaUsuario;
import com.cafeitsh.backend.dto.SolicitudLogin;
import com.cafeitsh.backend.dto.SolicitudRegistro;
import com.cafeitsh.backend.entity.Usuario;
import com.cafeitsh.backend.exception.ExcepcionNegocio;
import com.cafeitsh.backend.repository.UsuarioRepository;
import com.cafeitsh.backend.service.ServicioAutenticacion;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos de autenticación y consulta del usuario autenticado.
 */
@RestController
@RequestMapping("/api/auth")
public class ControladorAutenticacion {

    private final ServicioAutenticacion servicioAutenticacion;
    private final UsuarioRepository usuarios;

    public ControladorAutenticacion(
            ServicioAutenticacion servicioAutenticacion, UsuarioRepository usuarios) {
        this.servicioAutenticacion = servicioAutenticacion;
        this.usuarios = usuarios;
    }

    /** Registro público: siempre crea usuarios con rol CLIENTE. */
    @PostMapping("/registro")
    public ResponseEntity<RespuestaUsuario> registrar(
            @Valid @RequestBody SolicitudRegistro solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicioAutenticacion.registrar(solicitud));
    }

    @PostMapping("/login")
    public RespuestaLogin iniciarSesion(@Valid @RequestBody SolicitudLogin solicitud) {
        return servicioAutenticacion.iniciarSesion(solicitud);
    }

    /** Devuelve el usuario autenticado (para que el frontend conozca su rol). */
    @GetMapping("/me")
    public RespuestaUsuario usuarioActual(@AuthenticationPrincipal UserDetails detalles) {
        Usuario usuario = usuarios.findByCorreo(detalles.getUsername())
                .orElseThrow(() -> new ExcepcionNegocio("Usuario no encontrado", 404));
        return new RespuestaUsuario(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol(),
                usuario.isActivo());
    }
}
