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
class ControladorCocinaTests {

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
    void flujoCompletoDeCocinaHastaEntregado() throws Exception {
        String tokenCliente = crearUsuarioYToken("cocina.cliente." + SUFIJO + "@cafeitsh.mx", "Cliente", Rol.CLIENTE);
        String tokenCaja = crearUsuarioYToken("cocina.caja." + SUFIJO + "@cafeitsh.mx", "Caja", Rol.CAJA);
        String tokenCocina = crearUsuarioYToken("cocina.chef." + SUFIJO + "@cafeitsh.mx", "Cocina", Rol.COCINA);
        Producto producto = crearProductoDisponible("Cafe cocina " + SUFIJO, "30.00");
        long pedidoId = crearPedido(tokenCliente, producto.getId());
        pagarPedido(pedidoId, tokenCaja);

        String lista = mockMvc.perform(get("/api/cocina/pedidos")
                        .header("Authorization", "Bearer " + tokenCocina))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode arreglo = om.readTree(lista);
        boolean encontrado = false;
        for (JsonNode pedido : arreglo) {
            if (pedido.get("id").asLong() == pedidoId) {
                encontrado = true;
                assertThat(pedido.get("estado").asText()).isEqualTo("PAGADO");
            }
        }
        assertThat(encontrado).isTrue();

        mockMvc.perform(post("/api/pedidos/" + pedidoId + "/en-preparacion")
                        .header("Authorization", "Bearer " + tokenCocina))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PREPARACION"));

        mockMvc.perform(post("/api/pedidos/" + pedidoId + "/listo")
                        .header("Authorization", "Bearer " + tokenCocina))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("LISTO"));

        mockMvc.perform(post("/api/pedidos/" + pedidoId + "/entregado")
                        .header("Authorization", "Bearer " + tokenCocina))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADO"));
    }

    @Test
    void clienteNoPuedeVerNiTransicionarEnCocina() throws Exception {
        String tokenCliente = crearUsuarioYToken("cocina.acl.cliente." + SUFIJO + "@cafeitsh.mx", "Cliente", Rol.CLIENTE);
        String tokenCaja = crearUsuarioYToken("cocina.acl.caja." + SUFIJO + "@cafeitsh.mx", "Caja", Rol.CAJA);
        Producto producto = crearProductoDisponible("Pan cocina " + SUFIJO, "10.00");
        long pedidoId = crearPedido(tokenCliente, producto.getId());
        pagarPedido(pedidoId, tokenCaja);

        mockMvc.perform(get("/api/cocina/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/pedidos/" + pedidoId + "/en-preparacion")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/cocina/pedidos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cajaNoPuedeIniciarPreparacionPeroSiRegistrarEntrega() throws Exception {
        String tokenCliente = crearUsuarioYToken("cocina.rol.cliente." + SUFIJO + "@cafeitsh.mx", "Cliente", Rol.CLIENTE);
        String tokenCaja = crearUsuarioYToken("cocina.rol.caja." + SUFIJO + "@cafeitsh.mx", "Caja", Rol.CAJA);
        String tokenCocina = crearUsuarioYToken("cocina.rol.chef." + SUFIJO + "@cafeitsh.mx", "Cocina", Rol.COCINA);
        Producto producto = crearProductoDisponible("Jugo cocina " + SUFIJO, "20.00");
        long pedidoId = crearPedido(tokenCliente, producto.getId());
        pagarPedido(pedidoId, tokenCaja);

        mockMvc.perform(post("/api/pedidos/" + pedidoId + "/en-preparacion")
                        .header("Authorization", "Bearer " + tokenCaja))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/pedidos/" + pedidoId + "/en-preparacion")
                        .header("Authorization", "Bearer " + tokenCocina))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/pedidos/" + pedidoId + "/listo")
                        .header("Authorization", "Bearer " + tokenCocina))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/pedidos/" + pedidoId + "/entregado")
                        .header("Authorization", "Bearer " + tokenCaja))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADO"));
    }

    @Test
    void transicionSinPagoDevuelve409() throws Exception {
        String tokenCliente = crearUsuarioYToken("cocina.estado.cliente." + SUFIJO + "@cafeitsh.mx", "Cliente", Rol.CLIENTE);
        String tokenCocina = crearUsuarioYToken("cocina.estado.chef." + SUFIJO + "@cafeitsh.mx", "Cocina", Rol.COCINA);
        Producto producto = crearProductoDisponible("Cafe estado " + SUFIJO, "25.00");
        long pedidoId = crearPedido(tokenCliente, producto.getId());

        mockMvc.perform(post("/api/pedidos/" + pedidoId + "/en-preparacion")
                        .header("Authorization", "Bearer " + tokenCocina))
                .andExpect(status().isConflict());
    }

    @Test
    void pedidoInexistenteDevuelve404() throws Exception {
        String tokenCocina = crearUsuarioYToken("cocina.404." + SUFIJO + "@cafeitsh.mx", "Cocina", Rol.COCINA);

        mockMvc.perform(post("/api/pedidos/9223372036854775807/en-preparacion")
                        .header("Authorization", "Bearer " + tokenCocina))
                .andExpect(status().isNotFound());
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
        categoria.setNombre("Cat cocina " + nombre + " " + System.nanoTime());
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

    private void pagarPedido(long pedidoId, String tokenCaja) throws Exception {
        mockMvc.perform(post("/api/pedidos/" + pedidoId + "/pago")
                        .header("Authorization", "Bearer " + tokenCaja)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodoPago\":\"EFECTIVO\"}"))
                .andExpect(status().isOk());
    }
}
