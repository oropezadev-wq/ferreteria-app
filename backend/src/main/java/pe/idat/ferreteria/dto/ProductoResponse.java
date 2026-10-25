package pe.idat.ferreteria.dto;

import java.math.BigDecimal;

import pe.idat.ferreteria.model.Producto;

public record ProductoResponse(
        Long id,
        String codigo,
        String descripcion,
        Long categoriaId,
        String categoriaNombre,
        BigDecimal precioCompra,
        BigDecimal precioVenta,
        int stock,
        int stockMinimo,
        boolean stockBajo,
        boolean activo) {

    public static ProductoResponse de(Producto p) {
        return new ProductoResponse(p.getId(), p.getCodigo(), p.getDescripcion(),
                p.getCategoria().getId(), p.getCategoria().getNombre(),
                p.getPrecioCompra(), p.getPrecioVenta(), p.getStock(), p.getStockMinimo(),
                p.isStockBajo(), p.isActivo());
    }
}
