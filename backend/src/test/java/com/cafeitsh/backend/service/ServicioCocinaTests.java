package com.cafeitsh.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cafeitsh.backend.dto.RespuestaPedido;
import com.cafeitsh.backend.entity.EstadoPedido;
import com.cafeitsh.backend.entity.Pedido;
import com.cafeitsh.backend.entity.Rol;
import com.cafeitsh.backend.entity.Usuario;
import com.cafeitsh.backend.exception.ExcepcionNegocio;
import com.cafeitsh.backend.repository.PedidoRepository;
import com.cafeitsh.backend.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ServicioCocinaTests {

    @Mock
    private PedidoRepository repositorioPedidos;
    @Mock
    private UsuarioRepository repositorioUsuarios;
    @Mock
    private ServicioPedido servicioPedido;

    private ServicioCocina servicio;

    @BeforeEach
    void configurar() {
        servicio = new ServicioCocina(repositorioPedidos, repositorioUsuarios, servicioPedido);
    }

    @Test
    void cocinaListaPedidosPendientes() {
        Usuario cocina = usuario(2L, "cocina@cafeitsh.mx", Rol.COCINA);
        Pedido pedido = pedido(10L, EstadoPedido.PAGADO);
        RespuestaPedido respuesta = respuesta(10L, EstadoPedido.PAGADO);
        when(repositorioUsuarios.findByCorreo("cocina@cafeitsh.mx")).thenReturn(Optional.of(cocina));
        when(repositorioPedidos.buscarPendientesPreparacion()).thenReturn(List.of(pedido));
        when(servicioPedido.aRespuesta(pedido)).thenReturn(respuesta);

        List<RespuestaPedido> pendientes = servicio.listarPendientes("cocina@cafeitsh.mx");

        assertThat(pendientes).containsExactly(respuesta);
    }

    @Test
    void cajaNoPuedeListarPedidosPendientes() {
        Usuario caja = usuario(3L, "caja@cafeitsh.mx", Rol.CAJA);
        when(repositorioUsuarios.findByCorreo("caja@cafeitsh.mx")).thenReturn(Optional.of(caja));

        assertThatThrownBy(() -> servicio.listarPendientes("caja@cafeitsh.mx"))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(e -> assertThat(((ExcepcionNegocio) e).getEstadoHttp()).isEqualTo(403));
        verify(repositorioPedidos, never()).buscarPendientesPreparacion();
    }

    @Test
    void cocinaIniciaPreparacionDesdePagado() {
        Usuario cocina = usuario(2L, "cocina@cafeitsh.mx", Rol.COCINA);
        Pedido pedido = pedido(10L, EstadoPedido.PAGADO);
        when(repositorioUsuarios.findByCorreo("cocina@cafeitsh.mx")).thenReturn(Optional.of(cocina));
        when(repositorioPedidos.findById(10L)).thenReturn(Optional.of(pedido));
        when(repositorioPedidos.save(pedido)).thenReturn(pedido);
        when(servicioPedido.aRespuesta(pedido)).thenReturn(null);

        servicio.iniciarPreparacion(10L, "cocina@cafeitsh.mx");

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.EN_PREPARACION);
    }

    @Test
    void adminMarcaListoDesdeEnPreparacion() {
        Usuario admin = usuario(1L, "admin@cafeitsh.mx", Rol.ADMIN);
        Pedido pedido = pedido(10L, EstadoPedido.EN_PREPARACION);
        when(repositorioUsuarios.findByCorreo("admin@cafeitsh.mx")).thenReturn(Optional.of(admin));
        when(repositorioPedidos.findById(10L)).thenReturn(Optional.of(pedido));
        when(repositorioPedidos.save(pedido)).thenReturn(pedido);
        when(servicioPedido.aRespuesta(pedido)).thenReturn(null);

        servicio.marcarListo(10L, "admin@cafeitsh.mx");

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.LISTO);
    }

    @Test
    void cajaMarcaEntregadoDesdeListo() {
        Usuario caja = usuario(3L, "caja@cafeitsh.mx", Rol.CAJA);
        Pedido pedido = pedido(10L, EstadoPedido.LISTO);
        when(repositorioUsuarios.findByCorreo("caja@cafeitsh.mx")).thenReturn(Optional.of(caja));
        when(repositorioPedidos.findById(10L)).thenReturn(Optional.of(pedido));
        when(repositorioPedidos.save(pedido)).thenReturn(pedido);
        when(servicioPedido.aRespuesta(pedido)).thenReturn(null);

        servicio.marcarEntregado(10L, "caja@cafeitsh.mx");

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ENTREGADO);
    }

    @Test
    void clienteNoPuedeMarcarEntregado() {
        Usuario cliente = usuario(5L, "cliente@cafeitsh.mx", Rol.CLIENTE);
        when(repositorioUsuarios.findByCorreo("cliente@cafeitsh.mx")).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> servicio.marcarEntregado(10L, "cliente@cafeitsh.mx"))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(e -> assertThat(((ExcepcionNegocio) e).getEstadoHttp()).isEqualTo(403));
        verify(repositorioPedidos, never()).findById(any());
    }

    @Test
    void saltoDeEstadoDevuelve409() {
        Usuario cocina = usuario(2L, "cocina@cafeitsh.mx", Rol.COCINA);
        Pedido pedido = pedido(10L, EstadoPedido.PENDIENTE);
        when(repositorioUsuarios.findByCorreo("cocina@cafeitsh.mx")).thenReturn(Optional.of(cocina));
        when(repositorioPedidos.findById(10L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> servicio.iniciarPreparacion(10L, "cocina@cafeitsh.mx"))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(e -> assertThat(((ExcepcionNegocio) e).getEstadoHttp()).isEqualTo(409));
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
        verify(repositorioPedidos, never()).save(any());
    }

    @Test
    void pedidoInexistenteDevuelve404() {
        Usuario cocina = usuario(2L, "cocina@cafeitsh.mx", Rol.COCINA);
        when(repositorioUsuarios.findByCorreo("cocina@cafeitsh.mx")).thenReturn(Optional.of(cocina));
        when(repositorioPedidos.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciarPreparacion(99L, "cocina@cafeitsh.mx"))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(e -> assertThat(((ExcepcionNegocio) e).getEstadoHttp()).isEqualTo(404));
        verify(repositorioPedidos, never()).save(any());
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

    private Pedido pedido(Long id, EstadoPedido estado) {
        Pedido pedido = new Pedido();
        ReflectionTestUtils.setField(pedido, "id", id);
        pedido.setEstado(estado);
        pedido.setTotal(new BigDecimal("50.00"));
        pedido.setFechaHoraEntrega(LocalDateTime.of(2026, 11, 24, 14, 0));
        return pedido;
    }

    private RespuestaPedido respuesta(Long id, EstadoPedido estado) {
        return new RespuestaPedido(id, estado, LocalDateTime.of(2026, 11, 24, 14, 0),
                null, new BigDecimal("50.00"), null, List.of(), null, null, null);
    }
}
