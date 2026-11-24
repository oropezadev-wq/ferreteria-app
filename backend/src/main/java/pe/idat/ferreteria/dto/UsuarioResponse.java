package pe.idat.ferreteria.dto;

import pe.idat.ferreteria.model.Rol;
import pe.idat.ferreteria.model.Usuario;

/** Nunca incluye la contraseña ni su hash. */
public record UsuarioResponse(Long id, String usuario, String nombre, Rol rol, boolean activo) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getUsuario(), u.getNombre(), u.getRol(), u.isActivo());
    }
}
