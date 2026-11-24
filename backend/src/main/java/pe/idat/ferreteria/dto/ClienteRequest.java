package pe.idat.ferreteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank(message = "El DNI o RUC es obligatorio")
        @Pattern(regexp = "^(\\d{8}|\\d{11})$", message = "Ingrese un DNI de 8 dígitos o un RUC de 11 dígitos")
        String numeroDocumento,

        @NotBlank(message = "El nombre o razón social es obligatorio")
        @Size(max = 150, message = "El nombre admite hasta 150 caracteres")
        String nombre,

        @Size(max = 20, message = "El teléfono admite hasta 20 caracteres")
        String telefono) {
}
