package cl.duoc.tickets.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Almacenamiento temporal en Amazon EFS.
 *
 * En la EC2 el EFS está montado en /mnt/efs y el contenedor lo ve en /app/efs
 * (docker run -v /mnt/efs:/app/efs). Los tickets quedan en:
 *   /app/efs/tickets/{eventoId}/{usuarioId}/ticketNNN.pdf
 */
@Service
public class EfsStorageService {

    private static final Logger log = LoggerFactory.getLogger(EfsStorageService.class);

    private final Path raizTickets;

    public EfsStorageService(@Value("${app.efs.path}") String efsPath) {
        this.raizTickets = Path.of(efsPath, "tickets").toAbsolutePath().normalize();
        try {
            Files.createDirectories(raizTickets);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta de tickets en el EFS: " + raizTickets, e);
        }
        log.info("Tickets se guardarán temporalmente en el EFS: {}", raizTickets);
    }

    /** Guarda el PDF y devuelve la ruta absoluta donde quedó. */
    public String guardar(String eventoId, String usuarioId, String nombreArchivo, byte[] contenido) {
        try {
            Path carpeta = raizTickets.resolve(eventoId).resolve(usuarioId).normalize();
            if (!carpeta.startsWith(raizTickets)) {
                throw new IllegalArgumentException("Ruta inválida");
            }
            Files.createDirectories(carpeta);
            Path archivo = carpeta.resolve(nombreArchivo);
            Files.write(archivo, contenido);
            return archivo.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir el ticket en el EFS", e);
        }
    }

    public byte[] leer(String ruta) {
        try {
            return Files.readAllBytes(Path.of(ruta));
        } catch (IOException e) {
            throw new IllegalStateException("El archivo ya no está en el EFS: " + ruta, e);
        }
    }

    public boolean existe(String ruta) {
        return ruta != null && Files.exists(Path.of(ruta));
    }

    public void eliminar(String ruta) {
        if (ruta == null) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(ruta));
        } catch (IOException e) {
            log.warn("No se pudo eliminar {} del EFS: {}", ruta, e.getMessage());
        }
    }

    public String getRaiz() {
        return raizTickets.toString();
    }
}
