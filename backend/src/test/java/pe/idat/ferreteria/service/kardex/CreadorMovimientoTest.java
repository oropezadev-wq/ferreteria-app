package pe.idat.ferreteria.service.kardex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.idat.ferreteria.exception.ReglaNegocioException;
import pe.idat.ferreteria.model.Categoria;
import pe.idat.ferreteria.model.DocumentoIngreso;
import pe.idat.ferreteria.model.MovimientoKardex;
import pe.idat.ferreteria.model.Producto;
import pe.idat.ferreteria.model.TipoDocumento;
import pe.idat.ferreteria.model.TipoMovimiento;

/** Pruebas unitarias del Factory Method: sin Spring ni base de datos. */
class CreadorMovimientoTest {

    private Producto martillo;

    private final DocumentoIngreso guia = new DocumentoIngreso(TipoDocumento.GUIA_REMISION, "T001-000123",
            LocalDate.of(2026, 11, 3), "Distribuidora Ejemplo SAC");

    @BeforeEach
    void prepararProducto() {
        martillo = new Producto("HM-001", "Martillo", new Categoria("Herramientas"),
                new BigDecimal("18.50"), new BigDecimal("29.90"), 5);
        martillo.setStock(10);
    }

    @Test
    void entradaSumaAlStock() {
        MovimientoKardex m = new CreadorEntrada().registrar(martillo, 4, "Compra", guia);

        assertEquals(TipoMovimiento.ENTRADA, m.getTipo());
        assertEquals(4, m.getCantidad());
        assertEquals(10, m.getStockAnterior());
        assertEquals(14, martillo.getStock());
        assertEquals("T001-000123", m.getNumeroDocumento());
        assertEquals(LocalDate.of(2026, 11, 3), m.getFechaDocumento());
    }

    @Test
    void entradaSinDocumentoSeRechazaYNoCambiaElProducto() {
        DocumentoIngreso sinNumero = new DocumentoIngreso(TipoDocumento.FACTURA, " ",
                LocalDate.of(2026, 11, 3), "Distribuidora Ejemplo SAC");

        assertThrows(ReglaNegocioException.class, () -> new CreadorEntrada().registrar(martillo, 4, "Compra", null));
        assertThrows(ReglaNegocioException.class,
                () -> new CreadorEntrada().registrar(martillo, 4, "Compra", sinNumero));
        assertEquals(10, martillo.getStock());
    }

    @Test
    void salidaRestaDelStock() {
        MovimientoKardex m = new CreadorSalida().registrar(martillo, 3, "Merma", guia);

        assertEquals(-3, m.getCantidad());
        assertNull(m.getNumeroDocumento());
        assertEquals(7, martillo.getStock());
    }

    @Test
    void salidaMayorAlStockSeRechazaYNoCambiaElProducto() {
        assertThrows(ReglaNegocioException.class, () -> new CreadorSalida().registrar(martillo, 11, "Venta", null));
        assertEquals(10, martillo.getStock());
    }

    @Test
    void ajusteGuardaLaDiferenciaConElConteo() {
        MovimientoKardex m = new CreadorAjuste().registrar(martillo, 8, "Inventario físico", null);

        assertEquals(TipoMovimiento.AJUSTE, m.getTipo());
        assertEquals(-2, m.getCantidad());
        assertEquals(8, martillo.getStock());
    }

    @Test
    void ajusteSinDiferenciaSeRechaza() {
        assertThrows(ReglaNegocioException.class, () -> new CreadorAjuste().registrar(martillo, 10, "Conteo", null));
    }

    @Test
    void entradaConCantidadCeroSeRechaza() {
        assertThrows(ReglaNegocioException.class, () -> new CreadorEntrada().registrar(martillo, 0, "Compra", guia));
    }
}
