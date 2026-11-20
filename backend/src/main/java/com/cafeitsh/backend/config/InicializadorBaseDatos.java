package com.cafeitsh.backend.config;

import com.cafeitsh.backend.entity.Rol;
import com.cafeitsh.backend.entity.Usuario;
import com.cafeitsh.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el primer usuario administrador solo si la tabla está vacía.
 *
 * Requiere la variable de entorno ADMIN_PASSWORD (opcional ADMIN_CORREO,
 * por defecto admin@cafeitsh.mx). Si no está definida, no se crea nada y
 * se registra un aviso: nunca se almacena una contraseña por defecto en
 * el repositorio.
 */
@Component
public class InicializadorBaseDatos implements ApplicationRunner {

    private static final Logger bitacora = LoggerFactory.getLogger(InicializadorBaseDatos.class);
    private static final String CORREO_POR_DEFECTO = "admin@cafeitsh.mx";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;

    public InicializadorBaseDatos(UsuarioRepository usuarios, PasswordEncoder codificador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) {
            return;
        }

        String contrasena = System.getenv("ADMIN_PASSWORD");
        if (contrasena == null || contrasena.isBlank()) {
            bitacora.warn(
                    "No hay usuarios y no está definida ADMIN_PASSWORD: "
                            + "se omite la creación del administrador inicial. "
                            + "Define ADMIN_PASSWORD (y opcionalmente ADMIN_CORREO) "
                            + "y reinicia para crearlo.");
            return;
        }

        String correo = System.getenv("ADMIN_CORREO");
        if (correo == null || correo.isBlank()) {
            correo = CORREO_POR_DEFECTO;
        }

        Usuario admin = new Usuario();
        admin.setNombre("Administrador");
        admin.setCorreo(correo.trim().toLowerCase());
        admin.setContrasena(codificador.encode(contrasena));
        admin.setRol(Rol.ADMIN);
        admin.setActivo(true);
        usuarios.save(admin);

        bitacora.info("Usuario administrador inicial creado: {}", admin.getCorreo());
    }
}
