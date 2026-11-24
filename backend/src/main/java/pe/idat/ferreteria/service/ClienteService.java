package pe.idat.ferreteria.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.idat.ferreteria.dto.ClienteRequest;
import pe.idat.ferreteria.dto.ClienteResponse;
import pe.idat.ferreteria.exception.RecursoNoEncontradoException;
import pe.idat.ferreteria.exception.ReglaNegocioException;
import pe.idat.ferreteria.model.Cliente;
import pe.idat.ferreteria.repository.ClienteRepository;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> buscar(String texto) {
        String filtro = texto == null ? "" : texto.trim();
        return clienteRepository.buscar(filtro).stream().map(ClienteResponse::de).toList();
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        if (clienteRepository.findByNumeroDocumento(request.numeroDocumento()).isPresent()) {
            throw new ReglaNegocioException("Ya existe un cliente con el documento " + request.numeroDocumento());
        }
        Cliente cliente = new Cliente(request.numeroDocumento(), request.nombre().trim(), limpiar(request.telefono()));
        return ClienteResponse.de(clienteRepository.save(cliente));
    }

    /** El documento no cambia: identifica al cliente en sus comprobantes. */
    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el cliente " + id));
        if (!cliente.getNumeroDocumento().equals(request.numeroDocumento())) {
            throw new ReglaNegocioException("El DNI o RUC de un cliente no se puede cambiar");
        }
        cliente.setNombre(request.nombre().trim());
        cliente.setTelefono(limpiar(request.telefono()));
        return ClienteResponse.de(cliente);
    }

    /**
     * Al vender: si el cliente ya existe se usa ese registro; si no, se crea con el nombre
     * ingresado en la venta.
     */
    @Transactional
    public Cliente buscarOCrear(String numeroDocumento, String nombre) {
        return clienteRepository.findByNumeroDocumento(numeroDocumento)
                .orElseGet(() -> clienteRepository.save(new Cliente(numeroDocumento,
                        nombre != null ? nombre : "Cliente con documento " + numeroDocumento, null)));
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
