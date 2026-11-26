package com.cafeitsh.backend.service;

import com.cafeitsh.backend.dto.RespuestaCancelacionPedido;
import com.cafeitsh.backend.dto.RespuestaCanceladosCorte;
import com.cafeitsh.backend.dto.RespuestaClientePedido;
import com.cafeitsh.backend.dto.RespuestaCorteCaja;
import com.cafeitsh.backend.dto.RespuestaDesglosePago;
import com.cafeitsh.backend.dto.RespuestaPago;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operaciones de caja: registro de pago, cancelación y corte básico.
 * La autorización fina se valida en servicio además de la capa web.
 */
@Service
public class ServicioCaja {

    private static final ZoneId ZONA_MEXICO = ZoneId.of("America/Mexico_City");

    private final PedidoRepository pedidos;
    private final PagoRepository pagos;
    private final UsuarioRepository usuarios;
    private final ServicioPedido servicioPedido;

    public ServicioCaja(PedidoRepository pedidos,
                        PagoRepository pagos,
                        UsuarioRepository usuarios,
                        ServicioPedido servicioPedido) {
        this.pedidos = pedidos;
        this.pagos = pagos;
        this.usuarios = usuarios;
        this.servicioPedido = servicioPedido;
    }

    @Transactional
    public com.cafeitsh.backend.dto.RespuestaPedido registrarPago(Long pedidoId,
                                                                   SolicitudRegistroPago solicitud,
                                                                   String correoUsuario) {
        Usuario actor = obtenerUsuario(correoUsuario);
        exigirCajaOAdmin(actor);

        if (solicitud == null || solicitud.metodoPago() == null) {
            throw new ExcepcionNegocio("Los datos enviados no son válidos", 400,
                    Map.of("metodoPago", "El método de pago es obligatorio"));
        }

        Pedido pedido = pedidos.findByIdParaActualizar(pedidoId)
                .orElseThrow(() -> new ExcepcionNegocio("Pedido no encontrado", 404));

        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new ExcepcionNegocio(
                    "Solo se puede registrar el pago de un pedido en estado PENDIENTE", 409);
        }

        String referencia = normalizarTexto(solicitud.referencia());
        if (referencia != null && referencia.length() > 100) {
            throw new ExcepcionNegocio("Los datos enviados no son válidos", 400,
                    Map.of("referencia", "La referencia no debe exceder 100 caracteres"));
        }

        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setMetodoPago(solicitud.metodoPago());
        pago.setMonto(pedido.getTotal().setScale(2, RoundingMode.HALF_UP));
        pago.setFechaPago(ahoraMexico());
        pago.setRegistradoPor(actor);
        pago.setReferencia(referencia);
        pagos.save(pago);

