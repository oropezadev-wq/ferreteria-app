package pe.idat.ferreteria.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.idat.ferreteria.dto.CatalogoResponse;
import pe.idat.ferreteria.dto.CategoriaResponse;
import pe.idat.ferreteria.service.CatalogoService;
import pe.idat.ferreteria.service.CategoriaService;

/** Rutas públicas (sin token) para la página de catálogo que ven los clientes. */
@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {

    private final CatalogoService catalogoService;
    private final CategoriaService categoriaService;

    public CatalogoController(CatalogoService catalogoService, CategoriaService categoriaService) {
        this.catalogoService = catalogoService;
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public List<CatalogoResponse> buscar(@RequestParam(defaultValue = "") String buscar,
                                         @RequestParam(required = false) Long categoriaId) {
        return catalogoService.buscar(buscar, categoriaId);
    }

    @GetMapping("/categorias")
    public List<CategoriaResponse> categorias() {
        return categoriaService.listar();
    }
}
