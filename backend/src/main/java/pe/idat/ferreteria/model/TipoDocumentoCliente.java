package pe.idat.ferreteria.model;

public enum TipoDocumentoCliente {
    DNI,
    RUC;

    /** 11 dígitos es RUC; 8 dígitos es DNI. */
    public static TipoDocumentoCliente segunNumero(String numero) {
        return numero.length() == 11 ? RUC : DNI;
    }
}
