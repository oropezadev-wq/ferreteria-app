package pe.idat.ferreteria.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import pe.idat.ferreteria.model.MetodoPago;
import pe.idat.ferreteria.model.TipoComprobante;

/** El precio no viene de la vista: se toma del producto al registrar la venta. */
public record VentaRequest(
        @NotNull(message = "Elija boleta o factura")
        TipoComprobante tipoComprobante,

        @NotNull(message = "Elija el método de pago")
        MetodoPago metodoPago,

        @Size(max = 11, message = "El DNI o RUC admite hasta 11 dígitos")
        String clienteDocumento,

        @Size(max = 150, message = "El nombre o razón social admite hasta 150 caracteres")
        String clienteNombre,

        @NotEmpty(message = "Agregue al menos un producto")
        List<@Valid Linea> lineas) {

    public record Linea(
            @NotNull(message = "Falta el producto")
            Long productoId,

            @NotNull(message = "Falta la cantidad")
            @Positive(message = "La cantidad debe ser mayor que cero")
            Integer cantidad) {
    }
}
