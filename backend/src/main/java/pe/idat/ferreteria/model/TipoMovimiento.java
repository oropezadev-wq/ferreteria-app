package pe.idat.ferreteria.model;

public enum TipoMovimiento {
    ENTRADA,
    SALIDA,
    AJUSTE,
    /** Mercadería que vuelve al stock porque se anuló la venta. */
    DEVOLUCION
}
