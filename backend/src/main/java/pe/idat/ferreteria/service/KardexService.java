package pe.idat.ferreteria.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.idat.ferreteria.dto.MovimientoRequest;
import pe.idat.ferreteria.dto.MovimientoResponse;
import pe.idat.ferreteria.exception.ReglaNegocioException;
import pe.idat.ferreteria.model.MovimientoKardex;
import pe.idat.ferreteria.model.Producto;
import pe.idat.ferreteria.model.TipoMovimiento;
import pe.idat.ferreteria.repository.MovimientoKardexRepository;
import pe.idat.ferreteria.service.kardex.CreadorMovimiento;

@Service
public class KardexService {

    private final Map<TipoMovimiento, CreadorMovimiento> creadores = new EnumMap<>(TipoMovimiento.class);
    private final MovimientoKardexRepository movimientoRepository;
    private final ProductoService productoService;

    /** Spring entrega todas las subclases de CreadorMovimiento; se ordenan por el tipo que fabrican. */
    public KardexService(List<CreadorMovimiento> creadores, MovimientoKardexRepository movimientoRepository,
                         ProductoService productoService) {
        creadores.forEach(c -> this.creadores.put(c.tipo(), c));
        this.movimientoRepository = movimientoRepository;
        this.productoService = productoService;
    }

    /** El cambio de stock y su fila de kardex se guardan juntos o no se guarda ninguno. */
    @Transactional
    public MovimientoResponse registrar(MovimientoRequest request) {
        Producto producto = productoService.buscarEntidad(request.productoId());
        if (!producto.isActivo()) {
            throw new ReglaNegocioException("El producto " + producto.getCodigo() + " está desactivado");
        }
        CreadorMovimiento creador = creadores.get(request.tipo());
        MovimientoKardex movimiento = creador.registrar(producto, request.cantidad(), request.motivo().trim());
        return MovimientoResponse.de(movimientoRepository.save(movimiento));
    }

    @Transactional(readOnly = true)
    public List<MovimientoResponse> listar(Long productoId) {
        List<MovimientoKardex> movimientos = productoId == null
                ? movimientoRepository.findTop50ByOrderByFechaDescIdDesc()
                : movimientoRepository.findByProductoIdOrderByFechaDescIdDesc(productoId);
        return movimientos.stream().map(MovimientoResponse::de).toList();
    }
}
