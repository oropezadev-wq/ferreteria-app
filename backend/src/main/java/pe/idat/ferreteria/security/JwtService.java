package pe.idat.ferreteria.security;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import pe.idat.ferreteria.model.Usuario;

/** Genera el token firmado (HS256) que identifica al usuario y su rol en cada petición. */
@Service
public class JwtService {

    public static final String EMISOR = "ferreteria-api";
    public static final String CLAIM_ROL = "rol";

    private final JwtEncoder encoder;
    private final Duration vigencia;

    public JwtService(JwtEncoder encoder, @Value("${app.jwt.horas}") long horas) {
        this.encoder = encoder;
        this.vigencia = Duration.ofHours(horas);
    }

    public String generar(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .issuedAt(ahora)
                .expiresAt(ahora.plus(vigencia))
                .subject(usuario.getUsuario())
                .claim(CLAIM_ROL, usuario.getRol().name())
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }
}
