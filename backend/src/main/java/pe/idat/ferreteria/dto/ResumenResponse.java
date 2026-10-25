package pe.idat.ferreteria.dto;

import java.math.BigDecimal;

public record ResumenResponse(
        long productosActivos,
        long productosStockBajo,
        BigDecimal valorInventario,
        long movimientosHoy) {
}
