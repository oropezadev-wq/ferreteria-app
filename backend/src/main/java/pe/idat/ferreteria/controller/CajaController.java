package pe.idat.ferreteria.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.idat.ferreteria.dto.CajaRequest;
import pe.idat.ferreteria.dto.TurnoCajaResponse;
import pe.idat.ferreteria.service.CajaService;

/** Cada vendedor maneja solo su propio turno de caja. */
@RestController
@RequestMapping("/api/caja")
public class CajaController {

    private final CajaService cajaService;

    public CajaController(CajaService cajaService) {
        this.cajaService = cajaService;
    }

    /** 204 si el vendedor no tiene un turno abierto. */
    @GetMapping("/actual")
    public ResponseEntity<TurnoCajaResponse> actual(Authentication auth) {
        return cajaService.actual(auth.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/abrir")
    @ResponseStatus(HttpStatus.CREATED)
    public TurnoCajaResponse abrir(@Valid @RequestBody CajaRequest request, Authentication auth) {
        return cajaService.abrir(auth.getName(), request);
    }

    @PostMapping("/cerrar")
    public TurnoCajaResponse cerrar(@Valid @RequestBody CajaRequest request, Authentication auth) {
        return cajaService.cerrar(auth.getName(), request);
    }
}
