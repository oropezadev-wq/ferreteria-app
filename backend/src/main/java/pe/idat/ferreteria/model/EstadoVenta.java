package pe.idat.ferreteria.model;

/** Las ventas no se borran: una venta equivocada se anula y devuelve su stock. */
public enum EstadoVenta {
    REGISTRADA,
    ANULADA
}
