package com.cafeitsh.backend.service;

import com.cafeitsh.backend.dto.RespuestaCategoria;
import com.cafeitsh.backend.dto.SolicitudActualizacionCategoria;
import com.cafeitsh.backend.dto.SolicitudCategoria;
import com.cafeitsh.backend.entity.Categoria;
import com.cafeitsh.backend.exception.ExcepcionNegocio;
import com.cafeitsh.backend.repository.CategoriaRepository;
import com.cafeitsh.backend.repository.ProductoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administración de categorías. Reglas de negocio (spec menu §4):
 * nombre único ignorando mayúsculas; no se desactiva una categoría
 * con productos disponibles.
 */
@Service
public class ServicioCategoria {

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;

    public ServicioCategoria(CategoriaRepository categorias, ProductoRepository productos) {
        this.categorias = categorias;
        this.productos = productos;
    }

    @Transactional(readOnly = true)
    public List<RespuestaCategoria> listar() {
        return categorias.findAll().stream()
                .map(c -> new RespuestaCategoria(c.getId(), c.getNombre(), c.isActiva()))
                .toList();
    }

    @Transactional
    public RespuestaCategoria crear(SolicitudCategoria solicitud) {
        String nombre = solicitud.nombre().trim();
        if (categorias.existsByNombreIgnoreCase(nombre)) {
            throw new ExcepcionNegocio("Ya existe una categoría con ese nombre", 409);
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setActiva(true);

        return aRespuesta(categorias.save(categoria));
    }

    @Transactional
    public RespuestaCategoria actualizar(Long id, SolicitudActualizacionCategoria solicitud) {
        Categoria categoria = obtener(id);
        String nombre = solicitud.nombre().trim();

        if (categorias.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ExcepcionNegocio("Ya existe una categoría con ese nombre", 409);
        }

        boolean desactivando = !solicitud.activa() && categoria.isActiva();
        if (desactivando) {
            validarSinProductosDisponibles(id);
        }

        categoria.setNombre(nombre);
        categoria.setActiva(solicitud.activa());

        return aRespuesta(categorias.save(categoria));
    }

    /** Desactivación explícita (borrado lógico; nunca se borra la fila). */
    @Transactional
    public RespuestaCategoria desactivar(Long id) {
        Categoria categoria = obtener(id);
        if (!categoria.isActiva()) {
            return aRespuesta(categoria); // idempotente
        }

        validarSinProductosDisponibles(id);
        categoria.setActiva(false);

        return aRespuesta(categorias.save(categoria));
    }

    /**
     * Evita productos huérfanos visibles: primero se desactivan o migran
     * los productos de la categoría.
     */
    private void validarSinProductosDisponibles(Long categoriaId) {
        if (productos.existsByCategoriaIdAndDisponibleTrue(categoriaId)) {
            throw new ExcepcionNegocio(
                    "No se puede desactivar una categoría con productos disponibles; "
                            + "desactívalos o muévelos a otra categoría primero", 400);
        }
    }

    private Categoria obtener(Long id) {
        return categorias.findById(id)
                .orElseThrow(() -> new ExcepcionNegocio("Categoría no encontrada", 404));
    }

    private RespuestaCategoria aRespuesta(Categoria categoria) {
        return new RespuestaCategoria(categoria.getId(), categoria.getNombre(), categoria.isActiva());
    }
}
