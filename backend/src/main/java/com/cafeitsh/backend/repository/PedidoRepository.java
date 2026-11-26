package com.cafeitsh.backend.repository;

import com.cafeitsh.backend.entity.Pedido;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Query("""
            SELECT DISTINCT p FROM Pedido p
            JOIN FETCH p.usuario
            LEFT JOIN FETCH p.items i
            LEFT JOIN FETCH i.producto
            WHERE p.usuario.id = :usuarioId
            ORDER BY p.creadoEn DESC
            """)
    List<Pedido> buscarPorUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT DISTINCT p FROM Pedido p
            JOIN FETCH p.usuario
            LEFT JOIN FETCH p.items i
            LEFT JOIN FETCH i.producto
            ORDER BY p.creadoEn DESC
            """)
    List<Pedido> buscarTodosConDetalle();

    @Query("""
            SELECT DISTINCT p FROM Pedido p
            JOIN FETCH p.usuario
            LEFT JOIN FETCH p.items i
            LEFT JOIN FETCH i.producto
            WHERE p.id = :id
            """)
    Optional<Pedido> buscarPorIdConDetalle(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT p FROM Pedido p
            JOIN FETCH p.usuario
            LEFT JOIN FETCH p.items i
            LEFT JOIN FETCH i.producto
            WHERE p.estado = com.cafeitsh.backend.entity.EstadoPedido.PAGADO
            ORDER BY p.fechaHoraEntrega ASC, p.id ASC
            """)
    List<Pedido> buscarPendientesPreparacion();

    @Query("""
            SELECT DISTINCT p FROM Pedido p
            JOIN FETCH p.usuario
            LEFT JOIN FETCH p.canceladoPor
            WHERE p.estado = com.cafeitsh.backend.entity.EstadoPedido.CANCELADO
              AND p.canceladoEn >= :inicio AND p.canceladoEn < :fin
            """)
    List<Pedido> buscarCanceladosPorRango(@Param("inicio") LocalDateTime inicio,
                                          @Param("fin") LocalDateTime fin);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pedido p WHERE p.id = :id")
    Optional<Pedido> findByIdParaActualizar(@Param("id") Long id);
}
