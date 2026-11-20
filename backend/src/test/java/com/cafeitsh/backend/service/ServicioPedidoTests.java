package com.cafeitsh.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cafeitsh.backend.dto.RespuestaPedido;
import com.cafeitsh.backend.dto.SolicitudCreacionPedido;
import com.cafeitsh.backend.dto.SolicitudItemPedido;
import com.cafeitsh.backend.entity.Categoria;
import com.cafeitsh.backend.entity.EstadoPedido;
import com.cafeitsh.backend.entity.Pedido;
import com.cafeitsh.backend.entity.Producto;
import com.cafeitsh.backend.entity.Rol;
import com.cafeitsh.backend.entity.Usuario;
import com.cafeitsh.backend.exception.ExcepcionNegocio;
import com.cafeitsh.backend.repository.PedidoRepository;
import com.cafeitsh.backend.repository.ProductoRepository;
import com.cafeitsh.backend.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pruebas unitarias de reglas de negocio de pedidos (spec pedidos §4, §9).
 * Regla: mismo día, dentro de horario de servicio, entrega entre
 * (ahora + preparación) y (cierre - margen), sin exigir alineación a grilla.
 */
@ExtendWith(MockitoExtension.class)
class ServicioPedidoTests {

    @Mock
    private PedidoRepository repositorioPedidos;

    @Mock
    private ProductoRepository repositorioProductos;

    @Mock
    private UsuarioRepository repositorioUsuarios;

    private ServicioPedido servicio;

    @BeforeEach
    void configurar() {
        servicio = new ServicioPedido(repositorioPedidos, repositorioProductos,
                repositorioUsuarios, "08:00", "20:00", 15, 15);
    }

