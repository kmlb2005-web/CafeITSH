package com.cafeitsh.backend.service;

import com.cafeitsh.backend.dto.RespuestaUsuario;
import com.cafeitsh.backend.dto.SolicitudActualizacionUsuario;
import com.cafeitsh.backend.dto.SolicitudCreacionUsuario;
import com.cafeitsh.backend.entity.Usuario;
import com.cafeitsh.backend.exception.ExcepcionNegocio;
import com.cafeitsh.backend.repository.UsuarioRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio de usuarios. Solo un ADMIN gestiona usuarios desde la
 * API; el registro público (clientes) vive en {@link ServicioAutenticacion}.
 */
@Service
public class ServicioUsuario {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;

    public ServicioUsuario(UsuarioRepository usuarios, PasswordEncoder codificador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    @Transactional(readOnly = true)
    public List<RespuestaUsuario> listar() {
        return usuarios.findAll().stream().map(this::aRespuesta).toList();
    }

    @Transactional
    public RespuestaUsuario crear(SolicitudCreacionUsuario solicitud) {
        String correo = normalizarCorreo(solicitud.correo());
        if (usuarios.existsByCorreo(correo)) {
            throw new ExcepcionNegocio("El correo ya está registrado", 409);
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(solicitud.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setContrasena(codificador.encode(solicitud.contrasena()));
        usuario.setRol(solicitud.rol());
        usuario.setActivo(true);

        return aRespuesta(usuarios.save(usuario));
    }

    @Transactional
    public RespuestaUsuario actualizar(Long id, SolicitudActualizacionUsuario solicitud) {
        Usuario usuario = obtener(id);
        String correo = normalizarCorreo(solicitud.correo());

        if (usuarios.existsByCorreo(correo) && !correo.equals(usuario.getCorreo())) {
            throw new ExcepcionNegocio("El correo ya está registrado", 409);
        }

        // Un ADMIN no puede desactivarse a sí mismo: evitar quedar sin administradores.
        String correoActual = seguridadActual();
        if (!solicitud.activo() && usuario.getCorreo().equals(correoActual)) {
            throw new ExcepcionNegocio(
                    "No puedes desactivar tu propia cuenta de administrador", 400);
        }

        usuario.setNombre(solicitud.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setRol(solicitud.rol());
        usuario.setActivo(solicitud.activo());

        return aRespuesta(usuarios.save(usuario));
    }

    @Transactional(readOnly = true)
    public Usuario obtener(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new ExcepcionNegocio("Usuario no encontrado", 404));
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase();
    }

    /** Correo del usuario autenticado en la petición actual. */
    private String seguridadActual() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null ? autenticacion.getName() : "";
    }

    private RespuestaUsuario aRespuesta(Usuario usuario) {
        return new RespuestaUsuario(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol(),
                usuario.isActivo());
    }
}
