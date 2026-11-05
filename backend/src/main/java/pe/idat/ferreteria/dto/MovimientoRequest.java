package pe.idat.ferreteria.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import pe.idat.ferreteria.model.TipoDocumento;
import pe.idat.ferreteria.model.TipoMovimiento;

/**
 * En ENTRADA y SALIDA, cantidad son las unidades que entran o salen.
 * En AJUSTE, cantidad es el stock contado físicamente.
 * Los datos del documento del proveedor son obligatorios solo en ENTRADA.
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
        String motivo,

        TipoDocumento tipoDocumento,

        @Pattern(regexp = "[A-Za-z0-9]{1,4}-\\d{1,8}",
                message = "El número de documento va como SERIE-NÚMERO, por ejemplo T001-000123")
        String numeroDocumento,

        @PastOrPresent(message = "La fecha del documento no puede ser futura")
        LocalDate fechaDocumento,

        @Size(max = 120, message = "El proveedor admite hasta 120 caracteres")
        String proveedor) {
}
