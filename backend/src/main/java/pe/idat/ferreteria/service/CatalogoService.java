package pe.idat.ferreteria.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.idat.ferreteria.dto.CatalogoResponse;
import pe.idat.ferreteria.repository.ProductoRepository;

/** Catálogo público: lo consultan los clientes sin iniciar sesión. */
@Service
public class CatalogoService {

    private final ProductoRepository productoRepository;

    public CatalogoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public List<CatalogoResponse> buscar(String texto, Long categoriaId) {
        String filtro = texto == null ? "" : texto.trim();
        return productoRepository.buscar(filtro, true).stream()
                .filter(p -> categoriaId == null || p.getCategoria().getId().equals(categoriaId))
                .map(CatalogoResponse::de)
                .toList();
    }
}
