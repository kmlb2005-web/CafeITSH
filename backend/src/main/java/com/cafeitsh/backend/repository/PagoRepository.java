package com.cafeitsh.backend.repository;

import com.cafeitsh.backend.entity.Pago;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findFirstByPedidoIdOrderByFechaPagoDesc(Long pedidoId);

    @Query("""
            SELECT DISTINCT p FROM Pago p
            JOIN FETCH p.pedido pedido
            JOIN FETCH p.registradoPor
            WHERE p.fechaPago >= :inicio AND p.fechaPago < :fin
            ORDER BY p.fechaPago DESC
            """)
    List<Pago> buscarPorRangoFecha(@Param("inicio") LocalDateTime inicio,
                                   @Param("fin") LocalDateTime fin);
}
