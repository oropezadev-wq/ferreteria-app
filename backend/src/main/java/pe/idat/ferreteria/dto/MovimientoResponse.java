package pe.idat.ferreteria.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.idat.ferreteria.model.MovimientoKardex;
import pe.idat.ferreteria.model.TipoDocumento;
import pe.idat.ferreteria.model.TipoMovimiento;

public record MovimientoResponse(
        Long id,
        Long productoId,
        String productoCodigo,
        String productoDescripcion,
        TipoMovimiento tipo,
        int cantidad,
        int stockAnterior,
        int stockResultante,
        String motivo,
        LocalDateTime fecha,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        LocalDate fechaDocumento,
        String proveedor) {

    public static MovimientoResponse de(MovimientoKardex m) {
        return new MovimientoResponse(m.getId(), m.getProducto().getId(),
                m.getProducto().getCodigo(), m.getProducto().getDescripcion(),
                m.getTipo(), m.getCantidad(), m.getStockAnterior(), m.getStockResultante(),
                m.getMotivo(), m.getFecha(),
                m.getTipoDocumento(), m.getNumeroDocumento(), m.getFechaDocumento(), m.getProveedor());
    }
}
