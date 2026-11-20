package com.cafeitsh.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cafeitsh.backend.entity.Rol;
import com.cafeitsh.backend.entity.Usuario;
import com.cafeitsh.backend.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Pruebas de integración del módulo de autenticación y usuarios.
 * Requieren MySQL en ejecución (docker compose up en database/) y las
 * variables de entorno DB_PASSWORD y JWT_SECRET.
 *
 * Los correos llevan sufijo único para no chocar entre ejecuciones.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ControladorAutenticacionTests {

    private static final String SUFIJO = String.valueOf(System.nanoTime());
    private static final String CORREO_CLIENTE = "cliente.test" + SUFIJO + "@correo.mx";
    private static final String CORREO_ADMIN = "admin.test" + SUFIJO + "@correo.mx";
    private static final String CORREO_DUPLICADO = "duplicado.test" + SUFIJO + "@correo.mx";
    private static final String CONTRASENA = "claveSegura123";

    @Autowired
    private MockMvc mvc;

    // Jackson 2 viene por jjwt-jackson (runtime): se crea la instancia
    // directamente porque Boot 4 autoconfigura el bean de Jackson 3.
    private final ObjectMapper om = new ObjectMapper();

    @Autowired
    private UsuarioRepository repositorio;

    @Autowired
    private PasswordEncoder codificador;

    /** Registra un cliente y devuelve su token. */
    private String registrarYObtenerToken(String correo) throws Exception {
        mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Cliente de Prueba","correo":"%s","contrasena":"%s"}
                                """.formatted(correo, CONTRASENA)))
                .andExpect(status().isCreated());

        return iniciarSesion(correo, CONTRASENA);
    }

    private String iniciarSesion(String correo, String contrasena) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s","contrasena":"%s"}
                                """.formatted(correo, contrasena)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andReturn();

        JsonNode cuerpo = om.readTree(resultado.getResponse().getContentAsString());
        return cuerpo.get("token").asText();
    }

    private String cabecera(String token) {
        return "Bearer " + token;
    }

    @Test
    @Order(1)
    void registroCreaClienteYLuegoPermiteIniciarSesion() throws Exception {
        mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Cliente Nuevo","correo":"%s","contrasena":"%s"}
                                """.formatted(CORREO_CLIENTE, CONTRASENA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());

        String token = iniciarSesion(CORREO_CLIENTE, CONTRASENA);
        assertThat(token).isNotBlank();

        // El token funciona en un endpoint protegido y devuelve el rol real.
        mvc.perform(get("/api/auth/me").header("Authorization", cabecera(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(CORREO_CLIENTE))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    @Order(2)
    void loginConContrasenaIncorrectaDevuelve401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s","contrasena":"otraClave999"}
                                """.formatted(CORREO_CLIENTE)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
    }

    @Test
    @Order(3)
    void endpointProtegidoSinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(4)
    void tokenDeClienteEnGestionDeUsuariosDevuelve403() throws Exception {
        String token = iniciarSesion(CORREO_CLIENTE, CONTRASENA);

        mvc.perform(get("/api/usuarios").header("Authorization", cabecera(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje")
                        .value("No tienes permiso para realizar esta operación"));
    }

    @Test
    @Order(5)
    void tokenFalsoDevuelve401() throws Exception {
        mvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer token.inventado.aqui"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    void registroDuplicadoDevuelve409() throws Exception {
        mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Otro","correo":"%s","contrasena":"%s"}
                                """.formatted(CORREO_DUPLICADO, CONTRASENA)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Otro","correo":"%s","contrasena":"%s"}
                                """.formatted(CORREO_DUPLICADO, CONTRASENA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El correo ya está registrado"));
    }

    @Test
    @Order(7)
    void registroConDatosInvalidosDevuelve400ConDetalle() throws Exception {
        mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","correo":"no-es-correo","contrasena":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").isNotEmpty())
                .andExpect(jsonPath("$.campos.correo").isNotEmpty())
                .andExpect(jsonPath("$.campos.contrasena").isNotEmpty());
    }

    @Test
    @Order(8)
    void adminPuedeGestionarUsuarios() throws Exception {
        // Se crea un admin directamente en la BD de pruebas (sin usar el
        // endpoint de registro, que siempre asigna rol CLIENTE).
        crearAdminDePrueba();
        String tokenAdmin = iniciarSesion(CORREO_ADMIN, CONTRASENA);

        // Crear un usuario CAJA.
        MvcResult creado = mvc.perform(post("/api/usuarios")
                        .header("Authorization", cabecera(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Cajero de Prueba","correo":"caja.test%s@correo.mx",
                                 "contrasena":"%s","rol":"CAJA"}
                                """.formatted(SUFIJO, CONTRASENA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CAJA"))
                .andReturn();
        long idCajero = om.readTree(creado.getResponse().getContentAsString()).get("id").asLong();

        // Listar usuarios: debe incluir al cajero recién creado.
        MvcResult listado = mvc.perform(get("/api/usuarios")
                        .header("Authorization", cabecera(tokenAdmin)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode lista = om.readTree(listado.getResponse().getContentAsString());
        assertThat(lista.isArray()).isTrue();
        assertThat(lista.toString()).contains("caja.test" + SUFIJO + "@correo.mx");
        assertThat(listado.getResponse().getContentAsString())
                .doesNotContain(CONTRASENA); // jamás exponer contraseñas

        // Desactivar al cajero.
        mvc.perform(put("/api/usuarios/{id}", idCajero)
                        .header("Authorization", cabecera(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Cajero de Prueba","correo":"caja.test%s@correo.mx",
                                 "rol":"CAJA","activo":false}
                                """.formatted(SUFIJO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        // El cajero desactivado ya no puede iniciar sesión.
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"caja.test%s@correo.mx","contrasena":"%s"}
                                """.formatted(SUFIJO, CONTRASENA)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(9)
    void adminNoPuedeDesactivarSuPropiaCuenta() throws Exception {
        crearAdminDePrueba();
        String tokenAdmin = iniciarSesion(CORREO_ADMIN, CONTRASENA);

        MvcResult listado = mvc.perform(get("/api/usuarios")
                        .header("Authorization", cabecera(tokenAdmin)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode lista = om.readTree(listado.getResponse().getContentAsString());
        long idPropio = -1;
        for (JsonNode usuario : lista) {
            if (CORREO_ADMIN.equals(usuario.get("correo").asText())) {
                idPropio = usuario.get("id").asLong();
            }
        }
        assertThat(idPropio).isPositive();

        mvc.perform(put("/api/usuarios/{id}", idPropio)
                        .header("Authorization", cabecera(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Administrador","correo":"%s",
                                 "rol":"ADMIN","activo":false}
                                """.formatted(CORREO_ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("No puedes desactivar tu propia cuenta de administrador"));
    }

    /** Inserta el admin de prueba si aún no existe (rol ADMIN vía repositorio). */
    private void crearAdminDePrueba() {
        if (repositorio.existsByCorreo(CORREO_ADMIN)) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setNombre("Administrador de Prueba");
        admin.setCorreo(CORREO_ADMIN);
        admin.setContrasena(codificador.encode(CONTRASENA));
        admin.setRol(Rol.ADMIN);
        admin.setActivo(true);
        repositorio.save(admin);
    }
}
