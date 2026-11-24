package pe.idat.ferreteria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import pe.idat.ferreteria.dto.LoginRequest;
import pe.idat.ferreteria.dto.LoginResponse;
import pe.idat.ferreteria.dto.UsuarioRequest;
import pe.idat.ferreteria.exception.CredencialesInvalidasException;
import pe.idat.ferreteria.exception.ReglaNegocioException;
import pe.idat.ferreteria.model.Rol;
import pe.idat.ferreteria.model.Usuario;
import pe.idat.ferreteria.repository.UsuarioRepository;
import pe.idat.ferreteria.security.JwtService;

/** Pruebas unitarias del inicio de sesión y del registro de usuarios (BCrypt real, repositorio simulado). */
class UsuarioServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private UsuarioRepository repositorio;
    private UsuarioService servicio;
    private Usuario vendedor;

    @BeforeEach
    void preparar() {
        repositorio = mock(UsuarioRepository.class);
        JwtService jwtService = mock(JwtService.class);
        when(jwtService.generar(any())).thenReturn("token-de-prueba");
        servicio = new UsuarioService(repositorio, encoder, jwtService);

        vendedor = new Usuario("vendedor", "Vendedor de turno", encoder.encode("vendedor123"), Rol.VENDEDOR);
        when(repositorio.findByUsuario("vendedor")).thenReturn(Optional.of(vendedor));
    }

    @Test
    void conLaContraseñaCorrectaEntregaElTokenYElRol() {
        LoginResponse respuesta = servicio.ingresar(new LoginRequest("Vendedor ", "vendedor123"));

        assertEquals("token-de-prueba", respuesta.token());
        assertEquals(Rol.VENDEDOR, respuesta.rol());
    }

    @Test
    void conLaContraseñaIncorrectaSeRechaza() {
        assertThrows(CredencialesInvalidasException.class,
                () -> servicio.ingresar(new LoginRequest("vendedor", "otraclave1")));
    }

    @Test
    void unUsuarioDesactivadoNoPuedeIngresar() {
        vendedor.setActivo(false);

        assertThrows(CredencialesInvalidasException.class,
                () -> servicio.ingresar(new LoginRequest("vendedor", "vendedor123")));
    }

    @Test
    void laContraseñaSeGuardaCifradaConBCrypt() {
        when(repositorio.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(repositorio.findByUsuario("caja2")).thenReturn(Optional.empty());

        servicio.crear(new UsuarioRequest("caja2", "Caja dos", "Clave2026", Rol.VENDEDOR));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorio).save(guardado.capture());
        String hash = guardado.getValue().getClaveHash();
        assertNotEquals("Clave2026", hash);
        assertTrue(hash.startsWith("$2"));
        assertTrue(encoder.matches("Clave2026", hash));
    }

    @Test
    void noSePuedeCrearUnUsuarioRepetido() {
        when(repositorio.existsByUsuario("vendedor")).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(new UsuarioRequest("vendedor", "Otro", "Clave2026", Rol.VENDEDOR)));
    }

    @Test
    void elAdministradorNoPuedeDesactivarseASiMismo() {
        Usuario admin = new Usuario("admin", "Administrador", "hash", Rol.ADMINISTRADOR);
        when(repositorio.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(ReglaNegocioException.class, () -> servicio.cambiarEstado(1L, false, "admin"));
    }
}
