package com.cafeitsh.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cafeitsh.backend.dto.RespuestaCorteCaja;
import com.cafeitsh.backend.dto.SolicitudCancelacionPedido;
import com.cafeitsh.backend.dto.SolicitudRegistroPago;
import com.cafeitsh.backend.entity.EstadoPedido;
import com.cafeitsh.backend.entity.MetodoPago;
import com.cafeitsh.backend.entity.Pago;
import com.cafeitsh.backend.entity.Pedido;
import com.cafeitsh.backend.entity.Rol;
import com.cafeitsh.backend.entity.Usuario;
import com.cafeitsh.backend.exception.ExcepcionNegocio;
import com.cafeitsh.backend.repository.PagoRepository;
import com.cafeitsh.backend.repository.PedidoRepository;
import com.cafeitsh.backend.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class ServicioCajaTests {

    @Mock
    private PedidoRepository repositorioPedidos;
    @Mock
    private PagoRepository repositorioPagos;
    @Mock
    private UsuarioRepository repositorioUsuarios;
    @Mock
    private ServicioPedido servicioPedido;

    private ServicioCaja servicio;

    @BeforeEach
    void configurar() {
        servicio = new ServicioCaja(repositorioPedidos, repositorioPagos, repositorioUsuarios, servicioPedido) {
            @Override
            LocalDateTime ahoraMexico() {
                return LocalDateTime.of(2026, 11, 20, 12, 0);
            }

            @Override
            LocalDate hoyMexico() {
                return LocalDate.of(2026, 11, 20);
            }
        };
    }

    @Test
    void registrarPagoCambiaEstadoYGuardaPago() {
        Usuario caja = usuario(2L, "caja@cafeitsh.mx", Rol.CAJA);
        Pedido pedido = pedido(10L, EstadoPedido.PENDIENTE, usuario(5L, "c@c.mx", Rol.CLIENTE));
        when(repositorioUsuarios.findByCorreo("caja@cafeitsh.mx")).thenReturn(Optional.of(caja));
        when(repositorioPedidos.findByIdParaActualizar(10L)).thenReturn(Optional.of(pedido));
        when(repositorioPagos.save(any(Pago.class))).thenAnswer(i -> i.getArgument(0));
        when(repositorioPedidos.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));
        when(servicioPedido.aRespuesta(any(Pedido.class))).thenReturn(null);

        servicio.registrarPago(10L, new SolicitudRegistroPago(MetodoPago.EFECTIVO, null), "caja@cafeitsh.mx");

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PAGADO);
        verify(repositorioPedidos).findByIdParaActualizar(10L);
        verify(repositorioPagos).save(any(Pago.class));
    }

    @Test
    void registrarPagoEnEstadoInvalidoDevuelve409() {
        Usuario caja = usuario(2L, "caja@cafeitsh.mx", Rol.CAJA);
        Pedido pedido = pedido(10L, EstadoPedido.PAGADO, usuario(5L, "c@c.mx", Rol.CLIENTE));
        when(repositorioUsuarios.findByCorreo("caja@cafeitsh.mx")).thenReturn(Optional.of(caja));
        when(repositorioPedidos.findByIdParaActualizar(10L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> servicio.registrarPago(10L, new SolicitudRegistroPago(MetodoPago.EFECTIVO, null), "caja@cafeitsh.mx"))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(e -> assertThat(((ExcepcionNegocio) e).getEstadoHttp()).isEqualTo(409));
    }

    @Test
    void clientePuedeCancelarSoloPendientePropio() {
        Usuario cliente = usuario(5L, "c@c.mx", Rol.CLIENTE);
        Pedido pedido = pedido(10L, EstadoPedido.PENDIENTE, cliente);
        when(repositorioUsuarios.findByCorreo("c@c.mx")).thenReturn(Optional.of(cliente));
        when(repositorioPedidos.findByIdParaActualizar(10L)).thenReturn(Optional.of(pedido));
        when(repositorioPedidos.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));
        when(servicioPedido.aRespuesta(any(Pedido.class))).thenReturn(null);

        servicio.cancelar(10L, new SolicitudCancelacionPedido("Ya no lo quiero", false, false, null), "c@c.mx");
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CANCELADO);
    }

    @Test
    void clienteNoPuedeCancelarPagado() {
        Usuario cliente = usuario(5L, "c@c.mx", Rol.CLIENTE);
        Pedido pedido = pedido(10L, EstadoPedido.PAGADO, cliente);
        when(repositorioUsuarios.findByCorreo("c@c.mx")).thenReturn(Optional.of(cliente));
        when(repositorioPedidos.findByIdParaActualizar(10L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> servicio.cancelar(10L, new SolicitudCancelacionPedido("Motivo", true, false, null), "c@c.mx"))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(e -> assertThat(((ExcepcionNegocio) e).getEstadoHttp()).isEqualTo(403));
    }

    @Test
    void cajaPuedeCancelarPagadoConConstancia() {
        Usuario caja = usuario(2L, "caja@cafeitsh.mx", Rol.CAJA);
        Pedido pedido = pedido(10L, EstadoPedido.PAGADO, usuario(5L, "c@c.mx", Rol.CLIENTE));
        when(repositorioUsuarios.findByCorreo("caja@cafeitsh.mx")).thenReturn(Optional.of(caja));
        when(repositorioPedidos.findByIdParaActualizar(10L)).thenReturn(Optional.of(pedido));
        when(repositorioPedidos.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));
        when(servicioPedido.aRespuesta(any(Pedido.class))).thenReturn(null);

        servicio.cancelar(10L, new SolicitudCancelacionPedido("Sin capacidad", true, false, "Reembolso en efectivo"), "caja@cafeitsh.mx");
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CANCELADO);
        assertThat(pedido.isRequiereReembolso()).isTrue();
        assertThat(pedido.getMotivoCancelacion()).isEqualTo("Sin capacidad");
    }

    @Test
    void noSeCancelaEnPreparacion() {
        Usuario caja = usuario(2L, "caja@cafeitsh.mx", Rol.CAJA);
        Pedido pedido = pedido(10L, EstadoPedido.EN_PREPARACION, usuario(5L, "c@c.mx", Rol.CLIENTE));
        when(repositorioUsuarios.findByCorreo("caja@cafeitsh.mx")).thenReturn(Optional.of(caja));
        when(repositorioPedidos.findByIdParaActualizar(10L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> servicio.cancelar(10L, new SolicitudCancelacionPedido("Motivo", false, false, null), "caja@cafeitsh.mx"))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(e -> assertThat(((ExcepcionNegocio) e).getEstadoHttp()).isEqualTo(409));
    }

    @Test
    void corteCalculaTotalesYDesglose() {
        LocalDate fecha = LocalDate.of(2026, 11, 20);
        Pedido pedido = pedido(10L, EstadoPedido.PAGADO, usuario(5L, "c@c.mx", Rol.CLIENTE));
        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setMetodoPago(MetodoPago.EFECTIVO);
        pago.setMonto(new BigDecimal("50.00"));
        pago.setFechaPago(LocalDateTime.of(2026, 11, 20, 10, 0));
        pago.setRegistradoPor(usuario(2L, "caja@cafeitsh.mx", Rol.CAJA));

        when(repositorioPagos.buscarPorRangoFecha(any(), any())).thenReturn(List.of(pago));
        when(repositorioPedidos.buscarCanceladosPorRango(any(), any())).thenReturn(List.of());

        RespuestaCorteCaja corte = servicio.obtenerCorte(fecha);
        assertThat(corte.totalCobrado()).isEqualByComparingTo("50.00");
        assertThat(corte.cantidadPagos()).isEqualTo(1);
        assertThat(corte.desglosePorMetodo()).hasSize(2);
    }

    @Test
    void corteNoSumaPagoDePedidoCancelado() {
        LocalDate fecha = LocalDate.of(2026, 11, 20);
        Pedido cancelado = pedido(11L, EstadoPedido.CANCELADO, usuario(5L, "c@c.mx", Rol.CLIENTE));
        Pago pago = new Pago();
        pago.setPedido(cancelado);
        pago.setMetodoPago(MetodoPago.TRANSFERENCIA);
        pago.setMonto(new BigDecimal("50.00"));
        pago.setFechaPago(LocalDateTime.of(2026, 11, 20, 10, 0));
        pago.setRegistradoPor(usuario(2L, "caja@cafeitsh.mx", Rol.CAJA));

        when(repositorioPagos.buscarPorRangoFecha(any(), any())).thenReturn(List.of(pago));
        when(repositorioPedidos.buscarCanceladosPorRango(any(), any())).thenReturn(List.of(cancelado));
        when(repositorioPagos.findFirstByPedidoIdOrderByFechaPagoDesc(11L)).thenReturn(Optional.of(pago));

        RespuestaCorteCaja corte = servicio.obtenerCorte(fecha);
        assertThat(corte.totalCobrado()).isEqualByComparingTo("0.00");
        assertThat(corte.cantidadPagos()).isZero();
        assertThat(corte.cancelados().cantidad()).isEqualTo(1);
        assertThat(corte.cancelados().montoPagadoCancelado()).isEqualByComparingTo("50.00");
    }

    private Usuario usuario(Long id, String correo, Rol rol) {
        Usuario u = new Usuario();
        u.setNombre("U");
        u.setCorreo(correo);
        u.setRol(rol);
        u.setActivo(true);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private Pedido pedido(Long id, EstadoPedido estado, Usuario cliente) {
        Pedido p = new Pedido();
        ReflectionTestUtils.setField(p, "id", id);
        p.setEstado(estado);
        p.setUsuario(cliente);
        p.setTotal(new BigDecimal("50.00"));
        p.setFechaHoraEntrega(LocalDateTime.of(2026, 11, 20, 14, 0));
        return p;
    }
}
