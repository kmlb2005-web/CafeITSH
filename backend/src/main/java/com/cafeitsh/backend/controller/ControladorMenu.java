package com.cafeitsh.backend.controller;

import com.cafeitsh.backend.dto.RespuestaCategoriaResumen;
import com.cafeitsh.backend.dto.RespuestaProducto;
import com.cafeitsh.backend.service.ServicioMenu;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta pública del menú (sin autenticación): solo categorías activas
 * y productos disponibles. La visibilidad se resuelve en el backend.
 */
@RestController
@RequestMapping("/api/menu")
public class ControladorMenu {

    private final ServicioMenu servicioMenu;

    public ControladorMenu(ServicioMenu servicioMenu) {
        this.servicioMenu = servicioMenu;
    }

    @GetMapping("/categorias")
    public List<RespuestaCategoriaResumen> categorias() {
        return servicioMenu.listarCategorias();
    }

    /** Productos disponibles; filtro opcional por categoría (?categoria=id). */
    @GetMapping("/productos")
    public List<RespuestaProducto> productos(@RequestParam(name = "categoria", required = false)
                                             Long categoriaId) {
        return servicioMenu.listarProductos(categoriaId);
    }
}
