package pe.idat.ferreteria.dto;

import pe.idat.ferreteria.model.Rol;

/** El token se envía después en la cabecera Authorization: Bearer. */
public record LoginResponse(String token, String usuario, String nombre, Rol rol) {
}
