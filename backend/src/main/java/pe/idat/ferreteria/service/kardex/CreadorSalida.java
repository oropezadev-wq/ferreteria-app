package pe.idat.ferreteria.service.kardex;

import org.springframework.stereotype.Component;

import pe.idat.ferreteria.model.MovimientoKardex;
import pe.idat.ferreteria.model.Producto;
import pe.idat.ferreteria.model.TipoMovimiento;

/** Sale mercadería (venta, merma o uso interno): el stock baja. */
@Component
public class CreadorSalida extends CreadorMovimiento {

    @Override
    public TipoMovimiento tipo() {
        return TipoMovimiento.SALIDA;
    }

    @Override
    protected MovimientoKardex crearMovimiento(Producto producto, int cantidad, String motivo) {
        exigirCantidadPositiva(cantidad);
        return new MovimientoKardex(producto, TipoMovimiento.SALIDA, -cantidad, producto.getStock(), motivo);
    }
}
