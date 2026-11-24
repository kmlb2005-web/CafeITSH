package com.cafeitsh.backend.service;

import com.cafeitsh.backend.dto.RespuestaCancelacionPedido;
import com.cafeitsh.backend.dto.RespuestaClientePedido;
import com.cafeitsh.backend.dto.RespuestaItemPedido;
import com.cafeitsh.backend.dto.RespuestaPago;
import com.cafeitsh.backend.dto.RespuestaPedido;
import com.cafeitsh.backend.dto.SolicitudCreacionPedido;
import com.cafeitsh.backend.dto.SolicitudItemPedido;
import com.cafeitsh.backend.entity.EstadoPedido;
import com.cafeitsh.backend.entity.Pago;
import com.cafeitsh.backend.entity.Pedido;
import com.cafeitsh.backend.entity.PedidoItem;
import com.cafeitsh.backend.entity.Producto;
import com.cafeitsh.backend.entity.Rol;
import com.cafeitsh.backend.entity.Usuario;
import com.cafeitsh.backend.exception.ExcepcionNegocio;
import com.cafeitsh.backend.repository.PagoRepository;
import com.cafeitsh.backend.repository.PedidoRepository;
import com.cafeitsh.backend.repository.ProductoRepository;
import com.cafeitsh.backend.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gesti?n de pedidos anticipados. Valida la ventana de entrega dentro
 * del horario de servicio, la disponibilidad de productos y captura el
 * precio vigente al crear. Los pedidos son inmutables tras su creaci?n.
 */
@Service
public class ServicioPedido {

    static final ZoneId ZONA_MEXICO = ZoneId.of("America/Mexico_City");

    private final PedidoRepository pedidos;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;
    private final PagoRepository pagos;
    private final LocalTime horaApertura;
    private final LocalTime horaCierre;
    private final int tiempoPreparacionMinutos;
    private final int margenCierreMinutos;

    public ServicioPedido(PedidoRepository pedidos,
                          ProductoRepository productos,
                          UsuarioRepository usuarios,
                          PagoRepository pagos,
                          @Value("${cafeitsh.horario-apertura:08:00}") String horarioApertura,
                          @Value("${cafeitsh.horario-cierre:20:00}") String horarioCierre,
                          @Value("${cafeitsh.tiempo-preparacion-minutos:15}") int tiempoPreparacionMinutos,
                          @Value("${cafeitsh.margen-cierre-minutos:15}") int margenCierreMinutos) {
        this.pedidos = pedidos;
        this.productos = productos;
        this.usuarios = usuarios;
        this.pagos = pagos;
        this.horaApertura = LocalTime.parse(horarioApertura);
        this.horaCierre = LocalTime.parse(horarioCierre);
        this.tiempoPreparacionMinutos = tiempoPreparacionMinutos;
        this.margenCierreMinutos = margenCierreMinutos;
    }

    @Transactional
    public RespuestaPedido crear(SolicitudCreacionPedido solicitud, String correoCliente) {
        Usuario cliente = usuarios.findByCorreo(correoCliente)
                .orElseThrow(() -> new ExcepcionNegocio("Usuario no encontrado", 404));

        LocalDateTime entrega = solicitud.fechaHoraEntrega();
        if (entrega == null) {
            throw new ExcepcionNegocio("La fecha y hora de entrega es obligatoria", 400,
                    Map.of("fechaHoraEntrega", "La fecha y hora de entrega es obligatoria"));
        }
        validarVentanaEntrega(entrega, ahoraMexico());

        if (solicitud.items() == null || solicitud.items().isEmpty()) {
            throw new ExcepcionNegocio("El pedido debe incluir al menos un producto", 400,
                    Map.of("items", "El pedido debe incluir al menos un producto"));
        }

        Pedido pedido = new Pedido();
        pedido.setUsuario(cliente);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setFechaHoraEntrega(entrega);
        pedido.setMetodoPagoPreferido(solicitud.metodoPagoPreferido());

        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (SolicitudItemPedido itemSolicitud : solicitud.items()) {
            PedidoItem item = construirItem(itemSolicitud);
            pedido.agregarItem(item);
            total = total.add(item.getSubtotal());
        }
        pedido.setTotal(total);

        return aRespuesta(pedidos.save(pedido));
    }

