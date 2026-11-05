package pe.idat.ferreteria.service.kardex;

import pe.idat.ferreteria.exception.ReglaNegocioException;
import pe.idat.ferreteria.model.DocumentoIngreso;
import pe.idat.ferreteria.model.MovimientoKardex;
import pe.idat.ferreteria.model.Producto;
import pe.idat.ferreteria.model.TipoMovimiento;

/**
 * Patrón creacional Factory Method.
 *
 * El creador define los pasos comunes para registrar un movimiento (crearlo, validar que el stock
 * no quede negativo y actualizar el producto) y deja a cada subclase decidir cómo se crea su
 * movimiento: una entrada suma, una salida resta y un ajuste iguala el stock al conteo físico.
 * Para agregar un tipo nuevo (por ejemplo, devolución) se agrega una subclase, sin tocar las demás.
 */
public abstract class CreadorMovimiento {

    /** Tipo de movimiento que fabrica esta subclase. */
    public abstract TipoMovimiento tipo();

    /** El método fábrica: cada subclase crea su propio movimiento. */
    protected abstract MovimientoKardex crearMovimiento(Producto producto, int cantidad, String motivo);

    /** Si el movimiento debe llevar el documento del proveedor. Por defecto, no. */
    protected boolean exigeDocumento() {
        return false;
    }

    public MovimientoKardex registrar(Producto producto, int cantidad, String motivo, DocumentoIngreso documento) {
        if (exigeDocumento() && (documento == null || !documento.estaCompleto())) {
            throw new ReglaNegocioException("La entrada necesita el documento del proveedor: "
                    + "tipo, número, fecha y proveedor");
        }
        MovimientoKardex movimiento = crearMovimiento(producto, cantidad, motivo);
        if (movimiento.getStockResultante() < 0) {
            throw new ReglaNegocioException("Stock insuficiente: hay " + producto.getStock()
                    + " unidades de " + producto.getDescripcion());
        }
        if (exigeDocumento()) {
            movimiento.asignarDocumento(documento);
        }
        producto.setStock(movimiento.getStockResultante());
        return movimiento;
    }

    protected static void exigirCantidadPositiva(int cantidad) {
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad debe ser mayor que cero");
        }
    }
}
