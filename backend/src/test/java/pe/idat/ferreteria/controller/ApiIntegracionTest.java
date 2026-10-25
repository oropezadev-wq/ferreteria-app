package pe.idat.ferreteria.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/**
 * Pruebas de integración: recorren las tres capas (controlador, servicio y repositorio)
 * contra una base H2 en memoria. Cada prueba se deshace al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void registraProductoEntradaYSalidaYElKardexLoRefleja() throws Exception {
        long categoria = crearCategoria("Herramientas");
        long producto = crearProducto("HM-001", "Martillo de uña", categoria, 5);

        movimiento(producto, "ENTRADA", 10).andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockResultante").value(10));
        movimiento(producto, "SALIDA", 7).andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidad").value(-7))
                .andExpect(jsonPath("$.stockResultante").value(3));

        mvc.perform(get("/api/productos/" + producto))
                .andExpect(jsonPath("$.stock").value(3))
                .andExpect(jsonPath("$.stockBajo").value(true));
        mvc.perform(get("/api/movimientos").param("productoId", String.valueOf(producto)))
                .andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/productos/alertas"))
                .andExpect(jsonPath("$[0].codigo").value("HM-001"));
    }

    @Test
    void buscaPorCodigoODescripcionSinDistinguirMayusculas() throws Exception {
        long categoria = crearCategoria("Pinturas");
        crearProducto("PI-001", "Pintura látex blanco", categoria, 2);
        crearProducto("PI-002", "Thinner acrílico", categoria, 2);

        mvc.perform(get("/api/productos").param("buscar", "LÁTEX"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigo").value("PI-001"));
        mvc.perform(get("/api/productos").param("buscar", "pi-00"))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void salidaSinStockDevuelve409YNoCambiaNada() throws Exception {
        long producto = crearProducto("TF-001", "Tornillo", crearCategoria("Fijación"), 10);
        movimiento(producto, "ENTRADA", 2);

        movimiento(producto, "SALIDA", 5).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").exists());
        mvc.perform(get("/api/productos/" + producto)).andExpect(jsonPath("$.stock").value(2));
    }

    @Test
    void codigoRepetidoDevuelve409() throws Exception {
        long categoria = crearCategoria("Gasfitería");
        crearProducto("GA-001", "Tubo PVC", categoria, 5);

        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("ga-001", "Otro tubo", categoria, 5)))
                .andExpect(status().isConflict());
    }

    @Test
    void datosInvalidosDevuelven400ConDetalle() throws Exception {
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"\",\"descripcion\":\"\",\"precioVenta\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").isNotEmpty());
    }

    @Test
    void productoInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/productos/9999")).andExpect(status().isNotFound());
    }

    @Test
    void productoDesactivadoNoAdmiteMovimientosYSeOcultaEnLaVenta() throws Exception {
        long producto = crearProducto("HE-001", "Taladro", crearCategoria("Eléctricas"), 1);
        mvc.perform(patch("/api/productos/" + producto + "/desactivar"))
                .andExpect(jsonPath("$.activo").value(false));

        movimiento(producto, "ENTRADA", 3).andExpect(status().isConflict());
        mvc.perform(get("/api/productos").param("soloActivos", "true"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void actualizarNoCambiaElStock() throws Exception {
        long categoria = crearCategoria("Herramientas");
        long producto = crearProducto("HM-002", "Alicate", categoria, 2);
        movimiento(producto, "ENTRADA", 6);

        mvc.perform(put("/api/productos/" + producto).contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("HM-002", "Alicate universal 8\\\"", categoria, 4)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockMinimo").value(4))
                .andExpect(jsonPath("$.stock").value(6));
    }

    @Test
    void resumenDelPanel() throws Exception {
        long producto = crearProducto("PI-003", "Brocha 2\\\"", crearCategoria("Pinturas"), 5);
        movimiento(producto, "ENTRADA", 3);

        mvc.perform(get("/api/panel/resumen"))
                .andExpect(jsonPath("$.productosActivos").value(1))
                .andExpect(jsonPath("$.productosStockBajo").value(1))
                .andExpect(jsonPath("$.valorInventario").value(30.0))
                .andExpect(jsonPath("$.movimientosHoy").value(1));
    }

    private long crearCategoria(String nombre) throws Exception {
        MvcResult r = mvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return idDe(r);
    }

    private long crearProducto(String codigo, String descripcion, long categoria, int stockMinimo) throws Exception {
        MvcResult r = mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson(codigo, descripcion, categoria, stockMinimo)))
                .andExpect(status().isCreated()).andReturn();
        return idDe(r);
    }

    private ResultActions movimiento(long producto, String tipo, int cantidad)
            throws Exception {
        return mvc.perform(post("/api/movimientos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productoId\":" + producto + ",\"tipo\":\"" + tipo + "\",\"cantidad\":" + cantidad
                        + ",\"motivo\":\"Prueba\"}"));
    }

    private static String productoJson(String codigo, String descripcion, long categoria, int stockMinimo) {
        return "{\"codigo\":\"" + codigo + "\",\"descripcion\":\"" + descripcion + "\",\"categoriaId\":" + categoria
                + ",\"precioCompra\":10.00,\"precioVenta\":15.00,\"stockMinimo\":" + stockMinimo + "}";
    }

    private static long idDe(MvcResult r) throws Exception {
        Number id = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
