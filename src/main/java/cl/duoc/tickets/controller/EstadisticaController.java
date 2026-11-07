package cl.duoc.tickets.controller;

import cl.duoc.tickets.dto.EstadisticaEvento;
import cl.duoc.tickets.service.EfsStorageService;
import cl.duoc.tickets.service.EstadisticaService;
import cl.duoc.tickets.service.S3StorageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class EstadisticaController {

    private final EstadisticaService estadisticas;
    private final EfsStorageService efs;
    private final S3StorageService s3;

    public EstadisticaController(EstadisticaService estadisticas, EfsStorageService efs, S3StorageService s3) {
        this.estadisticas = estadisticas;
        this.efs = efs;
        this.s3 = s3;
    }

    /** 6. Estadísticas de ventas y reservas de todos los eventos. */
    @GetMapping("/api/estadisticas/eventos")
    public List<EstadisticaEvento> todas() {
        return estadisticas.porEvento();
    }

    /** 6b. Estadísticas de un evento. */
    @GetMapping("/api/estadisticas/eventos/{eventoId}")
    public EstadisticaEvento deEvento(@PathVariable String eventoId) {
        return estadisticas.deEvento(eventoId);
    }

    /** Info rápida para comprobar que el despliegue funciona. */
    @GetMapping("/")
    public Map<String, String> info() {
        return Map.of(
                "servicio", "tickets-service",
                "estado", "OK",
                "efs", efs.getRaiz(),
                "bucketS3", s3.getBucket());
    }
}
