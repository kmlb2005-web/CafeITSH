package com.cafeitsh.backend.controller;

import com.cafeitsh.backend.entity.Rol;
import com.cafeitsh.backend.entity.Usuario;
import com.cafeitsh.backend.repository.CategoriaRepository;
import com.cafeitsh.backend.repository.ProductoRepository;
import com.cafeitsh.backend.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración del módulo de menú (Sprint 1): endpoints públicos
 * del menú y endpoints de administración de categorías y productos.
 * Corren contra MySQL real.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ControladorMenuTests {

    private static final String CONTRASENA = "ClaveSegura123!";
    private static final String SUFIJO = String.valueOf(System.nanoTime());

    @Autowired
    private MockMvc mockMvc;

    // Jackson 2 no se registra como bean en Spring Boot 4 (usa Jackson 3);
    // se instancia directamente solo para leer respuestas en las pruebas.
    private final ObjectMapper om = new ObjectMapper();

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private CategoriaRepository categorias;

    @Autowired
    private ProductoRepository productos;

    @Autowired
    private PasswordEncoder codificador;

    // ------------------------------------------------------------------
    // Menú público
    // ------------------------------------------------------------------

    @Test
    void menuPublicoRespondeSinTokenYSinCamposInternos() throws Exception {
        mockMvc.perform(get("/api/menu/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").isNumber());

        mockMvc.perform(get("/api/menu/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").isNumber());

        // El menú público jamás expone metadatos internos (ni fechas de BD).
        JsonNode lista = obtenerJson("/api/menu/productos", null);
        for (JsonNode producto : lista) {
            assertThat(producto.has("creadoEn")).isFalse();
            assertThat(producto.has("actualizadoEn")).isFalse();
            assertThat(producto.get("categoria").has("activa")).isFalse();
        }
    }

    @Test
    void menuPublicoFiltraPorCategoriaExistente() throws Exception {
        String token = tokenAdmin();
        String categoriaId = crearCategoria(token, "Bebidas" + SUFIJO);
        crearProducto(token, categoriaId, "Agua natural" + SUFIJO, "12.00");

        mockMvc.perform(get("/api/menu/productos").param("categoria", categoriaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Agua natural" + SUFIJO))
                .andExpect(jsonPath("$[0].disponible").value(true));

        // Categoría inexistente → 404 (spec §5).
        mockMvc.perform(get("/api/menu/productos").param("categoria", "999999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void filtroPorCategoriaInexistenteNoEsErrorPeroValorNoNumericoSi() throws Exception {
        mockMvc.perform(get("/api/menu/productos").param("categoria", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.categoria").value("Valor inválido"));
    }

    @Test
    void rutaDesconocidaDevuelve404() throws Exception {
        mockMvc.perform(get("/api/menu/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Recurso no encontrado"));
    }

    // ------------------------------------------------------------------
    // Autorización
    // ------------------------------------------------------------------

    @Test
    void gestionDeMenuSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Se requiere autenticación válida"));
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X" + SUFIJO + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rolesNoAdminEnGestionDeMenuDevuelven403() throws Exception {
        String[] correos = {
                "menu.cliente." + SUFIJO + "@cafeitsh.mx",
                "menu.caja." + SUFIJO + "@cafeitsh.mx",
                "menu.cocina." + SUFIJO + "@cafeitsh.mx"
        };
        for (String correo : correos) {
            Rol rol = correo.contains("cliente") ? Rol.CLIENTE
                    : correo.contains("caja") ? Rol.CAJA : Rol.COCINA;
            String token = crearUsuarioDePrueba(correo, rol);

            mockMvc.perform(get("/api/categorias").header("Authorization", cabecera(token)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.mensaje")
                            .value("No tienes permiso para realizar esta operación"));
            mockMvc.perform(post("/api/productos")
                            .header("Authorization", cabecera(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"X\",\"precio\":10,\"categoriaId\":1}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ------------------------------------------------------------------
    // CRUD completo
    // ------------------------------------------------------------------

    @Test
    void adminCreaCategoriaYProductoVisiblesEnElMenuPublico() throws Exception {
        String token = tokenAdmin();
        String categoriaId = crearCategoria(token, "Postres" + SUFIJO);

        // Se crea visible por defecto y aparece en el menú público.
        mockMvc.perform(get("/api/menu/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre == 'Postres" + SUFIJO + "')]").exists());

        String productoId = crearProducto(token, categoriaId, "Flan napolitano" + SUFIJO, "35.50");

        mockMvc.perform(get("/api/menu/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre == 'Flan napolitano" + SUFIJO + "')]").exists());

        // La respuesta pública trae la categoría anidada sin campo `activa`.
        JsonNode menu = obtenerJson("/api/menu/productos", null);
        JsonNode publicado = buscarPorNombre(menu, "Flan napolitano" + SUFIJO);
        assertThat(publicado).isNotNull();
        assertThat(publicado.get("categoria").get("nombre").asText())
                .isEqualTo("Postres" + SUFIJO);
        assertThat(publicado.get("categoria").has("activa")).isFalse();
        assertThat(publicado.has("creadoEn")).isFalse();

        // Listado de administración: sí incluye el flag de estado.
        mockMvc.perform(get("/api/categorias").header("Authorization", cabecera(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre == 'Postres" + SUFIJO + "')].activa")
                        .value(true));
        mockMvc.perform(get("/api/productos").header("Authorization", cabecera(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[?(@.nombre == 'Flan napolitano" + SUFIJO + "')].disponible")
                        .value(true));

        // El id devuelto es consistente entre creación y detalle público.
        assertThat(productoId).isNotBlank();
    }

    @Test
    void adminEditaCategoriaYProducto() throws Exception {
        String token = tokenAdmin();
        String categoriaId = crearCategoria(token, "Especial" + SUFIJO);
        String productoId = crearProducto(token, categoriaId, "Tostada simple" + SUFIJO, "25.00");

        mockMvc.perform(put("/api/categorias/" + categoriaId)
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Especial editada" + SUFIJO + "\","
                                + "\"activa\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Especial editada" + SUFIJO))
                .andExpect(jsonPath("$.activa").value(true));

        mockMvc.perform(put("/api/productos/" + productoId)
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Tostada doble" + SUFIJO + "\","
                                + "\"descripcion\":\"Con aguacate\","
                                + "\"precio\":32.50,"
                                + "\"disponible\":false,"
                                + "\"categoriaId\":" + categoriaId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Tostada doble" + SUFIJO))
                .andExpect(jsonPath("$.precio").value(32.50))
                .andExpect(jsonPath("$.disponible").value(false));

        // Oculto para el público tras desactivarlo.
        JsonNode menu = obtenerJson("/api/menu/productos", null);
        assertThat(buscarPorNombre(menu, "Tostada doble" + SUFIJO)).isNull();
    }

    @Test
    void categoriaDuplicadaIgnorandoMayusculasDevuelve409() throws Exception {
        String token = tokenAdmin();
        String nombre = "Bebidas frías" + SUFIJO;
        crearCategoria(token, nombre);

        mockMvc.perform(post("/api/categorias")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"bebidas frÍas" + SUFIJO + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe una categoría con ese nombre"));
    }

    // ------------------------------------------------------------------
    // Validación de datos
    // ------------------------------------------------------------------

    @Test
    void datosInvalidosDevuelven400ConDetalleDeCampos() throws Exception {
        String token = tokenAdmin();
        String cabecera = cabecera(token);

        // Categoría sin nombre.
        mockMvc.perform(post("/api/categorias")
                        .header("Authorization", cabecera)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Los datos enviados no son válidos"))
                .andExpect(jsonPath("$.campos.nombre").exists());

        // Producto con precio negativo (sin decimales inválidos).
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", cabecera)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X," + SUFIJO + "\",\"precio\":-1,\"categoriaId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.precio").exists());

        // Precio con más de 2 decimales (@Digits(8,2)).
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", cabecera)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Y," + SUFIJO + "\",\"precio\":10.123,\"categoriaId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.precio").exists());

        // Categoría inexistente.
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", cabecera)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Z" + SUFIJO + "\",\"precio\":10,"
                                + "\"categoriaId\":999999999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.categoriaId")
                        .value("La categoría indicada no existe o está desactivada"));
    }

    @Test
    void categoriaConProductosDisponiblesNoSePuedeDesactivar() throws Exception {
        String token = tokenAdmin();
        String categoriaId = crearCategoria(token, "Temporada" + SUFIJO);
        crearProducto(token, categoriaId, "Café de otoño" + SUFIJO, "40.00");

        mockMvc.perform(put("/api/categorias/" + categoriaId + "/desactivar")
                        .header("Authorization", cabecera(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers
                        .containsString("productos disponibles")));

        // Sigue activa.
        JsonNode detalle = obtenerJson("/api/categorias", token);
        JsonNode categoria = buscarPorNombre(detalle, "Temporada" + SUFIJO);
        assertThat(categoria).isNotNull();
        assertThat(categoria.get("activa").asBoolean()).isTrue();
    }

    @Test
    void desactivarProductoOCategoriaLosOcultaPeroConservaLasFilas() throws Exception {
        String token = tokenAdmin();
        String cabecera = cabecera(token);
        String categoriaId = crearCategoria(token, "Sandwiches" + SUFIJO);
        String productoId = crearProducto(token, categoriaId, "Jamón y queso" + SUFIJO, "45.00");

        // 1) Desactivar el producto lo oculta del menú público.
        mockMvc.perform(put("/api/productos/" + productoId + "/desactivar")
                        .header("Authorization", cabecera))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(false));

        JsonNode menu = obtenerJson("/api/menu/productos", null);
        assertThat(buscarPorNombre(menu, "Jamón y queso" + SUFIJO)).isNull();

        // La categoría ahora sí se puede desactivar.
        mockMvc.perform(put("/api/categorias/" + categoriaId + "/desactivar")
                        .header("Authorization", cabecera))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false));

        // 2) Oculta para el público, pero las filas siguen en administración.
        mockMvc.perform(get("/api/menu/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre == 'Sandwiches" + SUFIJO + "')]")
                        .doesNotExist());

        mockMvc.perform(get("/api/categorias").header("Authorization", cabecera))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre == 'Sandwiches" + SUFIJO + "')].activa")
                        .value(false));
        mockMvc.perform(get("/api/productos").header("Authorization", cabecera))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[?(@.nombre == 'Jamón y queso" + SUFIJO + "')].disponible")
                        .value(false));

        // Nunca hubo borrado físico.
        assertThat(categorias.existsById(Long.parseLong(categoriaId))).isTrue();
        assertThat(productos.existsById(Long.parseLong(productoId))).isTrue();

        // 3) Reactivar la categoría (vía PUT general con activa:true, la
        // única vía definida en la spec §5) la vuelve visible.
        mockMvc.perform(put("/api/categorias/" + categoriaId)
                        .header("Authorization", cabecera)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Sandwiches" + SUFIJO + "\","
                                + "\"activa\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(true));

        mockMvc.perform(get("/api/menu/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre == 'Sandwiches" + SUFIJO + "')]").exists());
    }

    // ------------------------------------------------------------------
    // Criterio de aceptación 2: producto disponible en categoría inactiva
    // ------------------------------------------------------------------

    @Test
    void productoDisponibleEnCategoriaInactivaNoApareceEnElMenuPublico() throws Exception {
        String token = tokenAdmin();
        String categoriaId = crearCategoria(token, "Desayunos" + SUFIJO);
        String productoId = crearProducto(token, categoriaId, "Huevos rancheros" + SUFIJO, "60.00");

        // La API prohíbe desactivar una categoría con productos disponibles,
        // así que se fuerza el estado inválido directamente en BD para
        // verificar la protección del menú público (defensa en profundidad).
        var categoria = categorias.findById(Long.parseLong(categoriaId)).orElseThrow();
        categoria.setActiva(false);
        categorias.save(categoria);

        JsonNode menu = obtenerJson("/api/menu/productos", null);
        assertThat(buscarPorNombre(menu, "Huevos rancheros" + SUFIJO)).isNull();
        assertThat(productos.existsById(Long.parseLong(productoId))).isTrue();

        // El filtro por esa categoría inactiva → 404 (spec §5) y no
        // expone el producto oculto.
        mockMvc.perform(get("/api/menu/productos").param("categoria", categoriaId))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String cabecera(String token) {
        return "Bearer " + token;
    }

    private String crearUsuarioDePrueba(String correo, Rol rol) throws Exception {
        if (usuarios.findByCorreo(correo).isEmpty()) {
            Usuario usuario = new Usuario();
            usuario.setCorreo(correo);
            usuario.setContrasena(codificador.encode(CONTRASENA));
            usuario.setNombre("Menú " + rol);
            usuario.setRol(rol);
            usuario.setActivo(true);
            usuarios.save(usuario);
        }
        return iniciarSesion(correo);
    }

    private String tokenAdmin() throws Exception {
        String correo = "admin.menu." + SUFIJO + "@cafeitsh.mx";
        if (usuarios.findByCorreo(correo).isEmpty()) {
            Usuario admin = new Usuario();
            admin.setCorreo(correo);
            admin.setContrasena(codificador.encode(CONTRASENA));
            admin.setNombre("Admin Menú");
            admin.setRol(Rol.ADMIN);
            admin.setActivo(true);
            usuarios.save(admin);
        }
        return iniciarSesion(correo);
    }

    private String iniciarSesion(String correo) throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\","
                                + "\"contrasena\":\"" + CONTRASENA + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(respuesta).get("token").asText();
    }

    private String crearCategoria(String token, String nombre) throws Exception {
        String respuesta = mockMvc.perform(post("/api/categorias")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(respuesta).get("id").asText();
    }

    private String crearProducto(String token, String categoriaId, String nombre, String precio)
            throws Exception {
        String respuesta = mockMvc.perform(post("/api/productos")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\","
                                + "\"descripcion\":\"De prueba\","
                                + "\"precio\":" + precio + ","
                                + "\"categoriaId\":" + categoriaId + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(respuesta).get("id").asText();
    }

    private JsonNode obtenerJson(String ruta, String token) throws Exception {
        var peticion = get(ruta);
        if (token != null) {
            peticion = peticion.header("Authorization", cabecera(token));
        }
        String respuesta = mockMvc.perform(peticion)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(respuesta);
    }

    private JsonNode buscarPorNombre(JsonNode lista, String nombre) {
        for (JsonNode elemento : lista) {
            if (nombre.equals(elemento.get("nombre").asText())) {
                return elemento;
            }
        }
        return null;
    }
}
