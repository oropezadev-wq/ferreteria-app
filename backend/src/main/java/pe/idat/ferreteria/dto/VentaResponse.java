package pe.idat.ferreteria.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import pe.idat.ferreteria.model.Cliente;
import pe.idat.ferreteria.model.DetalleVenta;
import pe.idat.ferreteria.model.EstadoVenta;
import pe.idat.ferreteria.model.MetodoPago;
import pe.idat.ferreteria.model.TipoComprobante;
import pe.idat.ferreteria.model.Venta;

public record VentaResponse(
        Long id,
        String comprobante,
        TipoComprobante tipoComprobante,
        LocalDateTime fecha,
        String clienteDocumento,
        String clienteNombre,
        String vendedor,
        MetodoPago metodoPago,
        BigDecimal gravada,
        BigDecimal igv,
        BigDecimal total,
        EstadoVenta estado,
        String estadoSunat,
        String motivoAnulacion,
        List<Detalle> detalles) {

    public record Detalle(String codigo, String descripcion, int cantidad, BigDecimal precioUnitario,
                          BigDecimal subtotal) {

        static Detalle de(DetalleVenta d) {
            return new Detalle(d.getProducto().getCodigo(), d.getProducto().getDescripcion(), d.getCantidad(),
                    d.getPrecioUnitario(), d.getSubtotal());
        }
    }

    public static VentaResponse de(Venta v) {
        Cliente c = v.getCliente();
        return new VentaResponse(v.getId(), v.getComprobante(), v.getTipoComprobante(), v.getFecha(),
                c == null ? null : c.getNumeroDocumento(), c == null ? null : c.getNombre(),
                v.getVendedor().getNombre(), v.getMetodoPago(), v.getGravada(), v.getIgv(), v.getTotal(),
                v.getEstado(), v.getEstadoSunat(), v.getMotivoAnulacion(),
                v.getDetalles().stream().map(Detalle::de).toList());
    }
}
