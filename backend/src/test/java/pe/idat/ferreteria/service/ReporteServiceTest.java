package pe.idat.ferreteria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.idat.ferreteria.dto.ReporteVentasResponse;
import pe.idat.ferreteria.model.Categoria;
import pe.idat.ferreteria.model.MetodoPago;
import pe.idat.ferreteria.model.Producto;
import pe.idat.ferreteria.model.Rol;
import pe.idat.ferreteria.model.TipoComprobante;
import pe.idat.ferreteria.model.Usuario;
import pe.idat.ferreteria.model.Venta;
import pe.idat.ferreteria.repository.ProductoRepository;

/** Pruebas unitarias del reporte de ventas con las ventas simuladas. */
class ReporteServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 23);

    private final Usuario vendedor = new Usuario("vendedor", "Vendedor", "hash", Rol.VENDEDOR);
    private final Producto martillo = new Producto("HM-001", "Martillo", new Categoria("Herramientas"),
            new BigDecimal("18.50"), new BigDecimal("29.90"), 5);
    private final Producto teflon = new Producto("GA-002", "Cinta teflón", new Categoria("Gasfitería"),
            new BigDecimal("0.80"), new BigDecimal("1.50"), 30);
    private VentaService ventaService;
    private ReporteService servicio;

    @BeforeEach
    void preparar() {
        ventaService = mock(VentaService.class);
        ProductoRepository productoRepository = mock(ProductoRepository.class);
        servicio = new ReporteService(ventaService, productoRepository);
        when(productoRepository.findByActivoTrue()).thenReturn(List.of(martillo, teflon));
    }

    @Test
    void lasVentasAnuladasNoSumanEnElTotalNiEnLaUtilidad() {
        Venta anulada = venta(martillo, 5);
        anulada.anular("Error");
        when(ventaService.buscarEntre(HOY, HOY)).thenReturn(List.of(venta(martillo, 2), anulada));

        ReporteVentasResponse r = servicio.ventas(HOY, HOY);

        assertEquals(1, r.cantidadVentas());
        assertEquals(1, r.cantidadAnuladas());
        assertEquals(new BigDecimal("59.80"), r.total());
        assertEquals(new BigDecimal("37.00"), r.costo());
        assertEquals(new BigDecimal("22.80"), r.utilidad());
    }

    @Test
    void laRotacionOrdenaPorUnidadesVendidasEIncluyeLoQueNoSeVendio() {
        when(ventaService.buscarEntre(HOY, HOY)).thenReturn(List.of(venta(martillo, 3)));

        ReporteVentasResponse r = servicio.ventas(HOY, HOY);

        assertEquals("Martillo", r.mayorRotacion().get(0).producto());
        assertEquals(1, r.mayorRotacion().size());
        assertEquals("Cinta teflón", r.menorRotacion().get(0).producto());
        assertEquals(0, r.menorRotacion().get(0).unidades());
    }

    private Venta venta(Producto producto, int cantidad) {
        Venta venta = new Venta(TipoComprobante.BOLETA, 1, null, vendedor, null, MetodoPago.EFECTIVO);
        venta.agregar(producto, cantidad);
        return venta;
    }
}
