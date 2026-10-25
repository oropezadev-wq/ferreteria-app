package pe.idat.ferreteria.dto;

import java.util.List;

public record ErrorResponse(String mensaje, List<String> detalles) {

    public ErrorResponse(String mensaje) {
        this(mensaje, List.of());
    }
}
