package pe.idat.ferreteria.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import pe.idat.ferreteria.dto.CategoriaRequest;
import pe.idat.ferreteria.dto.MovimientoRequest;
import pe.idat.ferreteria.dto.ProductoRequest;
import pe.idat.ferreteria.model.TipoMovimiento;
import pe.idat.ferreteria.repository.CategoriaRepository;
import pe.idat.ferreteria.service.CategoriaService;
import pe.idat.ferreteria.service.KardexService;
import pe.idat.ferreteria.service.ProductoService;

/**
 * Carga un catálogo de ejemplo la primera vez que se inicia con la base vacía.
 * Usa los mismos servicios que la API, así el stock inicial también queda en el kardex.
 */
@Component
@Profile("!test")
public class DatosIniciales implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaService categoriaService;
    private final ProductoService productoService;
    private final KardexService kardexService;

    public DatosIniciales(CategoriaRepository categoriaRepository, CategoriaService categoriaService,
                          ProductoService productoService, KardexService kardexService) {
        this.categoriaRepository = categoriaRepository;
        this.categoriaService = categoriaService;
        this.productoService = productoService;
        this.kardexService = kardexService;
    }

    @Override
    public void run(String... args) {
        if (categoriaRepository.count() > 0) {
            return;
        }
        Long herramientas = categoria("Herramientas manuales");
        Long electricas = categoria("Herramientas eléctricas");
        Long fijacion = categoria("Tornillería y fijación");
        Long pinturas = categoria("Pinturas");
        Long gasfiteria = categoria("Gasfitería");

        producto("HM-001", "Martillo de uña 16 oz", herramientas, "18.50", "29.90", 5, 24);
        producto("HM-002", "Destornillador plano 1/4\"", herramientas, "4.20", "7.50", 10, 40);
        producto("HM-003", "Alicate universal 8\"", herramientas, "15.00", "25.00", 5, 3);
        producto("HE-001", "Taladro percutor 650 W", electricas, "145.00", "219.00", 2, 6);
        producto("HE-002", "Amoladora angular 4 1/2\"", electricas, "120.00", "185.00", 2, 1);
        producto("TF-001", "Tornillo drywall 6 x 1\" (ciento)", fijacion, "3.50", "6.00", 20, 150);
        producto("TF-002", "Tarugo plástico 1/4\" (ciento)", fijacion, "2.80", "5.00", 20, 12);
        producto("PI-001", "Pintura látex blanco 1 gl", pinturas, "28.00", "42.00", 6, 18);
        producto("PI-002", "Thinner acrílico 1 L", pinturas, "7.50", "12.00", 10, 8);
        producto("GA-001", "Tubo PVC 1/2\" x 5 m", gasfiteria, "9.00", "14.50", 15, 40);
        producto("GA-002", "Cinta teflón 3/4\"", gasfiteria, "0.80", "1.50", 30, 25);
    }

    private Long categoria(String nombre) {
        return categoriaService.crear(new CategoriaRequest(nombre)).id();
    }

    private void producto(String codigo, String descripcion, Long categoriaId, String compra, String venta,
                          int stockMinimo, int stockInicial) {
        Long id = productoService.crear(new ProductoRequest(codigo, descripcion, categoriaId,
                new BigDecimal(compra), new BigDecimal(venta), stockMinimo)).id();
        kardexService.registrar(new MovimientoRequest(id, TipoMovimiento.ENTRADA, stockInicial, "Inventario inicial"));
    }
}
