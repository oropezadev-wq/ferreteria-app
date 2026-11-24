package pe.idat.ferreteria.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import pe.idat.ferreteria.model.Rol;
import pe.idat.ferreteria.model.Usuario;
import pe.idat.ferreteria.repository.CategoriaRepository;
import pe.idat.ferreteria.repository.ClienteRepository;
import pe.idat.ferreteria.repository.MovimientoKardexRepository;
import pe.idat.ferreteria.repository.ProductoRepository;
import pe.idat.ferreteria.repository.TurnoCajaRepository;
import pe.idat.ferreteria.repository.UsuarioRepository;
import pe.idat.ferreteria.repository.VentaRepository;

/**
 * Pruebas de integración de los módulos de ventas, inventario (kardex), clientes, reportes y caja.
 * No usan una transacción de prueba: cada venta se confirma de verdad, así se comprueba que una
 * venta rechazada no deja stock descontado. Al terminar cada prueba se limpia la base.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "ADMINISTRADOR")
class VentaIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private VentaRepository ventaRepository;
    @Autowired
    private TurnoCajaRepository turnoRepository;
    @Autowired
    private MovimientoKardexRepository movimientoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;

    private long martillo;
    private long alicate;

    /** Martillo con 10 unidades (precio S/ 15.00, costo S/ 10.00) y alicate sin stock. */
    @BeforeEach
    void preparar() throws Exception {
        usuarioRepository.save(new Usuario("admin", "Administrador", "hash", Rol.ADMINISTRADOR));
        long categoria = idDe(mvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Herramientas\"}")).andExpect(status().isCreated()).andReturn());
        martillo = crearProducto("HM-001", "Martillo", categoria);
        alicate = crearProducto("HM-003", "Alicate", categoria);
        mvc.perform(post("/api/movimientos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productoId\":" + martillo + ",\"tipo\":\"ENTRADA\",\"cantidad\":10,\"motivo\":\"Compra\""
                        + ",\"tipoDocumento\":\"FACTURA\",\"numeroDocumento\":\"F001-000045\""
                        + ",\"fechaDocumento\":\"2026-11-20\",\"proveedor\":\"Distribuidora Ejemplo SAC\"}"))
                .andExpect(status().isCreated());
    }

    @AfterEach
    void limpiar() {
        ventaRepository.deleteAll();
        turnoRepository.deleteAll();
        movimientoRepository.deleteAll();
        clienteRepository.deleteAll();
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void laVentaDescuentaStockQuedaEnElKardexYSumaEnElReporte() throws Exception {
        vender("BOLETA", "EFECTIVO", martillo, 3).andExpect(status().isCreated())
                .andExpect(jsonPath("$.comprobante").value("B001-1"))
                .andExpect(jsonPath("$.total").value(45.00))
                .andExpect(jsonPath("$.gravada").value(38.14))
                .andExpect(jsonPath("$.igv").value(6.86))
                .andExpect(jsonPath("$.vendedor").value("Administrador"));

        mvc.perform(get("/api/productos/" + martillo)).andExpect(jsonPath("$.stock").value(7));
        mvc.perform(get("/api/movimientos").param("productoId", String.valueOf(martillo)))
                .andExpect(jsonPath("$[0].tipo").value("SALIDA"))
                .andExpect(jsonPath("$[0].motivo").value("Venta B001-1"));
        mvc.perform(get("/api/reportes/ventas"))
                .andExpect(jsonPath("$.cantidadVentas").value(1))
                .andExpect(jsonPath("$.total").value(45.00))
                .andExpect(jsonPath("$.utilidad").value(15.00))
                .andExpect(jsonPath("$.mayorRotacion[0].producto").value("Martillo"));
        mvc.perform(get("/api/panel/resumen")).andExpect(jsonPath("$.ventasHoy").value(45.00));
    }

    @Test
    void laVentaAnuladaDevuelveElStockYNoSumaEnElReporte() throws Exception {
        long venta = idDe(vender("BOLETA", "EFECTIVO", martillo, 3).andReturn());

        anular(venta).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ANULADA"));

        mvc.perform(get("/api/productos/" + martillo)).andExpect(jsonPath("$.stock").value(10));
        mvc.perform(get("/api/movimientos").param("productoId", String.valueOf(martillo)))
                .andExpect(jsonPath("$[0].tipo").value("DEVOLUCION"))
                .andExpect(jsonPath("$[0].motivo").value("Anulación de B001-1"));
        mvc.perform(get("/api/reportes/ventas"))
                .andExpect(jsonPath("$.cantidadVentas").value(0))
                .andExpect(jsonPath("$.cantidadAnuladas").value(1))
                .andExpect(jsonPath("$.total").value(0));
        anular(venta).andExpect(status().isConflict());
    }

    @Test
    void siUnProductoNoTieneStockNoSeGuardaNadaDeLaVenta() throws Exception {
        mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoComprobante\":\"BOLETA\",\"metodoPago\":\"EFECTIVO\",\"lineas\":["
                                + "{\"productoId\":" + martillo + ",\"cantidad\":5},"
                                + "{\"productoId\":" + alicate + ",\"cantidad\":1}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Stock insuficiente: hay 0 unidades de Alicate"));

        mvc.perform(get("/api/productos/" + martillo)).andExpect(jsonPath("$.stock").value(10));
        mvc.perform(get("/api/ventas")).andExpect(jsonPath("$", hasSize(0)));
        vender("BOLETA", "EFECTIVO", martillo, 1).andExpect(jsonPath("$.comprobante").value("B001-1"));
    }

    @Test
    void laFacturaRegistraAlClienteYLasSeriesSonIndependientes() throws Exception {
        vender("BOLETA", "EFECTIVO", martillo, 1).andExpect(jsonPath("$.comprobante").value("B001-1"));
        mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoComprobante\":\"FACTURA\",\"metodoPago\":\"TARJETA\","
                                + "\"clienteDocumento\":\"20512345678\","
                                + "\"clienteNombre\":\"Constructora Ejemplo SAC\",\"lineas\":["
                                + "{\"productoId\":" + martillo + ",\"cantidad\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comprobante").value("F001-1"))
                .andExpect(jsonPath("$.clienteNombre").value("Constructora Ejemplo SAC"));

        mvc.perform(get("/api/clientes").param("buscar", "20512"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tipoDocumento").value("RUC"));
    }

    @Test
    void laFacturaSinRucSeRechaza() throws Exception {
        vender("FACTURA", "EFECTIVO", martillo, 1).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje")
                        .value("La factura necesita un RUC válido de 11 dígitos (empieza con 10 o 20)"));
        mvc.perform(get("/api/productos/" + martillo)).andExpect(jsonPath("$.stock").value(10));
    }

    @Test
    void elCierreDeCajaCuadraConLasVentasEnEfectivoSinLasAnuladas() throws Exception {
        caja("abrir", "200.00").andExpect(status().isCreated());
        vender("BOLETA", "EFECTIVO", martillo, 2);
        vender("BOLETA", "TARJETA", martillo, 1);
        long anulada = idDe(vender("BOLETA", "EFECTIVO", martillo, 1).andReturn());
        anular(anulada);

        mvc.perform(get("/api/caja/actual"))
                .andExpect(jsonPath("$.ventasEfectivo").value(30.00))
                .andExpect(jsonPath("$.ventasTarjeta").value(15.00))
                .andExpect(jsonPath("$.montoEsperado").value(230.00));
        caja("cerrar", "225.00").andExpect(status().isOk())
                .andExpect(jsonPath("$.abierto").value(false))
                .andExpect(jsonPath("$.diferencia").value(-5.00));
        mvc.perform(get("/api/caja/actual")).andExpect(status().isNoContent());
        caja("cerrar", "10.00").andExpect(status().isConflict());
    }

    @Test
    void laDevolucionNoSePuedeRegistrarAMano() throws Exception {
        mvc.perform(post("/api/movimientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":" + martillo + ",\"tipo\":\"DEVOLUCION\",\"cantidad\":5,"
                                + "\"motivo\":\"Prueba\"}"))
                .andExpect(status().isConflict());
    }

    private ResultActions vender(String comprobante, String pago, long producto, int cantidad) throws Exception {
        return mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoComprobante\":\"" + comprobante + "\",\"metodoPago\":\"" + pago + "\",\"lineas\":["
                        + "{\"productoId\":" + producto + ",\"cantidad\":" + cantidad + "}]}"));
    }

    private ResultActions anular(long venta) throws Exception {
        return mvc.perform(patch("/api/ventas/" + venta + "/anular").contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Cantidad equivocada\"}"));
    }

    private ResultActions caja(String accion, String monto) throws Exception {
        return mvc.perform(post("/api/caja/" + accion).contentType(MediaType.APPLICATION_JSON)
                .content("{\"monto\":" + monto + "}"));
    }

    private long crearProducto(String codigo, String descripcion, long categoria) throws Exception {
        return idDe(mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + codigo + "\",\"descripcion\":\"" + descripcion
                                + "\",\"categoriaId\":" + categoria
                                + ",\"precioCompra\":10.00,\"precioVenta\":15.00,\"stockMinimo\":2}"))
                .andExpect(status().isCreated()).andReturn());
    }

    private static long idDe(MvcResult r) throws Exception {
        Number id = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