    @Transactional(readOnly = true)
    public List<RespuestaPedido> listar(String correoSolicitante) {
        Usuario solicitante = obtenerUsuario(correoSolicitante);
        List<Pedido> resultado = solicitante.getRol() == Rol.CLIENTE
                ? pedidos.buscarPorUsuarioId(solicitante.getId())
                : pedidos.buscarTodosConDetalle();
        return resultado.stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public RespuestaPedido obtenerPorId(Long id, String correoSolicitante) {
        Usuario solicitante = obtenerUsuario(correoSolicitante);
        Pedido pedido = pedidos.buscarPorIdConDetalle(id)
                .orElseThrow(() -> new ExcepcionNegocio("Pedido no encontrado", 404));

        if (solicitante.getRol() == Rol.CLIENTE
                && !pedido.getUsuario().getId().equals(solicitante.getId())) {
            throw new ExcepcionNegocio("No tienes permiso para consultar este pedido", 403);
        }

        return aRespuesta(pedido);
    }

    /**
     * Valida la ventana de entrega: mismo d?a y dentro del horario de
     * servicio (America/Mexico_City). La entrega debe ser al menos el
     * tiempo de preparaci?n despu?s de la solicitud y, como m?ximo, el
     * cierre menos el margen configurado. No se exige alineaci?n a una
     * cuadr?cula de franjas. Visibilidad de paquete para pruebas.
     */
    void validarVentanaEntrega(LocalDateTime entrega, LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        if (!entrega.toLocalDate().equals(hoy)) {
            throw errorEntrega("La fecha de entrega debe ser el mismo d?a de la solicitud");
        }

        LocalDateTime inicioServicio = LocalDateTime.of(hoy, horaApertura);
        LocalDateTime finServicio = LocalDateTime.of(hoy, horaCierre);

        if (ahora.isBefore(inicioServicio) || !ahora.isBefore(finServicio)) {
            throw new ExcepcionNegocio(
                    "Los pedidos solo pueden realizarse dentro del horario de servicio", 400,
                    Map.of("fechaHoraEntrega", String.format(
                            "El horario de servicio es de %s a %s",
                            formatearHora(horaApertura), formatearHora(horaCierre))));
        }

        if (entrega.isBefore(inicioServicio) || entrega.isAfter(finServicio)) {
            throw errorEntrega(String.format(
                    "La hora de entrega debe estar dentro del horario de servicio (%s a %s)",
                    formatearHora(horaApertura), formatearHora(horaCierre)));
        }

        LocalDateTime minimo = ahora.plusMinutes(tiempoPreparacionMinutos);
        if (minimo.isBefore(inicioServicio)) {
            minimo = inicioServicio;
        }
        LocalDateTime maximo = finServicio.minusMinutes(margenCierreMinutos);

        if (maximo.isBefore(minimo)) {
            throw errorEntrega("No hay horarios de entrega disponibles en este momento");
        }
        if (entrega.isBefore(minimo) || entrega.isAfter(maximo)) {
            throw errorEntrega(String.format(
                    "La hora de entrega debe estar entre %s y %s",
                    formatearHora(minimo.toLocalTime()), formatearHora(maximo.toLocalTime())));
        }
    }

    private PedidoItem construirItem(SolicitudItemPedido itemSolicitud) {
        if (itemSolicitud.productoId() == null) {
            throw new ExcepcionNegocio("Los datos enviados no son v?lidos", 400,
                    Map.of("items", "El producto es obligatorio"));
        }
        if (itemSolicitud.cantidad() == null || itemSolicitud.cantidad() < 1) {
            throw new ExcepcionNegocio("Los datos enviados no son v?lidos", 400,
                    Map.of("items", "La cantidad debe ser al menos 1"));
        }

        Producto producto = productos.findById(itemSolicitud.productoId())
                .orElseThrow(() -> new ExcepcionNegocio("Producto no encontrado", 404));

        if (!producto.isDisponible() || producto.getCategoria() == null
                || !producto.getCategoria().isActiva()) {
            throw new ExcepcionNegocio(
                    "El producto no est? disponible para realizar pedidos", 400,
                    Map.of("productoId",
                            "El producto no est? disponible o su categor?a est? inactiva"));
        }

        BigDecimal precioUnitario = producto.getPrecio().setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = precioUnitario
                .multiply(BigDecimal.valueOf(itemSolicitud.cantidad()))
                .setScale(2, RoundingMode.HALF_UP);

        PedidoItem item = new PedidoItem();
        item.setProducto(producto);
        item.setCantidad(itemSolicitud.cantidad());
        item.setPrecioUnitario(precioUnitario);
        item.setSubtotal(subtotal);
        return item;
    }

    private Usuario obtenerUsuario(String correo) {
        return usuarios.findByCorreo(correo)
                .orElseThrow(() -> new ExcepcionNegocio("Usuario no encontrado", 404));
    }

    LocalDateTime ahoraMexico() {
        return LocalDateTime.now(ZONA_MEXICO);
    }

    LocalTime getHoraApertura() {
        return horaApertura;
    }

    LocalTime getHoraCierre() {
        return horaCierre;
    }

    int getTiempoPreparacionMinutos() {
        return tiempoPreparacionMinutos;
    }

    int getMargenCierreMinutos() {
        return margenCierreMinutos;
    }

    private ExcepcionNegocio errorEntrega(String mensaje) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("fechaHoraEntrega", mensaje);
        return new ExcepcionNegocio(mensaje, 400, campos);
    }