    @Test
    void ventanaEntregaAceptaDentroDelRango() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 20, 11, 0);
        servicio.validarVentanaEntrega(LocalDateTime.of(2026, 11, 20, 11, 15), ahora);
        servicio.validarVentanaEntrega(LocalDateTime.of(2026, 11, 20, 19, 45), ahora);
        // No es obligatorio caer en múltiplos de 10 minutos
        servicio.validarVentanaEntrega(LocalDateTime.of(2026, 11, 20, 11, 23), ahora);
    }

    @Test
    void ventanaEntregaEjemploAcordado() {
        // 11:00 -> desde 11:15 (ej. 11:20) hasta 19:45
        LocalDateTime ahora11 = LocalDateTime.of(2026, 11, 20, 11, 0);
        servicio.validarVentanaEntrega(LocalDateTime.of(2026, 11, 20, 11, 20), ahora11);
        servicio.validarVentanaEntrega(LocalDateTime.of(2026, 11, 20, 19, 40), ahora11);
        assertThatThrownBy(() -> servicio.validarVentanaEntrega(
                LocalDateTime.of(2026, 11, 20, 11, 14), ahora11))
                .isInstanceOf(ExcepcionNegocio.class);

        // 19:20 -> primer posible 19:35 (ej. 19:40) y último 19:45
        LocalDateTime ahora1920 = LocalDateTime.of(2026, 11, 20, 19, 20);
        servicio.validarVentanaEntrega(LocalDateTime.of(2026, 11, 20, 19, 40), ahora1920);
        servicio.validarVentanaEntrega(LocalDateTime.of(2026, 11, 20, 19, 45), ahora1920);
        assertThatThrownBy(() -> servicio.validarVentanaEntrega(
                LocalDateTime.of(2026, 11, 20, 19, 34), ahora1920))
                .isInstanceOf(ExcepcionNegocio.class);

        // 19:31 -> ahora+15 = 19:46 > 19:45, sin horarios disponibles
        LocalDateTime ahora1931 = LocalDateTime.of(2026, 11, 20, 19, 31);
        assertThatThrownBy(() -> servicio.validarVentanaEntrega(
                LocalDateTime.of(2026, 11, 20, 19, 45), ahora1931))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(ex -> assertThat(((ExcepcionNegocio) ex).getMessage())
                        .contains("No hay horarios"));
    }

    @Test
    void ventanaEntregaRechazaAntesDelTiempoDePreparacion() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 20, 11, 0);
        assertThatThrownBy(() -> servicio.validarVentanaEntrega(
                LocalDateTime.of(2026, 11, 20, 11, 14), ahora))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(ex -> {
                    ExcepcionNegocio e = (ExcepcionNegocio) ex;
                    assertThat(e.getEstadoHttp()).isEqualTo(400);
                    assertThat(e.getCampos()).containsKey("fechaHoraEntrega");
                });
    }

    @Test
    void ventanaEntregaRechazaDespuesDelMargenDeCierre() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 20, 11, 0);
        assertThatThrownBy(() -> servicio.validarVentanaEntrega(
                LocalDateTime.of(2026, 11, 20, 19, 46), ahora))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(ex -> assertThat(((ExcepcionNegocio) ex).getCampos())
                        .containsKey("fechaHoraEntrega"));
    }

    @Test
    void ventanaEntregaRechazaFueraDeHorarioDeServicio() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 20, 11, 0);
        assertThatThrownBy(() -> servicio.validarVentanaEntrega(
                LocalDateTime.of(2026, 11, 20, 7, 30), ahora))
                .isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> servicio.validarVentanaEntrega(
                LocalDateTime.of(2026, 11, 20, 20, 10), ahora))
                .isInstanceOf(ExcepcionNegocio.class);
    }

    @Test
    void ventanaEntregaRechazaCuandoAhoraEsFueraDeServicio() {
        LocalDateTime ahoraAntes = LocalDateTime.of(2026, 11, 20, 7, 30);
        assertThatThrownBy(() -> servicio.validarVentanaEntrega(
                LocalDateTime.of(2026, 11, 20, 8, 30), ahoraAntes))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(ex -> assertThat(((ExcepcionNegocio) ex).getMessage())
                        .contains("horario de servicio"));

        LocalDateTime ahoraDespues = LocalDateTime.of(2026, 11, 20, 20, 5);
        assertThatThrownBy(() -> servicio.validarVentanaEntrega(
                LocalDateTime.of(2026, 11, 20, 19, 30), ahoraDespues))
                .isInstanceOf(ExcepcionNegocio.class);
    }

    @Test
    void ventanaEntregaRechazaDiaDistinto() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 20, 11, 0);
        assertThatThrownBy(() -> servicio.validarVentanaEntrega(
                LocalDateTime.of(2026, 11, 21, 11, 20), ahora))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(ex -> assertThat(((ExcepcionNegocio) ex).getMessage())
                        .contains("mismo"));
    }

    @Test
    void ventanaEntregaRechazaCuandoNoHayHorariosDisponibles() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 20, 19, 31);
        assertThatThrownBy(() -> servicio.validarVentanaEntrega(
                LocalDateTime.of(2026, 11, 20, 19, 45), ahora))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(ex -> assertThat(((ExcepcionNegocio) ex).getMessage())
                        .contains("No hay horarios"));
    }

    @Test
    void crearCapturaPrecioVigenteYCalculaTotal() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 20, 11, 0);
        LocalDateTime entrega = LocalDateTime.of(2026, 11, 20, 11, 20);
        ServicioPedido servicioControlado = new ServicioPedido(repositorioPedidos,
                repositorioProductos, repositorioUsuarios, "08:00", "20:00", 15, 15) {
            @Override
            LocalDateTime ahoraMexico() {
                return ahora;
            }
        };

        Usuario cliente = usuario(5L, "cliente@correo.mx", Rol.CLIENTE);
        Categoria categoria = categoriaActiva();
        Producto producto = producto(3L, "Café", new BigDecimal("25.00"), true, categoria);

        when(repositorioUsuarios.findByCorreo("cliente@correo.mx")).thenReturn(Optional.of(cliente));
        when(repositorioProductos.findById(3L)).thenReturn(Optional.of(producto));
        when(repositorioPedidos.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        SolicitudCreacionPedido solicitud = new SolicitudCreacionPedido(
                entrega, null, List.of(new SolicitudItemPedido(3L, 2)));

        RespuestaPedido respuesta = servicioControlado.crear(solicitud, "cliente@correo.mx");

        assertThat(respuesta.estado()).isEqualTo(EstadoPedido.PENDIENTE);
        assertThat(respuesta.total()).isEqualByComparingTo("50.00");
        assertThat(respuesta.items()).hasSize(1);
        assertThat(respuesta.items().get(0).precioUnitario()).isEqualByComparingTo("25.00");
        assertThat(respuesta.items().get(0).subtotal()).isEqualByComparingTo("50.00");

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(repositorioPedidos).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
        assertThat(captor.getValue().getTotal()).isEqualByComparingTo("50.00");
    }

    @Test
    void crearConProductoInexistenteDevuelve404() {
        ServicioPedido servicioControlado = servicioConAhora(LocalDateTime.of(2026, 11, 20, 11, 0));
        prepararCliente();
        when(repositorioProductos.findById(99L)).thenReturn(Optional.empty());

        SolicitudCreacionPedido solicitud = new SolicitudCreacionPedido(
                LocalDateTime.of(2026, 11, 20, 11, 20), null,
                List.of(new SolicitudItemPedido(99L, 1)));

        assertThatThrownBy(() -> servicioControlado.crear(solicitud, "cliente@correo.mx"))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(ex -> assertThat(((ExcepcionNegocio) ex).getEstadoHttp()).isEqualTo(404));
    }

    @Test
    void crearConProductoNoDisponibleDevuelve400() {
        ServicioPedido servicioControlado = servicioConAhora(LocalDateTime.of(2026, 11, 20, 11, 0));
        prepararCliente();
        Producto producto = producto(3L, "Café", new BigDecimal("25.00"), false, categoriaActiva());
        when(repositorioProductos.findById(3L)).thenReturn(Optional.of(producto));

        SolicitudCreacionPedido solicitud = new SolicitudCreacionPedido(
                LocalDateTime.of(2026, 11, 20, 11, 20), null,
                List.of(new SolicitudItemPedido(3L, 1)));

        assertThatThrownBy(() -> servicioControlado.crear(solicitud, "cliente@correo.mx"))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(ex -> {
                    ExcepcionNegocio e = (ExcepcionNegocio) ex;
                    assertThat(e.getEstadoHttp()).isEqualTo(400);
                    assertThat(e.getCampos()).containsKey("productoId");
                });
    }

    @Test
    void crearConCategoriaInactivaDevuelve400() {
        ServicioPedido servicioControlado = servicioConAhora(LocalDateTime.of(2026, 11, 20, 11, 0));
        prepararCliente();
        Categoria inactiva = new Categoria();
        inactiva.setNombre("Bebidas");
        inactiva.setActiva(false);
        Producto producto = producto(3L, "Café", new BigDecimal("25.00"), true, inactiva);
        when(repositorioProductos.findById(3L)).thenReturn(Optional.of(producto));

        SolicitudCreacionPedido solicitud = new SolicitudCreacionPedido(
                LocalDateTime.of(2026, 11, 20, 11, 20), null,
                List.of(new SolicitudItemPedido(3L, 1)));

        assertThatThrownBy(() -> servicioControlado.crear(solicitud, "cliente@correo.mx"))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(ex -> assertThat(((ExcepcionNegocio) ex).getEstadoHttp()).isEqualTo(400));
    }

    @Test
    void crearSinItemsDevuelve400() {
        ServicioPedido servicioControlado = servicioConAhora(LocalDateTime.of(2026, 11, 20, 11, 0));
        prepararCliente();
        SolicitudCreacionPedido solicitud = new SolicitudCreacionPedido(
                LocalDateTime.of(2026, 11, 20, 11, 20), null, List.of());

        assertThatThrownBy(() -> servicioControlado.crear(solicitud, "cliente@correo.mx"))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(ex -> assertThat(((ExcepcionNegocio) ex).getEstadoHttp()).isEqualTo(400));
    }

    @Test
    void listarComoClienteConsultaSoloPropios() {
        Usuario cliente = usuario(5L, "cliente@correo.mx", Rol.CLIENTE);
        when(repositorioUsuarios.findByCorreo("cliente@correo.mx")).thenReturn(Optional.of(cliente));
        when(repositorioPedidos.buscarPorUsuarioId(5L)).thenReturn(List.of());

        servicio.listar("cliente@correo.mx");

        verify(repositorioPedidos).buscarPorUsuarioId(5L);
    }

    @Test
    void listarComoCajaConsultaTodos() {
        Usuario caja = usuario(9L, "caja@correo.mx", Rol.CAJA);
        when(repositorioUsuarios.findByCorreo("caja@correo.mx")).thenReturn(Optional.of(caja));
        when(repositorioPedidos.buscarTodosConDetalle()).thenReturn(List.of());

        servicio.listar("caja@correo.mx");

        verify(repositorioPedidos).buscarTodosConDetalle();
    }

    @Test
    void clienteNoPuedeConsultarPedidoAjeno() {
        Usuario cliente = usuario(5L, "cliente@correo.mx", Rol.CLIENTE);
        Usuario otro = usuario(8L, "otro@correo.mx", Rol.CLIENTE);
        Pedido pedido = new Pedido();
        pedido.setUsuario(otro);

        when(repositorioUsuarios.findByCorreo("cliente@correo.mx")).thenReturn(Optional.of(cliente));
        when(repositorioPedidos.buscarPorIdConDetalle(10L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> servicio.obtenerPorId(10L, "cliente@correo.mx"))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(ex -> assertThat(((ExcepcionNegocio) ex).getEstadoHttp()).isEqualTo(403));
    }

    @Test
    void obtenerPedidoInexistenteDevuelve404() {
        Usuario admin = usuario(1L, "admin@correo.mx", Rol.ADMIN);
        when(repositorioUsuarios.findByCorreo("admin@correo.mx")).thenReturn(Optional.of(admin));
        when(repositorioPedidos.buscarPorIdConDetalle(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerPorId(999L, "admin@correo.mx"))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(ex -> assertThat(((ExcepcionNegocio) ex).getEstadoHttp()).isEqualTo(404));
    }

    private ServicioPedido servicioConAhora(LocalDateTime ahora) {
        return new ServicioPedido(repositorioPedidos, repositorioProductos,
                repositorioUsuarios, "08:00", "20:00", 15, 15) {
            @Override
            LocalDateTime ahoraMexico() {
                return ahora;
            }
        };
    }

    private void prepararCliente() {
        Usuario cliente = usuario(5L, "cliente@correo.mx", Rol.CLIENTE);
        when(repositorioUsuarios.findByCorreo("cliente@correo.mx")).thenReturn(Optional.of(cliente));
    }

    private Usuario usuario(Long id, String correo, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario");
        usuario.setCorreo(correo);
        usuario.setRol(rol);
        usuario.setActivo(true);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    private Categoria categoriaActiva() {
        Categoria categoria = new Categoria();
        categoria.setNombre("Bebidas");
        categoria.setActiva(true);
        return categoria;
    }

    private Producto producto(Long id, String nombre, BigDecimal precio,
                              boolean disponible, Categoria categoria) {
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setPrecio(precio);
        producto.setDisponible(disponible);
        producto.setCategoria(categoria);
        ReflectionTestUtils.setField(producto, "id", id);
        return producto;
    }
}
