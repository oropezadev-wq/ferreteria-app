package pe.idat.ferreteria.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.idat.ferreteria.dto.LoginRequest;
import pe.idat.ferreteria.dto.LoginResponse;
import pe.idat.ferreteria.dto.UsuarioRequest;
import pe.idat.ferreteria.dto.UsuarioResponse;
import pe.idat.ferreteria.exception.CredencialesInvalidasException;
import pe.idat.ferreteria.exception.RecursoNoEncontradoException;
import pe.idat.ferreteria.exception.ReglaNegocioException;
import pe.idat.ferreteria.model.Usuario;
import pe.idat.ferreteria.repository.UsuarioRepository;
import pe.idat.ferreteria.security.JwtService;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Compara la contraseña con su hash BCrypt y, si coincide, entrega el token. */
    @Transactional(readOnly = true)
    public LoginResponse ingresar(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsuario(request.usuario().trim().toLowerCase())
                .filter(Usuario::isActivo)
                .filter(u -> passwordEncoder.matches(request.clave(), u.getClaveHash()))
                .orElseThrow(CredencialesInvalidasException::new);
        return new LoginResponse(jwtService.generar(usuario), usuario.getUsuario(), usuario.getNombre(),
                usuario.getRol());
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAllByOrderByNombre().stream().map(UsuarioResponse::de).toList();
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (usuarioRepository.existsByUsuario(request.usuario())) {
            throw new ReglaNegocioException("Ya existe el usuario " + request.usuario());
        }
        Usuario usuario = new Usuario(request.usuario(), request.nombre().trim(),
                passwordEncoder.encode(request.clave()), request.rol());
        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    /** Un usuario desactivado ya no puede iniciar sesión ni registrar ventas. */
    @Transactional
    public UsuarioResponse cambiarEstado(Long id, boolean activo, String usuarioActual) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + id));
        if (!activo && usuario.getUsuario().equals(usuarioActual)) {
            throw new ReglaNegocioException("No puede desactivar su propio usuario");
        }
        usuario.setActivo(activo);
        return UsuarioResponse.de(usuario);
    }

    /** El usuario que hace la petición (sale del token). */
    @Transactional(readOnly = true)
    public Usuario buscarActivo(String usuario) {
        return usuarioRepository.findByUsuario(usuario)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new ReglaNegocioException("El usuario " + usuario + " no está activo"));
    }
}
