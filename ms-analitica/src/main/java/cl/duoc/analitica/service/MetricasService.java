package cl.duoc.analitica.service;

import cl.duoc.analitica.model.MetricasResumen;
import cl.duoc.analitica.model.PedidoEvento;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

/** Metricas en memoria. Se pierden al reiniciar el servicio. */
@Service
public class MetricasService {

    private final Set<String> eventosVistos = new HashSet<>();
    private final Map<String, Long> porEstado = new HashMap<>();
    private final Map<String, Long> porProducto = new HashMap<>();
    private long total;
    private long unidades;
    private Instant actualizadoEn;

    /** @return true si el evento se conto; false si era un eventId repetido (redelivery). */
    public synchronized boolean registrar(PedidoEvento evento) {
        if (!eventosVistos.add(evento.eventId())) {
            return false;
        }
        total++;
        unidades += evento.cantidad();
        porEstado.merge(evento.estado(), 1L, Long::sum);
        porProducto.merge(evento.producto(), 1L, Long::sum);
        actualizadoEn = Instant.now();
        return true;
    }

    public synchronized MetricasResumen resumen() {
        return new MetricasResumen(total, unidades, new TreeMap<>(porEstado), new TreeMap<>(porProducto), actualizadoEn);
    }
}