package pe.idat.ferreteria.service.kardex;

import org.springframework.stereotype.Component;

import pe.idat.ferreteria.exception.ReglaNegocioException;
import pe.idat.ferreteria.model.MovimientoKardex;
import pe.idat.ferreteria.model.Producto;
import pe.idat.ferreteria.model.TipoMovimiento;

/** Inventario físico: la cantidad recibida es lo contado y el movimiento guarda la diferencia. */
@Component
public class CreadorAjuste extends CreadorMovimiento {

    @Override
    public TipoMovimiento tipo() {
        return TipoMovimiento.AJUSTE;
    }

    @Override
    protected MovimientoKardex crearMovimiento(Producto producto, int stockContado, String motivo) {
        int diferencia = stockContado - producto.getStock();
        if (diferencia == 0) {
            throw new ReglaNegocioException("El stock contado es igual al registrado; no hay nada que ajustar");
        }
        return new MovimientoKardex(producto, TipoMovimiento.AJUSTE, diferencia, producto.getStock(), motivo);
    }
}