    private String formatearHora(LocalTime hora) {
        return String.format("%02d:%02d", hora.getHour(), hora.getMinute());
    }

    RespuestaPedido aRespuesta(Pedido pedido) {
        Usuario usuario = pedido.getUsuario();
        RespuestaClientePedido cliente = new RespuestaClientePedido(
                usuario.getId(), usuario.getNombre(), usuario.getCorreo());

        List<RespuestaItemPedido> items = pedido.getItems().stream()
                .map(i -> new RespuestaItemPedido(
                        i.getProducto().getId(),
                        i.getProducto().getNombre(),
                        i.getCantidad(),
                        i.getPrecioUnitario(),
                        i.getSubtotal()))
                .toList();

        RespuestaPago pago = pagos.findFirstByPedidoIdOrderByFechaPagoDesc(pedido.getId())
                .map(this::aRespuestaPago)
                .orElse(null);

        RespuestaCancelacionPedido cancelacion = null;
        if (pedido.getEstado() == EstadoPedido.CANCELADO || pedido.getCanceladoEn() != null) {
            RespuestaClientePedido canceladoPor = pedido.getCanceladoPor() == null ? null
                    : new RespuestaClientePedido(pedido.getCanceladoPor().getId(),
                    pedido.getCanceladoPor().getNombre(), pedido.getCanceladoPor().getCorreo());
            cancelacion = new RespuestaCancelacionPedido(
                    pedido.getCanceladoEn(),
                    canceladoPor,
                    pedido.getMotivoCancelacion(),
                    pedido.isRequiereReembolso(),
                    pedido.isReembolsoManualRegistrado(),
                    pedido.getObservacionReembolso());
        }

        return new RespuestaPedido(
                pedido.getId(),
                pedido.getEstado(),
                pedido.getFechaHoraEntrega(),
                pedido.getMetodoPagoPreferido(),
                pedido.getTotal(),
                cliente,
                items,
                pedido.getCreadoEn(),
                pago,
                cancelacion);
    }

    private RespuestaPago aRespuestaPago(Pago pago) {
        Usuario registradoPor = pago.getRegistradoPor();
        return new RespuestaPago(
                pago.getId(),
                pago.getMetodoPago(),
                pago.getMonto(),
                pago.getFechaPago(),
                new RespuestaClientePedido(registradoPor.getId(), registradoPor.getNombre(), registradoPor.getCorreo()),
                pago.getReferencia());
    }
}
