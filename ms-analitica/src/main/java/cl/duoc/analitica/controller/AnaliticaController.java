package cl.duoc.analitica.controller;

import cl.duoc.analitica.model.MetricasResumen;
import cl.duoc.analitica.service.MetricasService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analitica")
public class AnaliticaController {

    private final MetricasService metricas;

    public AnaliticaController(MetricasService metricas) {
        this.metricas = metricas;
    }

    @GetMapping("/metricas")
    public MetricasResumen metricas() {
        return metricas.resumen();
    }
}