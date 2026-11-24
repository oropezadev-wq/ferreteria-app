package pe.idat.ferreteria.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import pe.idat.ferreteria.model.Rol;
import pe.idat.ferreteria.model.Usuario;
import pe.idat.ferreteria.repository.UsuarioRepository;

/**
 * Pruebas de integración de la seguridad con tokens JWT reales: inicio de sesión,
 * acceso sin token, token alterado, permisos por rol y catálogo público.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SeguridadIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void crearUsuarios() {
        usuarioRepository.save(new Usuario("admin", "Administrador", passwordEncoder.encode("admin123"),
                Rol.ADMINISTRADOR));
        usuarioRepository.save(new Usuario("vendedor", "Vendedor", passwordEncoder.encode("vendedor123"),
                Rol.VENDEDOR));
    }

    @Test
    void loginCorrectoDevuelveTokenYRol() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"vendedor\",\"clave\":\"vendedor123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("VENDEDOR"))
                .andExpect(jsonPath("$.token").value(startsWith("eyJ")));
    }

    @Test
    void contraseñaIncorrectaDevuelve401SinDecirQueFallo() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"vendedor\",\"clave\":\"incorrecta1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos"));
    }

    @Test
    void sinTokenNoSeEntraALaApi() throws Exception {
        mvc.perform(get("/api/productos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Inicie sesión para continuar"));
    }

    @Test
    void unTokenAlteradoSeRechaza() throws Exception {
        String token = token("admin", "admin123");
        String alterado = token.substring(0, token.length() - 4) + "AAAA";

        mvc.perform(get("/api/productos").header("Authorization", "Bearer " + alterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elVendedorVeProductosPeroNoPuedeCrearlos() throws Exception {
        String token = token("vendedor", "vendedor123");

        mvc.perform(get("/api/productos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(post("/api/categorias").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Pinturas\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Su usuario no tiene permiso para esta opción"));
    }

    @Test
    void elVendedorNoVeReportesNiAnulaVentas() throws Exception {
        String token = token("vendedor", "vendedor123");

        mvc.perform(get("/api/reportes/ventas").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/ventas/1/anular").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Prueba\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elCatalogoEsPublicoYNoMuestraElPrecioDeCompra() throws Exception {
        String token = token("admin", "admin123");
        String categoria = mvc.perform(post("/api/categorias").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Gasfitería\"}"))
                .andReturn().getResponse().getContentAsString();
        Number categoriaId = JsonPath.read(categoria, "$.id");
        mvc.perform(post("/api/productos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"GA-002\",\"descripcion\":\"Cinta teflón\",\"categoriaId\":"
                                + categoriaId + ",\"precioCompra\":0.80,\"precioVenta\":1.50,\"stockMinimo\":30}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/catalogo").param("buscar", "teflón"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].precioVenta").value(1.50))
                .andExpect(jsonPath("$[0].disponible").value(false))
                .andExpect(jsonPath("$[0].precioCompra").doesNotExist())
                .andExpect(jsonPath("$[0].stock").doesNotExist());
    }

    @Test
    void unaContraseñaDebilNoSeAcepta() throws Exception {
        mvc.perform(post("/api/usuarios").header("Authorization", "Bearer " + token("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"caja2\",\"nombre\":\"Caja dos\",\"clave\":\"12345\","
                                + "\"rol\":\"VENDEDOR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles[0]")
                        .value("La contraseña debe tener al menos 8 caracteres, con letras y números"));
    }

    @Test
    void elNuevoUsuarioSeGuardaConLaContraseñaCifradaYPuedeIngresar() throws Exception {
        mvc.perform(post("/api/usuarios").header("Authorization", "Bearer " + token("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"caja2\",\"nombre\":\"Caja dos\",\"clave\":\"Clave2026\","
                                + "\"rol\":\"VENDEDOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clave").doesNotExist())
                .andExpect(jsonPath("$.claveHash").doesNotExist());

        String hash = usuarioRepository.findByUsuario("caja2").orElseThrow().getClaveHash();
        assertNotEquals("Clave2026", hash);
        assertTrue(hash.startsWith("$2"));
        token("caja2", "Clave2026");
    }

    private String token(String usuario, String clave) throws Exception {
        String json = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"" + usuario + "\",\"clave\":\"" + clave + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }
}
