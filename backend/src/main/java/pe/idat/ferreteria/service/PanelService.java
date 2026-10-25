package pe.idat.ferreteria.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.idat.ferreteria.dto.ResumenResponse;
import pe.idat.ferreteria.model.Producto;
import pe.idat.ferreteria.repository.MovimientoKardexRepository;
import pe.idat.ferreteria.repository.ProductoRepository;

@Service
public class PanelService {

    private final ProductoRepository productoRepository;
    private final MovimientoKardexRepository movimientoRepository;

    public PanelService(ProductoRepository productoRepository, MovimientoKardexRepository movimientoRepository) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    @Transactional(readOnly = true)
    public ResumenResponse resumen() {
        List<Producto> activos = productoRepository.findByActivoTrue();
        long stockBajo = activos.stream().filter(Producto::isStockBajo).count();
        // Valor del inventario a precio de compra.
        BigDecimal valor = activos.stream()
                .map(p -> p.getPrecioCompra().multiply(BigDecimal.valueOf(p.getStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long movimientosHoy = movimientoRepository.countByFechaAfter(LocalDate.now().atStartOfDay());
        return new ResumenResponse(activos.size(), stockBajo, valor, movimientosHoy);
    }
}
