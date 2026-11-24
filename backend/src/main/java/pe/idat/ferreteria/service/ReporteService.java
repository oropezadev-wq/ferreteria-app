package pe.idat.ferreteria.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.idat.ferreteria.dto.ReporteVentasResponse;
import pe.idat.ferreteria.dto.ReporteVentasResponse.Rotacion;
import pe.idat.ferreteria.dto.ReporteVentasResponse.VentaDia;
import pe.idat.ferreteria.model.DetalleVenta;
import pe.idat.ferreteria.model.Venta;
import pe.idat.ferreteria.repository.ProductoRepository;

/** Reporte de ventas, utilidad y rotación de productos (RF7). */
@Service
public class ReporteService {

    private static final int TOP = 5;

    private final VentaService ventaService;
    private final ProductoRepository productoRepository;

    public ReporteService(VentaService ventaService, ProductoRepository productoRepository) {
        this.ventaService = ventaService;
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public ReporteVentasResponse ventas(LocalDate desde, LocalDate hasta) {
        List<Venta> todas = ventaService.buscarEntre(desde, hasta);
        List<Venta> validas = todas.stream().filter(v -> !v.isAnulada()).toList();

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal costo = BigDecimal.ZERO;
        Map<LocalDate, BigDecimal> porDia = new TreeMap<>();
        Map<String, Integer> unidades = new HashMap<>();
        for (Venta venta : validas) {
            total = total.add(venta.getTotal());
            porDia.merge(venta.getFecha().toLocalDate(), venta.getTotal(), BigDecimal::add);
            for (DetalleVenta d : venta.getDetalles()) {
                costo = costo.add(d.getCostoUnitario().multiply(BigDecimal.valueOf(d.getCantidad())));
                unidades.merge(d.getProducto().getCodigo(), d.getCantidad(), Integer::sum);
            }
        }

        // Todos los productos activos entran a la rotación, también los que no se vendieron.
        // Se agrupa por código, que es único y siempre existe.
        List<Rotacion> rotacion = productoRepository.findByActivoTrue().stream()
                .map(p -> new Rotacion(p.getDescripcion(), unidades.getOrDefault(p.getCodigo(), 0)))
                .sorted(Comparator.comparingInt(Rotacion::unidades).reversed()
                        .thenComparing(Rotacion::producto))
                .toList();
        List<Rotacion> mayor = rotacion.stream().filter(r -> r.unidades() > 0).limit(TOP).toList();
        List<Rotacion> menor = rotacion.reversed().stream().limit(TOP).toList();

        return new ReporteVentasResponse(desde, hasta, validas.size(), todas.size() - validas.size(), total, costo,
                total.subtract(costo),
                porDia.entrySet().stream().map(e -> new VentaDia(e.getKey(), e.getValue())).toList(),
                mayor, menor);
    }

    /** Usado por el panel: total vendido hoy sin anuladas. */
    @Transactional(readOnly = true)
    public BigDecimal totalDelDia(LocalDate dia) {
        return ventaService.buscarEntre(dia, dia).stream()
                .filter(v -> !v.isAnulada())
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
