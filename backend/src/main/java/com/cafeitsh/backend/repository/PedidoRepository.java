package com.cafeitsh.backend.repository;

import com.cafeitsh.backend.entity.Pedido;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a datos de pedidos.
 */
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Query("""
            SELECT DISTINCT p FROM Pedido p
            JOIN FETCH p.usuario u
            LEFT JOIN FETCH p.items i
            LEFT JOIN FETCH i.producto pr
            WHERE u.id = :usuarioId
            ORDER BY p.creadoEn DESC
            """)
    List<Pedido> buscarPorUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT DISTINCT p FROM Pedido p
            JOIN FETCH p.usuario u
            LEFT JOIN FETCH p.items i
            LEFT JOIN FETCH i.producto pr
            ORDER BY p.creadoEn DESC
            """)
    List<Pedido> buscarTodosConDetalle();

    @Query("""
            SELECT DISTINCT p FROM Pedido p
            JOIN FETCH p.usuario u
            LEFT JOIN FETCH p.items i
            LEFT JOIN FETCH i.producto pr
            WHERE p.id = :id
            """)
    Optional<Pedido> buscarPorIdConDetalle(@Param("id") Long id);
}
