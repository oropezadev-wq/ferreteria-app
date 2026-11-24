package pe.idat.ferreteria.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Monto con que se abre el turno, o efectivo contado al cerrarlo. */
public record CajaRequest(
        @NotNull(message = "Ingrese el monto")
        @DecimalMin(value = "0.00", message = "El monto no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El monto admite hasta 2 decimales")
        BigDecimal monto,

        @Size(max = 200, message = "La observación admite hasta 200 caracteres")
        String observacion) {
}
