package com.cafeitsh.backend.repository;

import com.cafeitsh.backend.entity.PedidoItem;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de ?tems de pedido.
 */
public interface PedidoItemRepository extends JpaRepository<PedidoItem, Long> {
}
