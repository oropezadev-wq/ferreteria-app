package pe.idat.ferreteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnulacionRequest(
        @NotBlank(message = "Indique el motivo de la anulación")
        @Size(max = 150, message = "El motivo admite hasta 150 caracteres")
        String motivo) {
}
