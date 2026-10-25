package pe.idat.ferreteria.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.idat.ferreteria.dto.ResumenResponse;
import pe.idat.ferreteria.service.PanelService;

@RestController
@RequestMapping("/api/panel")
public class PanelController {

    private final PanelService panelService;

    public PanelController(PanelService panelService) {
        this.panelService = panelService;
    }

    @GetMapping("/resumen")
    public ResumenResponse resumen() {
        return panelService.resumen();
    }
}
