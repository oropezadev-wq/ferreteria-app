package pe.idat.ferreteria.model;

/** Cada comprobante tiene su propia serie y numeración correlativa. */
public enum TipoComprobante {
    BOLETA("B001"),
    FACTURA("F001");

    private final String serie;

    TipoComprobante(String serie) {
        this.serie = serie;
    }

    public String serie() {
        return serie;
    }
}
