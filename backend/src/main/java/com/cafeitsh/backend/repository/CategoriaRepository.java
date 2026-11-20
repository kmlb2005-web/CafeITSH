package com.cafeitsh.backend.repository;

import com.cafeitsh.backend.entity.Categoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de categorías del menú.
 */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    /** Categorías visibles en el menú público. */
    List<Categoria> findByActivaTrueOrderByNombreAsc();

    /** Unicidad de nombre ignorando mayúsculas (spec menu §4). */
    boolean existsByNombreIgnoreCase(String nombre);

    /** Excluir un id propio al validar unicidad en actualizaciones. */
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
