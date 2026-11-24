package pe.idat.ferreteria.dto;

import java.math.BigDecimal;

import pe.idat.ferreteria.model.Producto;

/**
 * Lo que ve el público en el catálogo. No incluye el precio de compra ni el stock exacto:
 * solo si hay o no hay unidades.
 */
public record CatalogoResponse(String codigo, String descripcion, String categoria, BigDecimal precioVenta,
                               boolean disponible) {

    public static CatalogoResponse de(Producto p) {
        return new CatalogoResponse(p.getCodigo(), p.getDescripcion(), p.getCategoria().getNombre(),
                p.getPrecioVenta(), p.getStock() > 0);
    }
}
