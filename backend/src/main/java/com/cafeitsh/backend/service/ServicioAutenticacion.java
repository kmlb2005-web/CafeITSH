package com.cafeitsh.backend.service;

import com.cafeitsh.backend.dto.RespuestaLogin;
import com.cafeitsh.backend.dto.RespuestaUsuario;
import com.cafeitsh.backend.dto.SolicitudLogin;
import com.cafeitsh.backend.dto.SolicitudRegistro;
import com.cafeitsh.backend.entity.Rol;
import com.cafeitsh.backend.entity.Usuario;
import com.cafeitsh.backend.exception.ExcepcionNegocio;
import com.cafeitsh.backend.repository.UsuarioRepository;
import com.cafeitsh.backend.security.ServicioJwt;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro público de clientes e inicio de sesión.
 */
@Service
public class ServicioAutenticacion {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private final ServicioJwt servicioJwt;

    public ServicioAutenticacion(
            UsuarioRepository usuarios, PasswordEncoder codificador, ServicioJwt servicioJwt) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.servicioJwt = servicioJwt;
    }

    /** El registro público siempre crea CLIENTE; otros roles solo vía ADMIN. */
    @Transactional
    public RespuestaUsuario registrar(SolicitudRegistro solicitud) {
        String correo = solicitud.correo().trim().toLowerCase();
        if (usuarios.existsByCorreo(correo)) {
            throw new ExcepcionNegocio("El correo ya está registrado", 409);
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(solicitud.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setContrasena(codificador.encode(solicitud.contrasena()));
        usuario.setRol(Rol.CLIENTE);
        usuario.setActivo(true);

        return aRespuesta(usuarios.save(usuario));
    }

    /**
     * Valida credenciales y emite el token. Un error de credenciales o una
     * cuenta desactivada producen la misma excepción para no filtrar
     * qué correos existen en el sistema.
     */
    @Transactional(readOnly = true)
    public RespuestaLogin iniciarSesion(SolicitudLogin solicitud) {
        String correo = solicitud.correo().trim().toLowerCase();
        Usuario usuario = usuarios.findByCorreo(correo)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!usuario.isActivo() || !codificador.matches(solicitud.contrasena(), usuario.getContrasena())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        String token = servicioJwt.generarToken(usuario.getCorreo(), usuario.getRol());
        return RespuestaLogin.conToken(token, aRespuesta(usuario));
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
