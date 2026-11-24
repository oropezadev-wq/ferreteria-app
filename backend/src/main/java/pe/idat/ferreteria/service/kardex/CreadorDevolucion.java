package pe.idat.ferreteria.service.kardex;

import org.springframework.stereotype.Component;

import pe.idat.ferreteria.model.MovimientoKardex;
import pe.idat.ferreteria.model.Producto;
import pe.idat.ferreteria.model.TipoMovimiento;

/**
 * Vuelve mercadería al stock al anular una venta. Es el tipo nuevo que prevé el Factory Method:
 * se agregó esta subclase sin cambiar las demás.
 */
@Component
public class CreadorDevolucion extends CreadorMovimiento {

    @Override
    public TipoMovimiento tipo() {
        return TipoMovimiento.DEVOLUCION;
    }

    @Override
    protected MovimientoKardex crearMovimiento(Producto producto, int cantidad, String motivo) {
        exigirCantidadPositiva(cantidad);
        return new MovimientoKardex(producto, TipoMovimiento.DEVOLUCION, cantidad, producto.getStock(), motivo);
    }
}
