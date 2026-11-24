package pe.idat.ferreteria.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Ventas del periodo. Las anuladas se cuentan aparte y no suman en los montos. */
public record ReporteVentasResponse(
        LocalDate desde,
        LocalDate hasta,
        int cantidadVentas,
        int cantidadAnuladas,
        BigDecimal total,
        BigDecimal costo,
        BigDecimal utilidad,
        List<VentaDia> ventasPorDia,
        List<Rotacion> mayorRotacion,
        List<Rotacion> menorRotacion) {

    public record VentaDia(LocalDate fecha, BigDecimal total) {
    }

    public record Rotacion(String producto, int unidades) {
    }
}
