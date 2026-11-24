package com.cafeitsh.backend.service;

import com.cafeitsh.backend.dto.RespuestaPedido;
import com.cafeitsh.backend.entity.EstadoPedido;
import com.cafeitsh.backend.entity.Pedido;
import com.cafeitsh.backend.entity.Rol;
import com.cafeitsh.backend.entity.Usuario;
import com.cafeitsh.backend.exception.ExcepcionNegocio;
import com.cafeitsh.backend.repository.PedidoRepository;
import com.cafeitsh.backend.repository.UsuarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioCocina {

    private static final List<Rol> ROLES_COCINA = List.of(Rol.COCINA, Rol.ADMIN);
    private static final List<Rol> ROLES_ENTREGA = List.of(Rol.COCINA, Rol.CAJA, Rol.ADMIN);

    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    private final ServicioPedido servicioPedido;

    public ServicioCocina(PedidoRepository pedidos,
                          UsuarioRepository usuarios,
                          ServicioPedido servicioPedido) {
        this.pedidos = pedidos;
        this.usuarios = usuarios;
        this.servicioPedido = servicioPedido;
    }

    @Transactional(readOnly = true)
    public List<RespuestaPedido> listarPendientes(String correoUsuario) {
        exigirRoles(obtenerUsuario(correoUsuario), ROLES_COCINA);
        return pedidos.buscarPendientesPreparacion().stream()
                .map(servicioPedido::aRespuesta)
                .toList();
    }

    @Transactional
    public RespuestaPedido iniciarPreparacion(Long pedidoId, String correoUsuario) {
        return transicionar(pedidoId, correoUsuario, ROLES_COCINA,
                EstadoPedido.PAGADO, EstadoPedido.EN_PREPARACION);
    }

    @Transactional
    public RespuestaPedido marcarListo(Long pedidoId, String correoUsuario) {
        return transicionar(pedidoId, correoUsuario, ROLES_COCINA,
                EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO);
    }

    @Transactional
    public RespuestaPedido marcarEntregado(Long pedidoId, String correoUsuario) {
        return transicionar(pedidoId, correoUsuario, ROLES_ENTREGA,
                EstadoPedido.LISTO, EstadoPedido.ENTREGADO);
    }

    private RespuestaPedido transicionar(Long pedidoId, String correoUsuario, List<Rol> rolesPermitidos,
                                         EstadoPedido estadoOrigen, EstadoPedido estadoDestino) {
        Usuario actor = obtenerUsuario(correoUsuario);
        exigirRoles(actor, rolesPermitidos);
        Pedido pedido = pedidos.findById(pedidoId)
                .orElseThrow(() -> new ExcepcionNegocio("Pedido no encontrado", 404));
        if (pedido.getEstado() != estadoOrigen) {
            throw new ExcepcionNegocio(
                    "El pedido no está en el estado requerido para esta operación", 409);
        }
        pedido.setEstado(estadoDestino);
        return servicioPedido.aRespuesta(pedidos.save(pedido));
    }

    private void exigirRoles(Usuario actor, List<Rol> rolesPermitidos) {
        if (!rolesPermitidos.contains(actor.getRol())) {
            throw new ExcepcionNegocio("No tienes permiso para realizar esta operación", 403);
        }
    }

    private Usuario obtenerUsuario(String correo) {
        return usuarios.findByCorreo(correo)
                .orElseThrow(() -> new ExcepcionNegocio("Usuario no encontrado", 404));
    }
}
