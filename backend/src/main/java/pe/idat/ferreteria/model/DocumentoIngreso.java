package pe.idat.ferreteria.model;

import java.time.LocalDate;

/**
 * Documento que respalda una entrada: permite saber con qué guía o comprobante, en qué fecha
 * y de qué proveedor llegó cada producto.
 */
public record DocumentoIngreso(TipoDocumento tipo, String numero, LocalDate fecha, String proveedor) {

    public boolean estaCompleto() {
        return tipo != null && numero != null && !numero.isBlank()
                && fecha != null && proveedor != null && !proveedor.isBlank();
    }
}
