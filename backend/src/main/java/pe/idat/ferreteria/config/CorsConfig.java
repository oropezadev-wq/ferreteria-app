package pe.idat.ferreteria.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Permite que la aplicación Angular (otro puerto) consuma la API. */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String origen;

    public CorsConfig(@Value("${app.cors.origen}") String origen) {
        this.origen = origen;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origen)
                .allowedMethods("GET", "POST", "PUT", "PATCH");
    }
}
