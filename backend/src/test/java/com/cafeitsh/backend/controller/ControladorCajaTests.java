package com.cafeitsh.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
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
import java.time.LocalDate;
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

@SpringBootTest(properties = {
        "cafeitsh.horario-apertura=00:00",
        "cafeitsh.horario-cierre=23:59",
        "cafeitsh.tiempo-preparacion-minutos=15",
        "cafeitsh.margen-cierre-minutos=15"
})
@AutoConfigureMockMvc
class ControladorCajaTests {

    private static final String CONTRASENA = "ClaveSegura123!";
    private static final String SUFIJO = String.valueOf(System.nanoTime());
    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper om = new ObjectMapper();
    @Autowired private UsuarioRepository usuarios;
    @Autowired private CategoriaRepository categorias;
    @Autowired private ProductoRepository productos;
    @Autowired private PasswordEncoder codificador;

    @Test
    void cajaRegistraPagoYPedidoQuedaPagado() throws Exception {
        String tokenCliente = crearUsuarioYToken("caja.cliente." + SUFIJO + "@cafeitsh.mx", "Cliente", Rol.CLIENTE);
        String tokenCaja = crearUsuarioYToken("caja.usuario." + SUFIJO + "@cafeitsh.mx", "Caja", Rol.CAJA);
        Producto producto = crearProductoDisponible("Cafe caja " + SUFIJO, "30.00");
        long pedidoId = crearPedido(tokenCliente, producto.getId());

        mockMvc.perform(post("/api/pedidos/" + pedidoId + "/pago")
                        .header("Authorization", "Bearer " + tokenCaja)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodoPago\":\"EFECTIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADO"))
                .andExpect(jsonPath("$.pago.metodoPago").value("EFECTIVO"))
                .andExpect(jsonPath("$.pago.monto").value(30.00));
    }

    @Test
    void clientePuedeCancelarPendientePropioYNoAjeno() throws Exception {
        String tokenA = crearUsuarioYToken("cancel.a." + SUFIJO + "@cafeitsh.mx", "A", Rol.CLIENTE);
        String tokenB = crearUsuarioYToken("cancel.b." + SUFIJO + "@cafeitsh.mx", "B", Rol.CLIENTE);
        Producto producto = crearProductoDisponible("Pan cancel " + SUFIJO, "10.00");
        long pedidoId = crearPedido(tokenA, producto.getId());

        mockMvc.perform(post("/api/pedidos/" + pedidoId + "/cancelar")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivoCancelacion\":\"No quiero\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/pedidos/" + pedidoId + "/cancelar")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivoCancelacion\":\"No quiero\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.cancelacion.motivoCancelacion").value("No quiero"));
    }

    @Test
    void cajaCancelaPagadoConConstanciaReembolso() throws Exception {
        String tokenCliente = crearUsuarioYToken("reemb.cliente." + SUFIJO + "@cafeitsh.mx", "Cliente", Rol.CLIENTE);
        String tokenCaja = crearUsuarioYToken("reemb.caja." + SUFIJO + "@cafeitsh.mx", "Caja", Rol.CAJA);
        Producto producto = crearProductoDisponible("Jugo reembolso " + SUFIJO, "20.00");
        long pedidoId = crearPedido(tokenCliente, producto.getId());

        mockMvc.perform(post("/api/pedidos/" + pedidoId + "/pago")
                        .header("Authorization", "Bearer " + tokenCaja)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodoPago\":\"TRANSFERENCIA\",\"referencia\":\"REF1\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/pedidos/" + pedidoId + "/cancelar")
                        .header("Authorization", "Bearer " + tokenCaja)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivoCancelacion\":\"Sin capacidad\",\"requiereReembolso\":true,\"reembolsoManualRegistrado\":false,\"observacionReembolso\":\"Devolver en caja\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.cancelacion.requiereReembolso").value(true));
    }

    @Test
    void corteDevuelvePagosDelDia() throws Exception {
        String tokenCliente = crearUsuarioYToken("corte.cliente." + SUFIJO + "@cafeitsh.mx", "Cliente", Rol.CLIENTE);
        String tokenCaja = crearUsuarioYToken("corte.caja." + SUFIJO + "@cafeitsh.mx", "Caja", Rol.CAJA);
        Producto producto = crearProductoDisponible("Corte prod " + SUFIJO, "15.00");
        long pedidoId = crearPedido(tokenCliente, producto.getId());

        mockMvc.perform(post("/api/pedidos/" + pedidoId + "/pago")
                        .header("Authorization", "Bearer " + tokenCaja)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodoPago\":\"EFECTIVO\"}"))
                .andExpect(status().isOk());

        String hoy = LocalDate.now(ZONA).toString();
        mockMvc.perform(get("/api/caja/corte").param("fecha", hoy)
                        .header("Authorization", "Bearer " + tokenCaja))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value(hoy))
                .andExpect(jsonPath("$.cantidadPagos").isNumber())
                .andExpect(jsonPath("$.totalCobrado").isNumber());
    }

    @Test
    void corteNoSumaPedidoPagadoYLuegoCancelado() throws Exception {
        String tokenCliente = crearUsuarioYToken("corte.cancel.cliente." + SUFIJO + "@cafeitsh.mx", "Cliente", Rol.CLIENTE);
        String tokenCaja = crearUsuarioYToken("corte.cancel.caja." + SUFIJO + "@cafeitsh.mx", "Caja", Rol.CAJA);
        Producto producto = crearProductoDisponible("Corte cancel " + SUFIJO, "20.00");
        long pedidoId = crearPedido(tokenCliente, producto.getId());
        String hoy = LocalDate.now(ZONA).toString();

        String antes = mockMvc.perform(get("/api/caja/corte").param("fecha", hoy)
                        .header("Authorization", "Bearer " + tokenCaja))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode jsonAntes = om.readTree(antes);
        BigDecimal totalAntes = new BigDecimal(jsonAntes.get("totalCobrado").asText());
        long canceladosAntes = jsonAntes.get("cancelados").get("cantidad").asLong();
        BigDecimal montoCanceladosAntes = new BigDecimal(
                jsonAntes.get("cancelados").get("montoPagadoCancelado").asText());

        mockMvc.perform(post("/api/pedidos/" + pedidoId + "/pago")
                        .header("Authorization", "Bearer " + tokenCaja)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodoPago\":\"EFECTIVO\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/pedidos/" + pedidoId + "/cancelar")
                        .header("Authorization", "Bearer " + tokenCaja)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivoCancelacion\":\"Sin capacidad\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));

        String despues = mockMvc.perform(get("/api/caja/corte").param("fecha", hoy)
                        .header("Authorization", "Bearer " + tokenCaja))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode jsonDespues = om.readTree(despues);
        assertThat(new BigDecimal(jsonDespues.get("totalCobrado").asText()))
                .isEqualByComparingTo(totalAntes);
        assertThat(jsonDespues.get("cancelados").get("cantidad").asLong())
                .isEqualTo(canceladosAntes + 1);
        assertThat(new BigDecimal(jsonDespues.get("cancelados").get("montoPagadoCancelado").asText()))
                .isEqualByComparingTo(montoCanceladosAntes.add(new BigDecimal("20.00")));
    }

    @Test
    void clienteNoPuedeRegistrarPagoNiVerCorte() throws Exception {
        String tokenCliente = crearUsuarioYToken("seg.c." + SUFIJO + "@cafeitsh.mx", "Cliente", Rol.CLIENTE);
        Producto producto = crearProductoDisponible("Seg prod " + SUFIJO, "10.00");
        long pedidoId = crearPedido(tokenCliente, producto.getId());

        mockMvc.perform(post("/api/pedidos/" + pedidoId + "/pago")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodoPago\":\"EFECTIVO\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/caja/corte")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
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
        categoria.setNombre("Cat caja " + nombre + " " + System.nanoTime());
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
        LocalDateTime ahora = LocalDateTime.now(ZONA).withNano(0);
        assumeTrue(ahora.toLocalTime().isBefore(LocalTime.parse("23:44")),
                "Fuera del horario en que se pueden crear pedidos de prueba");
        LocalDateTime entrega = ahora.plusMinutes(30).withSecond(0).withNano(0);
        LocalTime max = LocalTime.parse("23:44");
        if (entrega.toLocalTime().isAfter(max)) {
            entrega = LocalDateTime.of(ahora.toLocalDate(), max);
        }
        String cuerpo = "{\"fechaHoraEntrega\":\"" + entrega.format(FORMATO) + "\"," +
                "\"metodoPagoPreferido\":\"EFECTIVO_EN_CAJA\"," +
                "\"items\":[{\"productoId\":" + productoId + ",\"cantidad\":1}]}";
        String respuesta = mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(respuesta).get("id").asLong();
    }
}