        pedido.setEstado(EstadoPedido.PAGADO);
        Pedido guardado = pedidos.save(pedido);
        return servicioPedido.aRespuesta(guardado);
    }

    @Transactional
    public com.cafeitsh.backend.dto.RespuestaPedido cancelar(Long pedidoId,
                                                             SolicitudCancelacionPedido solicitud,
                                                             String correoUsuario) {
        Usuario actor = obtenerUsuario(correoUsuario);
        Pedido pedido = pedidos.findByIdParaActualizar(pedidoId)
                .orElseThrow(() -> new ExcepcionNegocio("Pedido no encontrado", 404));

        validarPermisoCancelacion(pedido, actor);

        if (pedido.getEstado() == EstadoPedido.CANCELADO
                || pedido.getEstado() == EstadoPedido.EN_PREPARACION
                || pedido.getEstado() == EstadoPedido.LISTO
                || pedido.getEstado() == EstadoPedido.ENTREGADO) {
            throw new ExcepcionNegocio(
                    "El pedido no puede cancelarse en su estado actual", 409);
        }

        String motivo = solicitud == null ? null : normalizarTexto(solicitud.motivoCancelacion());
        if (motivo == null || motivo.isBlank()) {
            throw new ExcepcionNegocio("Los datos enviados no son válidos", 400,
                    Map.of("motivoCancelacion", "El motivo de cancelación es obligatorio"));
        }
        if (motivo.length() > 255) {
            throw new ExcepcionNegocio("Los datos enviados no son válidos", 400,
                    Map.of("motivoCancelacion", "El motivo de cancelación no debe exceder 255 caracteres"));
        }

        boolean requiereReembolso = Boolean.TRUE.equals(solicitud.requiereReembolso());
        boolean reembolsoManualRegistrado = Boolean.TRUE.equals(solicitud.reembolsoManualRegistrado());
        String observacion = normalizarTexto(solicitud.observacionReembolso());
        if (observacion != null && observacion.length() > 255) {
            throw new ExcepcionNegocio("Los datos enviados no son válidos", 400,
                    Map.of("observacionReembolso", "La observación de reembolso no debe exceder 255 caracteres"));
        }

        pedido.setEstado(EstadoPedido.CANCELADO);
        pedido.setCanceladoEn(ahoraMexico());
        pedido.setCanceladoPor(actor);
        pedido.setMotivoCancelacion(motivo);
        pedido.setRequiereReembolso(requiereReembolso);
        pedido.setReembolsoManualRegistrado(reembolsoManualRegistrado);
        pedido.setObservacionReembolso(observacion);

        Pedido guardado = pedidos.save(pedido);
        return servicioPedido.aRespuesta(guardado);
    }

    @Transactional(readOnly = true)
    public RespuestaCorteCaja obtenerCorte(LocalDate fecha) {
        LocalDate fechaCorte = fecha != null ? fecha : hoyMexico();
        LocalDateTime inicio = fechaCorte.atStartOfDay();
        LocalDateTime fin = fechaCorte.plusDays(1).atStartOfDay();

        List<Pago> pagosDelDia = pagos.buscarPorRangoFecha(inicio, fin).stream()
                .filter(pago -> pago.getPedido().getEstado() != EstadoPedido.CANCELADO)
                .toList();
        Map<MetodoPago, BigDecimal> montos = new EnumMap<>(MetodoPago.class);
        Map<MetodoPago, Long> cantidades = new EnumMap<>(MetodoPago.class);
        for (MetodoPago metodo : MetodoPago.values()) {
            montos.put(metodo, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            cantidades.put(metodo, 0L);
        }

        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (Pago pago : pagosDelDia) {
            MetodoPago metodo = pago.getMetodoPago();
            BigDecimal monto = pago.getMonto().setScale(2, RoundingMode.HALF_UP);
            montos.put(metodo, montos.get(metodo).add(monto));
            cantidades.put(metodo, cantidades.get(metodo) + 1);
            total = total.add(monto);
        }

        List<RespuestaDesglosePago> desglose = new ArrayList<>();
        for (MetodoPago metodo : MetodoPago.values()) {
            desglose.add(new RespuestaDesglosePago(metodo, cantidades.get(metodo), montos.get(metodo)));
        }

        List<Pedido> cancelados = pedidos.buscarCanceladosPorRango(inicio, fin);
        BigDecimal montoPagadoCancelado = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (Pedido cancelado : cancelados) {
            if (tienePago(cancelado.getId())) {
                montoPagadoCancelado = montoPagadoCancelado.add(cancelado.getTotal().setScale(2, RoundingMode.HALF_UP));
            }
        }

        return new RespuestaCorteCaja(
                fechaCorte,
                ZONA_MEXICO.getId(),
                total,
                pagosDelDia.size(),
                desglose,
                new RespuestaCanceladosCorte(cancelados.size(), montoPagadoCancelado));
    }

    private boolean tienePago(Long pedidoId) {
        return pagos.findFirstByPedidoIdOrderByFechaPagoDesc(pedidoId).isPresent();
    }

    private void validarPermisoCancelacion(Pedido pedido, Usuario actor) {
        if (actor.getRol() == Rol.CLIENTE) {
            if (!pedido.getUsuario().getId().equals(actor.getId())) {
                throw new ExcepcionNegocio("No tienes permiso para cancelar este pedido", 403);
            }
            if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
                throw new ExcepcionNegocio("Solo puedes cancelar pedidos en estado PENDIENTE", 403);
            }
            return;
        }
        if (actor.getRol() == Rol.CAJA || actor.getRol() == Rol.ADMIN) {
            if (pedido.getEstado() == EstadoPedido.PENDIENTE || pedido.getEstado() == EstadoPedido.PAGADO) {
                return;
            }
            throw new ExcepcionNegocio("El pedido no puede cancelarse en su estado actual", 409);
        }
        throw new ExcepcionNegocio("No tienes permiso para realizar esta operación", 403);
    }

    private void exigirCajaOAdmin(Usuario actor) {
        if (actor.getRol() != Rol.CAJA && actor.getRol() != Rol.ADMIN) {
            throw new ExcepcionNegocio("No tienes permiso para realizar esta operación", 403);
        }
    }

    private Usuario obtenerUsuario(String correo) {
        return usuarios.findByCorreo(correo)
                .orElseThrow(() -> new ExcepcionNegocio("Usuario no encontrado", 404));
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim();
        return normalizado.isEmpty() ? null : normalizado;
    }

    LocalDateTime ahoraMexico() {
        return LocalDateTime.now(ZONA_MEXICO);
    }

    LocalDate hoyMexico() {
        return LocalDate.now(ZONA_MEXICO);
    }
}
