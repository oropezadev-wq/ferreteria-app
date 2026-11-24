package pe.idat.ferreteria.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Estado del turno con las ventas cobradas por método de pago (sin contar las anuladas). */
public record TurnoCajaResponse(
        Long id,
        boolean abierto,
        BigDecimal montoApertura,
        BigDecimal ventasEfectivo,
        BigDecimal ventasTarjeta,
        BigDecimal ventasYape,
        BigDecimal montoEsperado,
        BigDecimal montoContado,
        BigDecimal diferencia,
        String observacion,
        LocalDateTime fechaApertura,
        LocalDateTime fechaCierre) {
}
