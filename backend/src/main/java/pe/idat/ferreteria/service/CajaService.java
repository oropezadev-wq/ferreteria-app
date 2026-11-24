package pe.idat.ferreteria.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.idat.ferreteria.dto.CajaRequest;
import pe.idat.ferreteria.dto.TurnoCajaResponse;
import pe.idat.ferreteria.exception.ReglaNegocioException;
import pe.idat.ferreteria.model.MetodoPago;
import pe.idat.ferreteria.model.TurnoCaja;
import pe.idat.ferreteria.model.Venta;
import pe.idat.ferreteria.repository.TurnoCajaRepository;
import pe.idat.ferreteria.repository.VentaRepository;

/** Apertura y cierre del turno de caja de cada vendedor (CU11). */
@Service
public class CajaService {

    private final TurnoCajaRepository turnoRepository;
    private final VentaRepository ventaRepository;
    private final UsuarioService usuarioService;

    public CajaService(TurnoCajaRepository turnoRepository, VentaRepository ventaRepository,
                       UsuarioService usuarioService) {
        this.turnoRepository = turnoRepository;
        this.ventaRepository = ventaRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional(readOnly = true)
    public Optional<TurnoCajaResponse> actual(String usuario) {
        return turnoRepository.findByUsuarioUsuarioAndAbiertoTrue(usuario).map(this::aRespuesta);
    }

    @Transactional
    public TurnoCajaResponse abrir(String usuario, CajaRequest request) {
        if (turnoRepository.findByUsuarioUsuarioAndAbiertoTrue(usuario).isPresent()) {
            throw new ReglaNegocioException("Ya tiene un turno de caja abierto");
        }
        TurnoCaja turno = new TurnoCaja(usuarioService.buscarActivo(usuario), request.monto());
        return aRespuesta(turnoRepository.save(turno));
    }

    /** Esperado = monto de apertura + ventas en efectivo del turno; la diferencia es sobrante o faltante. */
    @Transactional
    public TurnoCajaResponse cerrar(String usuario, CajaRequest request) {
        TurnoCaja turno = turnoRepository.findByUsuarioUsuarioAndAbiertoTrue(usuario)
                .orElseThrow(() -> new ReglaNegocioException("No tiene un turno de caja abierto"));
        List<Venta> ventas = ventaRepository.findByTurnoId(turno.getId());
        BigDecimal esperado = turno.getMontoApertura().add(sumar(ventas, MetodoPago.EFECTIVO));
        String observacion = request.observacion() == null || request.observacion().isBlank()
                ? null : request.observacion().trim();
        turno.cerrar(esperado, request.monto(), observacion);
        return aRespuesta(turno);
    }

    private TurnoCajaResponse aRespuesta(TurnoCaja turno) {
        List<Venta> ventas = turno.getId() == null ? List.of() : ventaRepository.findByTurnoId(turno.getId());
        BigDecimal efectivo = sumar(ventas, MetodoPago.EFECTIVO);
        BigDecimal esperado = turno.isAbierto() ? turno.getMontoApertura().add(efectivo) : turno.getMontoEsperado();
        return new TurnoCajaResponse(turno.getId(), turno.isAbierto(), turno.getMontoApertura(), efectivo,
                sumar(ventas, MetodoPago.TARJETA), sumar(ventas, MetodoPago.YAPE), esperado,
                turno.getMontoContado(), turno.getDiferencia(), turno.getObservacion(), turno.getFechaApertura(),
                turno.getFechaCierre());
    }

    /** Las ventas anuladas no suman a la caja. */
    private static BigDecimal sumar(List<Venta> ventas, MetodoPago metodo) {
        return ventas.stream()
                .filter(v -> !v.isAnulada() && v.getMetodoPago() == metodo)
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
