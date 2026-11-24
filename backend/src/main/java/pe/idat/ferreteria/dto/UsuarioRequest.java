package pe.idat.ferreteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.idat.ferreteria.model.Rol;

public record UsuarioRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Pattern(regexp = "^[a-z0-9._]{3,30}$",
                message = "El usuario debe tener de 3 a 30 letras minúsculas, números, punto o guion bajo")
        String usuario,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre admite hasta 80 caracteres")
        String nombre,

        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
                message = "La contraseña debe tener al menos 8 caracteres, con letras y números")
        String clave,

        @NotNull(message = "El rol es obligatorio")
        Rol rol) {
}
