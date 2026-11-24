package pe.idat.ferreteria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import pe.idat.ferreteria.dto.CatalogoResponse;
import pe.idat.ferreteria.model.Categoria;
import pe.idat.ferreteria.model.Producto;
import pe.idat.ferreteria.repository.ProductoRepository;

/** Prueba unitaria del catálogo público con el repositorio simulado. */
class CatalogoServiceTest {

    @Test
    void elCatalogoMuestraDisponibleOAgotadoSinElStockExacto() {
        ProductoRepository repositorio = mock(ProductoRepository.class);
        Producto agotado = new Producto("HE-002", "Amoladora", new Categoria("Herramientas eléctricas"),
                new BigDecimal("120.00"), new BigDecimal("185.00"), 2);
        when(repositorio.buscar("amoladora", true)).thenReturn(List.of(agotado));

        List<CatalogoResponse> lista = new CatalogoService(repositorio).buscar(" amoladora ", null);

        assertEquals(1, lista.size());
        assertEquals(new BigDecimal("185.00"), lista.get(0).precioVenta());
        assertFalse(lista.get(0).disponible());
    }
}
