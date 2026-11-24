package pe.idat.ferreteria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.idat.ferreteria.dto.ProductoRequest;
import pe.idat.ferreteria.dto.ProductoResponse;
import pe.idat.ferreteria.exception.RecursoNoEncontradoException;
import pe.idat.ferreteria.exception.ReglaNegocioException;
import pe.idat.ferreteria.model.Categoria;
import pe.idat.ferreteria.model.Producto;
import pe.idat.ferreteria.repository.CategoriaRepository;
import pe.idat.ferreteria.repository.ProductoRepository;

/** Pruebas unitarias de las reglas de productos con repositorios simulados. */
class ProductoServiceTest {

    private ProductoRepository productoRepository;
    private ProductoService servicio;

    @BeforeEach
    void preparar() {
        productoRepository = mock(ProductoRepository.class);
        CategoriaRepository categoriaRepository = mock(CategoriaRepository.class);
        servicio = new ProductoService(productoRepository, categoriaRepository);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(new Categoria("Pinturas")));
        when(productoRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void unProductoNuevoEmpiezaConStockCero() {
        ProductoResponse p = servicio.crear(producto("PI-001", "28.00", "42.00"));

        assertEquals(0, p.stock());
        assertEquals("Pinturas", p.categoriaNombre());
    }

    @Test
    void noSeAceptaUnCodigoRepetido() {
        when(productoRepository.existsByCodigoIgnoreCase("PI-001")).thenReturn(true);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(producto("PI-001", "28.00", "42.00")));

        assertEquals("Ya existe un producto con el código PI-001", ex.getMessage());
        verify(productoRepository, never()).save(any());
    }

    @Test
    void elPrecioDeVentaNoPuedeSerMenorQueElDeCompra() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(producto("PI-002", "28.00", "20.00")));

        assertEquals("El precio de venta no puede ser menor que el de compra", ex.getMessage());
    }

    @Test
    void unProductoInexistenteDaError() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener(99L));
    }

    private static ProductoRequest producto(String codigo, String compra, String venta) {
        return new ProductoRequest(codigo, "Pintura látex blanco 1 gl", 1L, new BigDecimal(compra),
                new BigDecimal(venta), 6);
    }
}
