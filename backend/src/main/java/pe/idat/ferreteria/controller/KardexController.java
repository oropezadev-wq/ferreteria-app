package pe.idat.ferreteria.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.idat.ferreteria.dto.MovimientoRequest;
import pe.idat.ferreteria.dto.MovimientoResponse;
import pe.idat.ferreteria.service.KardexService;

@RestController
@RequestMapping("/api/movimientos")
public class KardexController {

    private final KardexService kardexService;

    public KardexController(KardexService kardexService) {
        this.kardexService = kardexService;
    }

    @GetMapping
    public List<MovimientoResponse> listar(@RequestParam(required = false) Long productoId,
                                           @RequestParam(required = false) String documento) {
        return kardexService.listar(productoId, documento);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoResponse registrar(@Valid @RequestBody MovimientoRequest request) {
        return kardexService.registrar(request);
    }
}
