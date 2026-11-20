package com.cafeitsh.backend.service;

import com.cafeitsh.backend.dto.RespuestaCategoriaResumen;
import com.cafeitsh.backend.dto.RespuestaProducto;
import com.cafeitsh.backend.entity.Categoria;
import com.cafeitsh.backend.entity.Producto;
import com.cafeitsh.backend.exception.ExcepcionNegocio;
import com.cafeitsh.backend.repository.CategoriaRepository;
import com.cafeitsh.backend.repository.ProductoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta pública del menú: solo categorías activas y productos
 * disponibles, sin exponer estados internos (spec menu §5/§8).
 */
@Service
public class ServicioMenu {

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;

    public ServicioMenu(CategoriaRepository categorias, ProductoRepository productos) {
        this.categorias = categorias;
        this.productos = productos;
    }

    @Transactional(readOnly = true)
    public List<RespuestaCategoriaResumen> listarCategorias() {
        return categorias.findByActivaTrueOrderByNombreAsc().stream()
                .map(c -> new RespuestaCategoriaResumen(c.getId(), c.getNombre()))
                .toList();
    }

    /**
     * Productos disponibles en categorías activas. Si se indica categoría,
     * debe existir y estar activa (404 si no); una categoría válida sin
     * productos responde 200 con lista vacía.
     */
    @Transactional(readOnly = true)
    public List<RespuestaProducto> listarProductos(Long categoriaId) {
        if (categoriaId != null) {
            validarCategoriaPublica(categoriaId);
        }

        List<Producto> resultado = categoriaId == null
                ? productos.buscarMenuPublico()
                : productos.buscarMenuPublicoPorCategoria(categoriaId);

        return resultado.stream().map(this::aRespuesta).toList();
    }

    /** Distingue "categoría inactiva/inexistente" (404) de "sin productos". */
    private void validarCategoriaPublica(Long categoriaId) {
        categorias.findById(categoriaId)
                .filter(Categoria::isActiva)
                .orElseThrow(() -> new ExcepcionNegocio("Categoría no encontrada", 404));
    }

    private RespuestaProducto aRespuesta(Producto producto) {
        Categoria categoria = producto.getCategoria();
        return new RespuestaProducto(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.isDisponible(),
                new RespuestaCategoriaResumen(categoria.getId(), categoria.getNombre()));
    }
}
