package com.cafeitsh.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cafeitsh.backend.dto.RespuestaProducto;
import com.cafeitsh.backend.dto.SolicitudActualizacionProducto;
import com.cafeitsh.backend.dto.SolicitudProducto;
import com.cafeitsh.backend.entity.Categoria;
import com.cafeitsh.backend.entity.Producto;
import com.cafeitsh.backend.exception.ExcepcionNegocio;
import com.cafeitsh.backend.repository.CategoriaRepository;
import com.cafeitsh.backend.repository.ProductoRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas unitarias de las reglas de negocio de productos (spec menu §4):
 * pertenencia a categoría existente y activa, y desactivación lógica que
 * conserva la fila. Sin base de datos (Mockito).
 */
@ExtendWith(MockitoExtension.class)
class ServicioProductoTests {

    @Mock
    private ProductoRepository repositorioProductos;

    @Mock
    private CategoriaRepository repositorioCategorias;

    private ServicioProducto servicio;

    @BeforeEach
    void configurar() {
        servicio = new ServicioProducto(repositorioProductos, repositorioCategorias);
    }

    private Categoria categoria(String nombre, boolean activa) {
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setActiva(activa);
        return categoria;
    }

    private SolicitudProducto solicitud(Long categoriaId) {
        return new SolicitudProducto("Café Americano", "De grano", new BigDecimal("25.00"),
                null, categoriaId);
    }

    @Test
    void crearProductoEnCategoriaInexistenteDevuelve400ConDetalleDeCampos() {
        when(repositorioCategorias.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(solicitud(99L)))
                .isInstanceOf(ExcepcionNegocio.class)
                .hasMessageContaining("Los datos enviados no son válidos")
                .satisfies(ex -> {
                    ExcepcionNegocio negocio = (ExcepcionNegocio) ex;
                    assertThat(negocio.getEstadoHttp()).isEqualTo(400);
                    assertThat(negocio.getCampos())
                            .isEqualTo(Map.of("categoriaId",
                                    "La categoría indicada no existe o está desactivada"));
                });

        verify(repositorioProductos, never()).save(any());
    }

    @Test
    void crearProductoEnCategoriaInactivaDevuelve400ConDetalleDeCampos() {
        when(repositorioCategorias.findById(5L))
                .thenReturn(Optional.of(categoria("Desactivada", false)));

        assertThatThrownBy(() -> servicio.crear(solicitud(5L)))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(ex -> {
                    ExcepcionNegocio negocio = (ExcepcionNegocio) ex;
                    assertThat(negocio.getEstadoHttp()).isEqualTo(400);
                    assertThat(negocio.getCampos()).containsKey("categoriaId");
                });

        verify(repositorioProductos, never()).save(any());
    }

    @Test
    void crearProductoEnCategoriaValidaSeCreaDisponiblePorDefecto() {
        Categoria activa = categoria("Bebidas", true);
        when(repositorioCategorias.findById(5L)).thenReturn(Optional.of(activa));
        when(repositorioProductos.save(any(Producto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RespuestaProducto respuesta = servicio.crear(solicitud(5L));

        assertThat(respuesta.disponible()).isTrue();
        assertThat(respuesta.nombre()).isEqualTo("Café Americano");
        assertThat(respuesta.categoria().nombre()).isEqualTo("Bebidas");
    }

    @Test
    void crearProductoConDisponibleFalseSeCreaNoDisponible() {
        Categoria activa = categoria("Bebidas", true);
        when(repositorioCategorias.findById(5L)).thenReturn(Optional.of(activa));
        when(repositorioProductos.save(any(Producto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RespuestaProducto respuesta = servicio.crear(
                new SolicitudProducto("Café Americano", null, new BigDecimal("25.00"),
                        false, 5L));

        assertThat(respuesta.disponible()).isFalse();
    }

    @Test
    void desactivarProductoCambiaElEstadoSinBorrarLaFila() {
        Categoria activa = categoria("Bebidas", true);
        Producto producto = new Producto();
        producto.setCategoria(activa);
        producto.setNombre("Café Americano");
        producto.setPrecio(new BigDecimal("25.00"));
        producto.setDisponible(true);

        when(repositorioProductos.findById(1L)).thenReturn(Optional.of(producto));
        when(repositorioProductos.save(any(Producto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RespuestaProducto respuesta = servicio.desactivar(1L);

        assertThat(respuesta.disponible()).isFalse();
        // Nunca se borra la fila: los pedidos existentes la referencian.
        verify(repositorioProductos, never()).delete(any());
        verify(repositorioProductos).save(producto);
    }

    @Test
    void desactivarProductoYaNoDisponibleEsIdempotente() {
        Categoria activa = categoria("Bebidas", true);
        Producto producto = new Producto();
        producto.setCategoria(activa);
        producto.setNombre("Café Americano");
        producto.setPrecio(new BigDecimal("25.00"));
        producto.setDisponible(false);

        when(repositorioProductos.findById(1L)).thenReturn(Optional.of(producto));

        RespuestaProducto respuesta = servicio.desactivar(1L);

        assertThat(respuesta.disponible()).isFalse();
        verify(repositorioProductos, never()).save(any());
    }

    @Test
    void actualizarProductoConCategoriaInexistenteDevuelve400ConDetalleDeCampos() {
        Categoria activa = categoria("Bebidas", true);
        Producto producto = new Producto();
        producto.setCategoria(activa);
        producto.setNombre("Café Americano");
        producto.setPrecio(new BigDecimal("25.00"));
        producto.setDisponible(true);

        when(repositorioProductos.findById(1L)).thenReturn(Optional.of(producto));
        when(repositorioCategorias.findById(99L)).thenReturn(Optional.empty());

        SolicitudActualizacionProducto solicitud = new SolicitudActualizacionProducto(
                "Café Americano", null, new BigDecimal("25.00"), true, 99L);

        assertThatThrownBy(() -> servicio.actualizar(1L, solicitud))
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(ex -> {
                    ExcepcionNegocio negocio = (ExcepcionNegocio) ex;
                    assertThat(negocio.getEstadoHttp()).isEqualTo(400);
                    assertThat(negocio.getCampos()).containsKey("categoriaId");
                });

        verify(repositorioProductos, never()).save(any());
    }

    @Test
    void productoInexistenteDevuelve404() {
        when(repositorioProductos.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.desactivar(404L))
                .isInstanceOf(ExcepcionNegocio.class)
                .hasMessageContaining("Producto no encontrado")
                .satisfies(ex -> assertThat(((ExcepcionNegocio) ex).getEstadoHttp())
                        .isEqualTo(404));
    }
}
