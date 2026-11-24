package pe.idat.ferreteria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.idat.ferreteria.dto.VentaRequest;
import pe.idat.ferreteria.dto.VentaResponse;
import pe.idat.ferreteria.exception.ReglaNegocioException;
import pe.idat.ferreteria.model.Categoria;
import pe.idat.ferreteria.model.EstadoVenta;
import pe.idat.ferreteria.model.MetodoPago;
import pe.idat.ferreteria.model.Producto;
import pe.idat.ferreteria.model.Rol;
import pe.idat.ferreteria.model.TipoComprobante;
import pe.idat.ferreteria.model.TipoMovimiento;
import pe.idat.ferreteria.model.Usuario;
import pe.idat.ferreteria.model.Venta;
import pe.idat.ferreteria.repository.TurnoCajaRepository;
import pe.idat.ferreteria.repository.VentaRepository;

/**
 * Pruebas unitarias de la lógica de ventas. Las dependencias (repositorios y otros servicios)
 * se reemplazan con objetos simulados de Mockito: no se usa Spring ni base de datos.
 */
class VentaServiceTest {

    private VentaRepository ventaRepository;
    private KardexService kardexService;
    private ProductoService productoService;
    private VentaService servicio;
    private Producto martillo;
    private final Usuario vendedor = new Usuario("vendedor", "Vendedor de turno", "hash", Rol.VENDEDOR);

    @BeforeEach
    void preparar() {
        ventaRepository = mock(VentaRepository.class);
        kardexService = mock(KardexService.class);
        productoService = mock(ProductoService.class);
        TurnoCajaRepository turnoRepository = mock(TurnoCajaRepository.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        servicio = new VentaService(ventaRepository, turnoRepository, productoService, kardexService,
                mock(ClienteService.class), usuarioService);

        martillo = new Producto("HM-001", "Martillo de uña 16 oz", new Categoria("Herramientas"),
                new BigDecimal("18.50"), new BigDecimal("29.90"), 5);
        martillo.setStock(24);
        when(productoService.buscarEntidad(1L)).thenReturn(martillo);
        when(usuarioService.buscarActivo("vendedor")).thenReturn(vendedor);
        when(turnoRepository.findByUsuarioUsuarioAndAbiertoTrue("vendedor")).thenReturn(Optional.empty());
        when(ventaRepository.save(any(Venta.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void boletaCalculaGravadaIgvYTotalConPreciosQueIncluyenIgv() {
        VentaResponse venta = servicio.registrar(boleta(null, 2), "vendedor");

        assertEquals(new BigDecimal("59.80"), venta.total());
        assertEquals(new BigDecimal("50.68"), venta.gravada());
        assertEquals(new BigDecimal("9.12"), venta.igv());
        assertEquals(EstadoVenta.REGISTRADA, venta.estado());
        assertEquals("PENDIENTE", venta.estadoSunat());
    }

    @Test
    void cadaLineaRegistraSuSalidaEnElKardex() {
        servicio.registrar(boleta(null, 3), "vendedor");

        verify(kardexService).registrarPorVenta(martillo, TipoMovimiento.SALIDA, 3, "Venta B001-1");
    }

    @Test
    void elNumeroDelComprobanteEsCorrelativoPorSerie() {
        when(ventaRepository.ultimoNumero("B001")).thenReturn(7);

        VentaResponse venta = servicio.registrar(boleta(null, 1), "vendedor");

        assertEquals("B001-8", venta.comprobante());
    }

    @Test
    void boletaMayorA700SinDniSeRechaza() {
        martillo.setPrecioVenta(new BigDecimal("219.00"));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(boleta(null, 4), "vendedor"));

        assertEquals("Para una boleta de más de S/ 700 se debe ingresar el DNI del cliente", ex.getMessage());
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void productoDesactivadoNoSeVende() {
        martillo.setActivo(false);

        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(boleta(null, 1), "vendedor"));
        verify(kardexService, never()).registrarPorVenta(any(), any(), anyInt(), anyString());
    }

    @Test
    void facturaSinRucValidoSeRechaza() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> VentaService.validarCliente(TipoComprobante.FACTURA, "45781236", "Juan Pérez"));

        assertEquals("La factura necesita un RUC válido de 11 dígitos (empieza con 10 o 20)", ex.getMessage());
    }

    @Test
    void facturaSinRazonSocialSeRechaza() {
        assertThrows(ReglaNegocioException.class,
                () -> VentaService.validarCliente(TipoComprobante.FACTURA, "20512345678", null));
    }

    @Test
    void boletaConDocumentoQueNoEsDniSeRechaza() {
        assertThrows(ReglaNegocioException.class,
                () -> VentaService.validarCliente(TipoComprobante.BOLETA, "123", null));
    }

    @Test
    void anularMarcaLaVentaYDevuelveCadaProductoAlStock() {
        Venta venta = new Venta(TipoComprobante.BOLETA, 1, null, vendedor, null, MetodoPago.EFECTIVO);
        venta.agregar(martillo, 2);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));

        VentaResponse anulada = servicio.anular(5L, "Error en la cantidad");

        assertEquals(EstadoVenta.ANULADA, anulada.estado());
        assertEquals("Error en la cantidad", anulada.motivoAnulacion());
        verify(kardexService).registrarPorVenta(martillo, TipoMovimiento.DEVOLUCION, 2, "Anulación de B001-1");
    }

    @Test
    void unaVentaAnuladaNoSePuedeAnularOtraVez() {
        Venta venta = new Venta(TipoComprobante.BOLETA, 1, null, vendedor, null, MetodoPago.EFECTIVO);
        venta.anular("Prueba");
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));

        assertThrows(ReglaNegocioException.class, () -> servicio.anular(5L, "Otra vez"));
        verify(kardexService, never()).registrarPorVenta(any(), eq(TipoMovimiento.DEVOLUCION), anyInt(),
                anyString());
    }

    @Test
    void boletaSinDocumentoNoTieneCliente() {
        VentaResponse venta = servicio.registrar(boleta(null, 1), "vendedor");

        assertNull(venta.clienteDocumento());
    }

    private static VentaRequest boleta(String dni, int cantidad) {
        return new VentaRequest(TipoComprobante.BOLETA, MetodoPago.EFECTIVO, dni, null,
                List.of(new VentaRequest.Linea(1L, cantidad)));
    }
}
