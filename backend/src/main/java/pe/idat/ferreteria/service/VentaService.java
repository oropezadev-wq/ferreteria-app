package pe.idat.ferreteria.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.idat.ferreteria.dto.VentaRequest;
import pe.idat.ferreteria.dto.VentaResponse;
import pe.idat.ferreteria.exception.RecursoNoEncontradoException;
import pe.idat.ferreteria.exception.ReglaNegocioException;
import pe.idat.ferreteria.model.Cliente;
import pe.idat.ferreteria.model.DetalleVenta;
import pe.idat.ferreteria.model.Producto;
import pe.idat.ferreteria.model.TipoComprobante;
import pe.idat.ferreteria.model.TipoMovimiento;
import pe.idat.ferreteria.model.TurnoCaja;
import pe.idat.ferreteria.model.Usuario;
import pe.idat.ferreteria.model.Venta;
import pe.idat.ferreteria.repository.TurnoCajaRepository;
import pe.idat.ferreteria.repository.VentaRepository;

/**
 * Registra ventas con su boleta o factura y las anula. La venta, sus líneas y las salidas
 * del kardex se guardan en una sola transacción: si una regla falla, no se guarda nada.
 */
@Service
public class VentaService {

    /** SUNAT exige identificar al cliente en boletas que superan este monto. */
    static final BigDecimal TOPE_BOLETA_SIN_DNI = new BigDecimal("700");

    private final VentaRepository ventaRepository;
    private final TurnoCajaRepository turnoRepository;
    private final ProductoService productoService;
    private final KardexService kardexService;
    private final ClienteService clienteService;
    private final UsuarioService usuarioService;

    public VentaService(VentaRepository ventaRepository, TurnoCajaRepository turnoRepository,
                        ProductoService productoService, KardexService kardexService,
                        ClienteService clienteService, UsuarioService usuarioService) {
        this.ventaRepository = ventaRepository;
        this.turnoRepository = turnoRepository;
        this.productoService = productoService;
        this.kardexService = kardexService;
        this.clienteService = clienteService;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public VentaResponse registrar(VentaRequest request, String usuarioActual) {
        Usuario vendedor = usuarioService.buscarActivo(usuarioActual);
        String documento = limpiar(request.clienteDocumento());
        String nombre = limpiar(request.clienteNombre());
        validarCliente(request.tipoComprobante(), documento, nombre);

        Cliente cliente = documento == null ? null : clienteService.buscarOCrear(documento, nombre);
        TurnoCaja turno = turnoRepository.findByUsuarioUsuarioAndAbiertoTrue(usuarioActual).orElse(null);
        TipoComprobante tipo = request.tipoComprobante();
        int numero = ventaRepository.ultimoNumero(tipo.serie()) + 1;
        Venta venta = new Venta(tipo, numero, cliente, vendedor, turno, request.metodoPago());

        for (VentaRequest.Linea linea : request.lineas()) {
            Producto producto = productoService.buscarEntidad(linea.productoId());
            if (!producto.isActivo()) {
                throw new ReglaNegocioException("El producto " + producto.getCodigo() + " está desactivado");
            }
            venta.agregar(producto, linea.cantidad());
            kardexService.registrarPorVenta(producto, TipoMovimiento.SALIDA, linea.cantidad(),
                    "Venta " + venta.getComprobante());
        }
        if (tipo == TipoComprobante.BOLETA && documento == null
                && venta.getTotal().compareTo(TOPE_BOLETA_SIN_DNI) > 0) {
            throw new ReglaNegocioException("Para una boleta de más de S/ 700 se debe ingresar el DNI del cliente");
        }
        return VentaResponse.de(ventaRepository.save(venta));
    }

    /** Reglas del comprobante: factura con RUC y razón social; boleta con DNI opcional. */
    static void validarCliente(TipoComprobante tipo, String documento, String nombre) {
        if (tipo == TipoComprobante.FACTURA) {
            if (documento == null || !documento.matches("(10|20)\\d{9}")) {
                throw new ReglaNegocioException(
                        "La factura necesita un RUC válido de 11 dígitos (empieza con 10 o 20)");
            }
            if (nombre == null) {
                throw new ReglaNegocioException("La factura necesita la razón social del cliente");
            }
        } else if (documento != null && !documento.matches("\\d{8}")) {
            throw new ReglaNegocioException("En la boleta, el documento del cliente debe ser un DNI de 8 dígitos");
        }
    }

    /** La venta no se borra: queda anulada y cada producto vuelve al stock con una devolución en el kardex. */
    @Transactional
    public VentaResponse anular(Long id, String motivo) {
        Venta venta = buscarEntidad(id);
        if (venta.isAnulada()) {
            throw new ReglaNegocioException("La venta " + venta.getComprobante() + " ya está anulada");
        }
        for (DetalleVenta detalle : venta.getDetalles()) {
            kardexService.registrarPorVenta(detalle.getProducto(), TipoMovimiento.DEVOLUCION, detalle.getCantidad(),
                    "Anulación de " + venta.getComprobante());
        }
        venta.anular(motivo.trim());
        return VentaResponse.de(venta);
    }

    @Transactional(readOnly = true)
    public VentaResponse obtener(Long id) {
        return VentaResponse.de(buscarEntidad(id));
    }

    /** Ventas entre dos fechas, incluidas ambas, de la más reciente a la más antigua. */
    @Transactional(readOnly = true)
    public List<VentaResponse> listar(LocalDate desde, LocalDate hasta) {
        return buscarEntre(desde, hasta).stream().map(VentaResponse::de).toList();
    }

    List<Venta> buscarEntre(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new ReglaNegocioException("La fecha final no puede ser anterior a la inicial");
        }
        return ventaRepository.findByFechaGreaterThanEqualAndFechaLessThanOrderByFechaDesc(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
    }

    private Venta buscarEntidad(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la venta " + id));
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
