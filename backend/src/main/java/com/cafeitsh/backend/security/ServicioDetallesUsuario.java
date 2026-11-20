package com.cafeitsh.backend.security;

import com.cafeitsh.backend.entity.Usuario;
import com.cafeitsh.backend.repository.UsuarioRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carga el usuario desde la base de datos para Spring Security.
 * Un usuario desactivado no puede autenticarse (deshabilitado).
 */
@Service
public class ServicioDetallesUsuario implements UserDetailsService {

    private final UsuarioRepository usuarios;

    public ServicioDetallesUsuario(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarios.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return User.builder()
                .username(usuario.getCorreo())
                .password(usuario.getContrasena())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())))
                .disabled(!usuario.isActivo())
                .build();
    }
}
