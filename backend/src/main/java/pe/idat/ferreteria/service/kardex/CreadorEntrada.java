package pe.idat.ferreteria.service.kardex;

import org.springframework.stereotype.Component;

import pe.idat.ferreteria.model.MovimientoKardex;
import pe.idat.ferreteria.model.Producto;
import pe.idat.ferreteria.model.TipoMovimiento;

/** Llega mercadería del proveedor: el stock sube y queda el documento con el que ingresó. */
@Component
public class CreadorEntrada extends CreadorMovimiento {

    @Override
    public TipoMovimiento tipo() {
        return TipoMovimiento.ENTRADA;
    }

    @Override
    protected boolean exigeDocumento() {
        return true;
    }

    @Override
    protected MovimientoKardex crearMovimiento(Producto producto, int cantidad, String motivo) {
        exigirCantidadPositiva(cantidad);
        return new MovimientoKardex(producto, TipoMovimiento.ENTRADA, cantidad, producto.getStock(), motivo);
    }
}
