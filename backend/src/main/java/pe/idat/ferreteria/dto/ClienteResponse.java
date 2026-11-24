package pe.idat.ferreteria.dto;

import pe.idat.ferreteria.model.Cliente;
import pe.idat.ferreteria.model.TipoDocumentoCliente;

public record ClienteResponse(Long id, TipoDocumentoCliente tipoDocumento, String numeroDocumento, String nombre,
                              String telefono) {

    public static ClienteResponse de(Cliente c) {
        return new ClienteResponse(c.getId(), c.getTipoDocumento(), c.getNumeroDocumento(), c.getNombre(),
                c.getTelefono());
    }
}
