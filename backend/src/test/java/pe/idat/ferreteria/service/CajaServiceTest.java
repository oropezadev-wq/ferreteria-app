package pe.idat.ferreteria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.idat.ferreteria.dto.CajaRequest;
import pe.idat.ferreteria.exception.ReglaNegocioException;
import pe.idat.ferreteria.model.Categoria;
import pe.idat.ferreteria.model.MetodoPago;
import pe.idat.ferreteria.model.Producto;
import pe.idat.ferreteria.model.Rol;
import pe.idat.ferreteria.model.TipoComprobante;
import pe.idat.ferreteria.model.TurnoCaja;
import pe.idat.ferreteria.model.Usuario;
import pe.idat.ferreteria.model.Venta;
import pe.idat.ferreteria.repository.TurnoCajaRepository;
import pe.idat.ferreteria.repository.VentaRepository;

/** Pruebas unitarias del cierre de caja con repositorios simulados. */
class CajaServiceTest {

    private final Usuario vendedor = new Usuario("vendedor", "Vendedor de turno", "hash", Rol.VENDEDOR);
    private TurnoCajaRepository turnoRepository;
    private VentaRepository ventaRepository;
    private CajaService servicio;

    @BeforeEach
    void preparar() {
        turnoRepository = mock(TurnoCajaRepository.class);
        ventaRepository = mock(VentaRepository.class);
        servicio = new CajaService(turnoRepository, ventaRepository, mock(UsuarioService.class));
    }

    @Test
    void elCierreComparaElEfectivoContadoConElEsperadoSinContarAnuladas() {
        TurnoCaja turno = new TurnoCaja(vendedor, new BigDecimal("200.00"));
        when(turnoRepository.findByUsuarioUsuarioAndAbiertoTrue("vendedor")).thenReturn(Optional.of(turno));
        Venta anulada = venta(MetodoPago.EFECTIVO, 2);
        anulada.anular("Error");
        when(ventaRepository.findByTurnoId(any())).thenReturn(List.of(
                venta(MetodoPago.EFECTIVO, 2), venta(MetodoPago.TARJETA, 1), anulada));

        servicio.cerrar("vendedor", new CajaRequest(new BigDecimal("255.00"), "Billete roto"));

        assertFalse(turno.isAbierto());
        assertEquals(new BigDecimal("259.80"), turno.getMontoEsperado());
        assertEquals(new BigDecimal("-4.80"), turno.getDiferencia());
    }

    @Test
    void noSePuedeCerrarSinUnTurnoAbierto() {
        when(turnoRepository.findByUsuarioUsuarioAndAbiertoTrue("vendedor")).thenReturn(Optional.empty());

        assertThrows(ReglaNegocioException.class,
                () -> servicio.cerrar("vendedor", new CajaRequest(BigDecimal.TEN, null)));
    }

    @Test
    void noSeAbreUnSegundoTurno() {
        when(turnoRepository.findByUsuarioUsuarioAndAbiertoTrue("vendedor"))
                .thenReturn(Optional.of(new TurnoCaja(vendedor, BigDecimal.TEN)));

        assertThrows(ReglaNegocioException.class,
                () -> servicio.abrir("vendedor", new CajaRequest(BigDecimal.TEN, null)));
    }

    /** Venta de martillos a S/ 29.90 cada uno. */
    private Venta venta(MetodoPago metodo, int cantidad) {
        Producto martillo = new Producto("HM-001", "Martillo", new Categoria("Herramientas"),
                new BigDecimal("18.50"), new BigDecimal("29.90"), 5);
        Venta venta = new Venta(TipoComprobante.BOLETA, 1, null, vendedor, null, metodo);
        venta.agregar(martillo, cantidad);
        return venta;
    }
}
