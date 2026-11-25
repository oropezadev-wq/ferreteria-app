package pe.idat.ferreteria.security;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Reglas de acceso de la API. No hay sesiones en el servidor: cada petición trae su token JWT.
 * El público solo entra al catálogo; vender y atender clientes lo hacen ambos roles;
 * el resto (productos, inventario, reportes, usuarios, anulaciones) solo el administrador.
 */
@Configuration
public class SeguridadConfig {

    private static final String ADMIN = "ADMINISTRADOR";

    @Bean
    SecurityFilterChain filtros(HttpSecurity http, RespuestasDeSeguridad respuestas) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/catalogo/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/h2-console/**",
                                "/error").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/ventas/*/anular").hasRole(ADMIN)
                        .requestMatchers("/api/ventas/**", "/api/clientes/**", "/api/caja/**", "/api/panel/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/productos/**", "/api/categorias/**").authenticated()
                        // Página de Angular (solo la versión portable la sirve desde aquí); los datos siguen en /api.
                        .requestMatchers(r -> !r.getRequestURI().startsWith("/api/")).permitAll()
                        .anyRequest().hasRole(ADMIN))
                .oauth2ResourceServer(o -> o
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolesDelToken()))
                        .authenticationEntryPoint(respuestas))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas));
        return http.build();
    }

    /** El claim "rol" del token se convierte en la autoridad ROLE_ADMINISTRADOR o ROLE_VENDEDOR. */
    private static JwtAuthenticationConverter rolesDelToken() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(JwtService.CLAIM_ROL);
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(roles);
        return conversor;
    }

    @Bean
    SecretKey claveJwt(@Value("${app.jwt.secreto}") String secreto) {
        if (secreto.length() < 32) {
            throw new IllegalStateException("app.jwt.secreto debe tener al menos 32 caracteres");
        }
        return new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey clave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    /** Rechaza tokens con otra firma, vencidos o emitidos por otro sistema. */
    @Bean
    JwtDecoder jwtDecoder(SecretKey clave) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JwtService.EMISOR));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Solo la aplicación Angular puede llamar a la API desde el navegador. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.origen}") String origen) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origen));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", config);
        return fuente;
    }
}
