package com.cafeitsh.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cafeitsh.backend.entity.Categoria;
import com.cafeitsh.backend.entity.Producto;
import com.cafeitsh.backend.entity.Rol;
import com.cafeitsh.backend.entity.Usuario;
import com.cafeitsh.backend.repository.CategoriaRepository;
import com.cafeitsh.backend.repository.ProductoRepository;
import com.cafeitsh.backend.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pruebas de integración del módulo de pedidos (Sprint 2).
 * Corren contra MySQL real. Se configura horario amplio para disponer
 * de ventana válida durante la ejecución.
 */
@SpringBootTest(properties = {
        "cafeitsh.horario-apertura=00:00",
        "cafeitsh.horario-cierre=23:59",
        "cafeitsh.tiempo-preparacion-minutos=15",
        "cafeitsh.margen-cierre-minutos=15"
})
@AutoConfigureMockMvc
class ControladorPedidoTests {

    private static final String CONTRASENA = "ClaveSegura123!";
    private static final String SUFIJO = String.valueOf(System.nanoTime());
    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper om = new ObjectMapper();

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private CategoriaRepository categorias;

    @Autowired
    private ProductoRepository productos;

    @Autowired
    private PasswordEncoder codificador;

    @Value("${cafeitsh.horario-apertura:08:00}")
    private String horarioApertura;

    @Value("${cafeitsh.horario-cierre:20:00}")
    private String horarioCierre;

    @Value("${cafeitsh.tiempo-preparacion-minutos:15}")
    private int tiempoPreparacionMinutos;

    @Value("${cafeitsh.margen-cierre-minutos:15}")
    private int margenCierreMinutos;

