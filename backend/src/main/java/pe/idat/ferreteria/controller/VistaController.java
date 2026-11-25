package pe.idat.ferreteria.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Solo en la versión portable, donde el mismo servidor entrega la página de Angular.
 * Las rutas de Angular (/panel, /ventas, /catalogo...) no son archivos: se responde con
 * index.html y Angular muestra la pantalla que corresponde.
 */
@Controller
@Profile("portable")
public class VistaController {

    @GetMapping("/{ruta:[^.]*}")
    public String paginaAngular() {
        return "forward:/index.html";
    }
}
