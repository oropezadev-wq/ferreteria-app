package pe.idat.ferreteria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.idat.ferreteria.dto.ClienteRequest;
import pe.idat.ferreteria.dto.ClienteResponse;
import pe.idat.ferreteria.exception.ReglaNegocioException;
import pe.idat.ferreteria.model.Cliente;
import pe.idat.ferreteria.model.TipoDocumentoCliente;
import pe.idat.ferreteria.repository.ClienteRepository;

/** Pruebas unitarias de clientes con el repositorio simulado. */
class ClienteServiceTest {

    private ClienteRepository repositorio;
    private ClienteService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(ClienteRepository.class);
        servicio = new ClienteService(repositorio);
        when(repositorio.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void elTipoDeDocumentoSaleDeLaCantidadDeDigitos() {
        ClienteResponse dni = servicio.crear(new ClienteRequest("45781236", "Juan Pérez", null));
        ClienteResponse ruc = servicio.crear(new ClienteRequest("20512345678", "Constructora Ejemplo SAC", null));

        assertEquals(TipoDocumentoCliente.DNI, dni.tipoDocumento());
        assertEquals(TipoDocumentoCliente.RUC, ruc.tipoDocumento());
    }

    @Test
    void noSeRegistraDosVecesElMismoDocumento() {
        when(repositorio.findByNumeroDocumento("45781236"))
                .thenReturn(Optional.of(new Cliente("45781236", "Juan Pérez", null)));

        assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(new ClienteRequest("45781236", "Otro nombre", null)));
    }

    @Test
    void elDocumentoDeUnClienteNoSeCambia() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(new Cliente("45781236", "Juan Pérez", null)));

        assertThrows(ReglaNegocioException.class,
                () -> servicio.actualizar(1L, new ClienteRequest("71234567", "Juan Pérez", null)));
    }

    @Test
    void alVenderSeReusaElClienteQueYaExiste() {
        Cliente existente = new Cliente("20512345678", "Constructora Ejemplo SAC", null);
        when(repositorio.findByNumeroDocumento("20512345678")).thenReturn(Optional.of(existente));

        Cliente usado = servicio.buscarOCrear("20512345678", "Otro nombre");

        assertSame(existente, usado);
        verify(repositorio, never()).save(any());
    }
}
