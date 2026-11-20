package com.cafeitsh.backend.repository;

import com.cafeitsh.backend.entity.Producto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a datos de productos del menú.
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /**
     * Menú público: solo productos disponibles en categorías activas.
     * El fetch une la categoría para evitar consultas N+1.
     */
    @Query("""
            SELECT p FROM Producto p
            JOIN FETCH p.categoria c
            WHERE p.disponible = true AND c.activa = true
            ORDER BY c.nombre, p.nombre
            """)
    List<Producto> buscarMenuPublico();

    /** Menú público filtrado por categoría. */
    @Query("""
            SELECT p FROM Producto p
            JOIN FETCH p.categoria c
            WHERE p.disponible = true AND c.activa = true AND c.id = :categoriaId
            ORDER BY p.nombre
            """)
    List<Producto> buscarMenuPublicoPorCategoria(@Param("categoriaId") Long categoriaId);

    /** Regla de negocio: categoría con productos disponibles no se desactiva. */
    boolean existsByCategoriaIdAndDisponibleTrue(Long categoriaId);
}
