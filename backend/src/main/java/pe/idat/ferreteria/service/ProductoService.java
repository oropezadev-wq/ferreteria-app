package pe.idat.ferreteria.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.idat.ferreteria.dto.ProductoRequest;
import pe.idat.ferreteria.dto.ProductoResponse;
import pe.idat.ferreteria.exception.RecursoNoEncontradoException;
import pe.idat.ferreteria.exception.ReglaNegocioException;
import pe.idat.ferreteria.model.Categoria;
import pe.idat.ferreteria.model.Producto;
import pe.idat.ferreteria.repository.CategoriaRepository;
import pe.idat.ferreteria.repository.ProductoRepository;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoService(ProductoRepository productoRepository, CategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> buscar(String texto, boolean soloActivos) {
        String filtro = texto == null ? "" : texto.trim();
        return productoRepository.buscar(filtro, soloActivos).stream()
                .map(ProductoResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.de(buscarEntidad(id));
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> conStockBajo() {
        return productoRepository.conStockBajo().stream()
                .map(ProductoResponse::de)
                .toList();
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        String codigo = request.codigo().trim();
        if (productoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new ReglaNegocioException("Ya existe un producto con el código " + codigo);
        }
        validarPrecios(request);
        Producto producto = new Producto(codigo, request.descripcion().trim(), buscarCategoria(request.categoriaId()),
                request.precioCompra(), request.precioVenta(), request.stockMinimo());
        return ProductoResponse.de(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscarEntidad(id);
        String codigo = request.codigo().trim();
        if (productoRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw new ReglaNegocioException("Ya existe un producto con el código " + codigo);
        }
        validarPrecios(request);
        producto.setCodigo(codigo);
        producto.setDescripcion(request.descripcion().trim());
        producto.setCategoria(buscarCategoria(request.categoriaId()));
        producto.setPrecioCompra(request.precioCompra());
        producto.setPrecioVenta(request.precioVenta());
        producto.setStockMinimo(request.stockMinimo());
        return ProductoResponse.de(producto);
    }

    /** No se borra: el producto tiene historial en el kardex, así que solo se oculta de la venta. */
    @Transactional
    public ProductoResponse cambiarEstado(Long id, boolean activo) {
        Producto producto = buscarEntidad(id);
        producto.setActivo(activo);
        return ProductoResponse.de(producto);
    }

    Producto buscarEntidad(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + id));
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoría " + id));
    }

    private static void validarPrecios(ProductoRequest request) {
        if (request.precioVenta().compareTo(request.precioCompra()) < 0) {
            throw new ReglaNegocioException("El precio de venta no puede ser menor que el de compra");
        }
    }
}
