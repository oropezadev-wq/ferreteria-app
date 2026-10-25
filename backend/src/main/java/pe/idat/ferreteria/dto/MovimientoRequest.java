package pe.idat.ferreteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import pe.idat.ferreteria.model.TipoMovimiento;

/**
 * En ENTRADA y SALIDA, cantidad son las unidades que entran o salen.
 * En AJUSTE, cantidad es el stock contado físicamente.
 */
public record MovimientoRequest(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "El tipo de movimiento es obligatorio")
        TipoMovimiento tipo,

        @NotNull(message = "La cantidad es obligatoria")
        @PositiveOrZero(message = "La cantidad no puede ser negativa")
        Integer cantidad,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 150, message = "El motivo admite hasta 150 caracteres")
        String motivo) {
}