    @Test
    void clienteCreaPedidoValido() throws Exception {
        String correoCliente = "pedido.cliente." + SUFIJO + "@cafeitsh.mx";
        String tokenCliente = crearUsuarioYToken(correoCliente, "Cliente Pedido", Rol.CLIENTE);
        Producto producto = crearProductoDisponible("Café pedido " + SUFIJO, "25.00");

        String entrega = horaEntregaValida().format(FORMATO);
        String cuerpo = "{\"fechaHoraEntrega\":\"" + entrega + "\"," +
                "\"metodoPagoPreferido\":\"EFECTIVO_EN_CAJA\"," +
                "\"items\":[{\"productoId\":" + producto.getId() + ",\"cantidad\":2}]}";

        String respuesta = mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", cabecera(tokenCliente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.total").value(50.00))
                .andExpect(jsonPath("$.cliente.correo").value(correoCliente))
                .andExpect(jsonPath("$.items[0].productoId").value(producto.getId()))
                .andExpect(jsonPath("$.items[0].cantidad").value(2))
                .andExpect(jsonPath("$.items[0].precioUnitario").value(25.00))
                .andExpect(jsonPath("$.items[0].subtotal").value(50.00))
                .andReturn().getResponse().getContentAsString();

        JsonNode json = om.readTree(respuesta);
        assertThat(json.get("id").asLong()).isPositive();
    }

    @Test
    void crearPedidoConHoraFueraDeVentanaDevuelve400() throws Exception {
        String tokenCliente = crearUsuarioYToken(
                "pedido.fuera." + SUFIJO + "@cafeitsh.mx", "Cliente", Rol.CLIENTE);
        Producto producto = crearProductoDisponible("Té fuera " + SUFIJO, "20.00");

        // Menos del tiempo de preparación requerido
        LocalDateTime ahora = LocalDateTime.now(ZONA).withNano(0);
        String entregaFuera = ahora.plusMinutes(Math.max(1, tiempoPreparacionMinutos - 10))
                .withSecond(0).format(FORMATO);
        String cuerpo = "{\"fechaHoraEntrega\":\"" + entregaFuera + "\"," +
                "\"items\":[{\"productoId\":" + producto.getId() + ",\"cantidad\":1}]}";

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", cabecera(tokenCliente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.fechaHoraEntrega").exists());
    }

    @Test
    void crearPedidoConProductoInexistenteDevuelve404() throws Exception {
        String tokenCliente = crearUsuarioYToken(
                "pedido.noexiste." + SUFIJO + "@cafeitsh.mx", "Cliente", Rol.CLIENTE);

        String entrega = horaEntregaValida().format(FORMATO);
        String cuerpo = "{\"fechaHoraEntrega\":\"" + entrega + "\"," +
                "\"items\":[{\"productoId\":999999999,\"cantidad\":1}]}";

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", cabecera(tokenCliente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Producto no encontrado"));
    }

    @Test
    void crearPedidoConProductoNoDisponibleDevuelve400() throws Exception {
        String tokenCliente = crearUsuarioYToken(
                "pedido.nodisp." + SUFIJO + "@cafeitsh.mx", "Cliente", Rol.CLIENTE);
        Producto producto = crearProductoDisponible("Galleta off " + SUFIJO, "15.00");
        producto.setDisponible(false);
        productos.save(producto);

        String entrega = horaEntregaValida().format(FORMATO);
        String cuerpo = "{\"fechaHoraEntrega\":\"" + entrega + "\"," +
                "\"items\":[{\"productoId\":" + producto.getId() + ",\"cantidad\":1}]}";

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", cabecera(tokenCliente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.productoId").exists());
    }

    @Test
    void crearPedidoSinTokenDevuelve401YRolNoClienteDevuelve403() throws Exception {
        Producto producto = crearProductoDisponible("Agua auth " + SUFIJO, "10.00");
        String entrega = horaEntregaValida().format(FORMATO);
        String cuerpo = "{\"fechaHoraEntrega\":\"" + entrega + "\"," +
                "\"items\":[{\"productoId\":" + producto.getId() + ",\"cantidad\":1}]}";

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isUnauthorized());

        String tokenCaja = crearUsuarioYToken(
                "pedido.caja." + SUFIJO + "@cafeitsh.mx", "Caja", Rol.CAJA);
        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", cabecera(tokenCaja))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearPedidoSinItemsDevuelve400() throws Exception {
        String tokenCliente = crearUsuarioYToken(
                "pedido.vacio." + SUFIJO + "@cafeitsh.mx", "Cliente", Rol.CLIENTE);
        String entrega = horaEntregaValida().format(FORMATO);
        String cuerpo = "{\"fechaHoraEntrega\":\"" + entrega + "\",\"items\":[]}";

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", cabecera(tokenCliente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.items").exists());
    }

    @Test
    void clienteVeSoloSusPedidosYCajaVeTodos() throws Exception {
        String correoA = "pedido.a." + SUFIJO + "@cafeitsh.mx";
        String correoB = "pedido.b." + SUFIJO + "@cafeitsh.mx";
        String tokenA = crearUsuarioYToken(correoA, "Cliente A", Rol.CLIENTE);
        String tokenB = crearUsuarioYToken(correoB, "Cliente B", Rol.CLIENTE);
        String tokenCaja = crearUsuarioYToken(
                "pedido.caja2." + SUFIJO + "@cafeitsh.mx", "Caja", Rol.CAJA);

        Producto producto = crearProductoDisponible("Jugo lista " + SUFIJO, "30.00");
        long idA = crearPedido(tokenA, producto.getId());
        long idB = crearPedido(tokenB, producto.getId());

        String respA = mockMvc.perform(get("/api/pedidos")
                        .header("Authorization", cabecera(tokenA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode listaA = om.readTree(respA);
        assertThat(contieneId(listaA, idA)).isTrue();
        assertThat(contieneId(listaA, idB)).isFalse();

        String respCaja = mockMvc.perform(get("/api/pedidos")
                        .header("Authorization", cabecera(tokenCaja)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode listaCaja = om.readTree(respCaja);
        assertThat(contieneId(listaCaja, idA)).isTrue();
        assertThat(contieneId(listaCaja, idB)).isTrue();
    }

    @Test
    void clienteNoPuedeVerPedidoAjenoPeroPuedeVerElPropio() throws Exception {
        String correoA = "pedido.prop." + SUFIJO + "@cafeitsh.mx";
        String correoB = "pedido.otro." + SUFIJO + "@cafeitsh.mx";
        String tokenA = crearUsuarioYToken(correoA, "Cliente A", Rol.CLIENTE);
        String tokenB = crearUsuarioYToken(correoB, "Cliente B", Rol.CLIENTE);

        Producto producto = crearProductoDisponible("Pan ver " + SUFIJO, "18.00");
        long idA = crearPedido(tokenA, producto.getId());

        mockMvc.perform(get("/api/pedidos/" + idA)
                        .header("Authorization", cabecera(tokenB)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/pedidos/" + idA)
                        .header("Authorization", cabecera(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idA));

        mockMvc.perform(get("/api/pedidos/999999999")
                        .header("Authorization", cabecera(tokenA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void precioVigenteSeConservaAunqueCambieDespues() throws Exception {
        String tokenCliente = crearUsuarioYToken(
                "pedido.precio." + SUFIJO + "@cafeitsh.mx", "Cliente", Rol.CLIENTE);
        Producto producto = crearProductoDisponible("Latte precio " + SUFIJO, "40.00");
        long idPedido = crearPedido(tokenCliente, producto.getId());

        producto.setPrecio(new BigDecimal("99.00"));
        productos.save(producto);

        mockMvc.perform(get("/api/pedidos/" + idPedido)
                        .header("Authorization", cabecera(tokenCliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].precioUnitario").value(40.00))
                .andExpect(jsonPath("$.total").value(40.00));
    }

    private String cabecera(String token) {
        return "Bearer " + token;
    }

    private String crearUsuarioYToken(String correo, String nombre, Rol rol) throws Exception {
        if (usuarios.findByCorreo(correo).isEmpty()) {
            Usuario usuario = new Usuario();
            usuario.setCorreo(correo);
            usuario.setContrasena(codificador.encode(CONTRASENA));
            usuario.setNombre(nombre);
            usuario.setRol(rol);
            usuario.setActivo(true);
            usuarios.save(usuario);
        }
        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + CONTRASENA + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(respuesta).get("token").asText();
    }

    private Producto crearProductoDisponible(String nombre, String precio) {
        Categoria categoria = new Categoria();
        categoria.setNombre("Cat pedido " + nombre + " " + System.nanoTime());
        categoria.setActiva(true);
        categoria = categorias.save(categoria);

        Producto producto = new Producto();
        producto.setCategoria(categoria);
        producto.setNombre(nombre);
        producto.setDescripcion("Prueba");
        producto.setPrecio(new BigDecimal(precio));
        producto.setDisponible(true);
        return productos.save(producto);
    }

    private long crearPedido(String tokenCliente, Long productoId) throws Exception {
        String entrega = horaEntregaValida().format(FORMATO);
        String cuerpo = "{\"fechaHoraEntrega\":\"" + entrega + "\"," +
                "\"metodoPagoPreferido\":\"EFECTIVO_EN_CAJA\"," +
                "\"items\":[{\"productoId\":" + productoId + ",\"cantidad\":1}]}";
        String respuesta = mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", cabecera(tokenCliente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(respuesta).get("id").asLong();
    }

    private LocalDateTime horaEntregaValida() {
        LocalDateTime ahora = LocalDateTime.now(ZONA).withNano(0);
        LocalTime apertura = LocalTime.parse(horarioApertura);
        LocalTime cierre = LocalTime.parse(horarioCierre);

        LocalDateTime inicioServicio = LocalDateTime.of(ahora.toLocalDate(), apertura);
        LocalDateTime finServicio = LocalDateTime.of(ahora.toLocalDate(), cierre);
        LocalDateTime minimo = ahora.plusMinutes(tiempoPreparacionMinutos);
        if (minimo.isBefore(inicioServicio)) {
            minimo = inicioServicio;
        }
        LocalDateTime maximo = finServicio.minusMinutes(margenCierreMinutos);

        if (ahora.isBefore(inicioServicio) || !ahora.isBefore(finServicio) || maximo.isBefore(minimo)) {
            throw new IllegalStateException(
                    "No hay ventana válida para ejecutar la prueba en este horario; "
                            + "ajusta las propiedades de prueba o la hora de ejecución.");
        }

        LocalDateTime candidata = minimo.plusMinutes(5).withSecond(0).withNano(0);
        if (candidata.isAfter(maximo)) {
            candidata = maximo.withSecond(0).withNano(0);
        }
        if (candidata.isBefore(minimo)) {
            candidata = minimo.withSecond(0).withNano(0);
        }
        if (candidata.isBefore(minimo) || candidata.isAfter(maximo)) {
            throw new IllegalStateException(
                    "No hay ventana válida para ejecutar la prueba en este horario; "
                            + "ajusta las propiedades de prueba o la hora de ejecución.");
        }
        return candidata;
    }

    private boolean contieneId(JsonNode lista, long id) {
        for (JsonNode nodo : lista) {
            if (nodo.get("id").asLong() == id) {
                return true;
            }
        }
        return false;
    }
}
