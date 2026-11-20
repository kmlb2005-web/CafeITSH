package com.cafeitsh.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cafeitsh.backend.dto.RespuestaCategoria;
import com.cafeitsh.backend.dto.SolicitudActualizacionCategoria;
import com.cafeitsh.backend.dto.SolicitudCategoria;
import com.cafeitsh.backend.entity.Categoria;
import com.cafeitsh.backend.exception.ExcepcionNegocio;
import com.cafeitsh.backend.repository.CategoriaRepository;
import com.cafeitsh.backend.repository.ProductoRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas unitarias de las reglas de negocio de categorías (spec menu §4):
 * unicidad de nombre, desactivación bloqueada con productos disponibles y
 * desactivación lógica. Sin base de datos (Mockito).
 */
@ExtendWith(MockitoExtension.class)
class ServicioCategoriaTests {

    @Mock
    private CategoriaRepository repositorioCategorias;

    @Mock
    private ProductoRepository repositorioProductos;

    private ServicioCategoria servicio;

    @BeforeEach
    void configurar() {
        servicio = new ServicioCategoria(repositorioCategorias, repositorioProductos);
    }

    private Categoria categoriaExistente(String nombre, boolean activa) {
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setActiva(activa);
        return categoria;
    }

    @Test
    void crearCategoriaConNombreDuplicadoIgnorandoMayusculasDevuelve409() {
        // El servicio trimea y delega la comparación case-insensitive al repositorio.
        when(repositorioCategorias.existsByNombreIgnoreCase("Bebidas")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(new SolicitudCategoria("  Bebidas  ")))
                .isInstanceOf(ExcepcionNegocio.class)
                .hasMessageContaining("Ya existe una categoría con ese nombre")
                .satisfies(ex -> assertThat(((ExcepcionNegocio) ex).getEstadoHttp())
                        .isEqualTo(409));

        verify(repositorioCategorias).existsByNombreIgnoreCase("Bebidas");
        verify(repositorioCategorias, never()).save(any());
    }

    @Test
    void crearCategoriaValidaSeGuardaActivaYConNombreRecortado() {
        when(repositorioCategorias.existsByNombreIgnoreCase("Postres")).thenReturn(false);
        when(repositorioCategorias.save(any(Categoria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RespuestaCategoria respuesta =
                servicio.crear(new SolicitudCategoria("  Postres  "));

        assertThat(respuesta.nombre()).isEqualTo("Postres");
        assertThat(respuesta.activa()).isTrue();
    }

    @Test
    void desactivarCategoriaConProductosDisponiblesDevuelve400() {
        Categoria categoria = categoriaExistente("Bebidas", true);
        when(repositorioCategorias.findById(1L)).thenReturn(Optional.of(categoria));
        when(repositorioProductos.existsByCategoriaIdAndDisponibleTrue(1L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.desactivar(1L))
                .isInstanceOf(ExcepcionNegocio.class)
                .hasMessageContaining("productos disponibles")
                .satisfies(ex -> assertThat(((ExcepcionNegocio) ex).getEstadoHttp())
                        .isEqualTo(400));

        // No se modifica ni se borra nada.
        verify(repositorioCategorias, never()).save(any());
        assertThat(categoria.isActiva()).isTrue();
    }

    @Test
    void desactivarCategoriaSinProductosDisponiblesSeDesactiva() {
        Categoria categoria = categoriaExistente("Bebidas", true);
        when(repositorioCategorias.findById(1L)).thenReturn(Optional.of(categoria));
        when(repositorioProductos.existsByCategoriaIdAndDisponibleTrue(1L)).thenReturn(false);
        when(repositorioCategorias.save(any(Categoria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RespuestaCategoria respuesta = servicio.desactivar(1L);

        assertThat(respuesta.activa()).isFalse();
        verify(repositorioCategorias).save(categoria);
    }

    @Test
    void desactivarCategoriaYaInactivaEsIdempotente() {
        Categoria categoria = categoriaExistente("Bebidas", false);
        when(repositorioCategorias.findById(1L)).thenReturn(Optional.of(categoria));

        RespuestaCategoria respuesta = servicio.desactivar(1L);

        assertThat(respuesta.activa()).isFalse();
        verify(repositorioCategorias, never()).save(any());
        verify(repositorioProductos, never()).existsByCategoriaIdAndDisponibleTrue(any());
    }

    @Test
    void desactivarCategoriaInexistenteDevuelve404() {
        when(repositorioCategorias.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.desactivar(99L))
                .isInstanceOf(ExcepcionNegocio.class)
                .hasMessageContaining("Categoría no encontrada")
                .satisfies(ex -> assertThat(((ExcepcionNegocio) ex).getEstadoHttp())
                        .isEqualTo(404));
    }

    @Test
    void actualizarCategoriaQueDuplicaOtroNombreDevuelve409() {
        Categoria categoria = categoriaExistente("Bebidas", true);
        when(repositorioCategorias.findById(1L)).thenReturn(Optional.of(categoria));
        when(repositorioCategorias.existsByNombreIgnoreCaseAndIdNot("Postres", 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> servicio.actualizar(
                1L, new SolicitudActualizacionCategoria("Postres", true)))
                .isInstanceOf(ExcepcionNegocio.class)
                .hasMessageContaining("Ya existe una categoría con ese nombre")
                .satisfies(ex -> assertThat(((ExcepcionNegocio) ex).getEstadoHttp())
                        .isEqualTo(409));
    }

    @Test
    void actualizarCategoriaADesactivaConProductosDisponiblesDevuelve400() {
        Categoria categoria = categoriaExistente("Bebidas", true);
        when(repositorioCategorias.findById(1L)).thenReturn(Optional.of(categoria));
        when(repositorioCategorias.existsByNombreIgnoreCaseAndIdNot("Bebidas", 1L))
                .thenReturn(false);
        when(repositorioProductos.existsByCategoriaIdAndDisponibleTrue(1L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.actualizar(
                1L, new SolicitudActualizacionCategoria("Bebidas", false)))
                .isInstanceOf(ExcepcionNegocio.class)
                .hasMessageContaining("productos disponibles")
                .satisfies(ex -> assertThat(((ExcepcionNegocio) ex).getEstadoHttp())
                        .isEqualTo(400));
    }
}
