package com.cafeitsh.backend.service;

import com.cafeitsh.backend.dto.RespuestaCategoriaResumen;
import com.cafeitsh.backend.dto.RespuestaProducto;
import com.cafeitsh.backend.dto.SolicitudActualizacionProducto;
import com.cafeitsh.backend.dto.SolicitudProducto;
import com.cafeitsh.backend.entity.Categoria;
import com.cafeitsh.backend.entity.Producto;
import com.cafeitsh.backend.exception.ExcepcionNegocio;
import com.cafeitsh.backend.repository.CategoriaRepository;
import com.cafeitsh.backend.repository.ProductoRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administración de productos. Reglas de negocio (spec menu §4):
 * el producto siempre apunta a una categoría existente y activa
 * (si no, 400 con detalle de campos — criterio 7); la desactivación
 * es lógica (disponible = false) porque los pedidos existentes
 * referencian la fila.
 */
@Service
public class ServicioProducto {

    private static final String MENSAJE_CATEGORIA =
            "La categoría indicada no existe o está desactivada";

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;

    public ServicioProducto(ProductoRepository productos, CategoriaRepository categorias) {
        this.productos = productos;
        this.categorias = categorias;
    }

    @Transactional(readOnly = true)
    public List<RespuestaProducto> listar() {
        return productos.findAll().stream().map(this::aRespuesta).toList();
    }

    @Transactional
    public RespuestaProducto crear(SolicitudProducto solicitud) {
        Categoria categoria = obtenerCategoriaActiva(solicitud.categoriaId());

        Producto producto = new Producto();
        producto.setCategoria(categoria);
        producto.setNombre(solicitud.nombre().trim());
        producto.setDescripcion(normalizarDescripcion(solicitud.descripcion()));
        producto.setPrecio(solicitud.precio());
        // Si no se envía disponible, se crea disponible por defecto.
        producto.setDisponible(solicitud.disponible() == null || solicitud.disponible());

        return aRespuesta(productos.save(producto));
    }

    @Transactional
    public RespuestaProducto actualizar(Long id, SolicitudActualizacionProducto solicitud) {
        Producto producto = obtener(id);
        Categoria categoria = obtenerCategoriaActiva(solicitud.categoriaId());

        producto.setCategoria(categoria);
        producto.setNombre(solicitud.nombre().trim());
        producto.setDescripcion(normalizarDescripcion(solicitud.descripcion()));
        producto.setPrecio(solicitud.precio());
        producto.setDisponible(solicitud.disponible());

        return aRespuesta(productos.save(producto));
    }

    /** Desactivación explícita: la fila se conserva para los pedidos. */
    @Transactional
    public RespuestaProducto desactivar(Long id) {
        Producto producto = obtener(id);
        if (!producto.isDisponible()) {
            return aRespuesta(producto); // idempotente
        }

        producto.setDisponible(false);
        return aRespuesta(productos.save(producto));
    }

    /**
     * Toda categoría referenciada debe existir y estar activa. Es una
     * validación de entrada: responde 400 con detalle de `categoriaId`
     * (spec menu §6/criterio 7), no 404.
     */
    private Categoria obtenerCategoriaActiva(Long categoriaId) {
        return categorias.findById(categoriaId)
                .filter(Categoria::isActiva)
                .orElseThrow(() -> new ExcepcionNegocio(
                        "Los datos enviados no son válidos", 400,
                        Map.of("categoriaId", MENSAJE_CATEGORIA)));
    }

    private String normalizarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        return descripcion.trim();
    }

    private Producto obtener(Long id) {
        return productos.findById(id)
                .orElseThrow(() -> new ExcepcionNegocio("Producto no encontrado", 404));
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
