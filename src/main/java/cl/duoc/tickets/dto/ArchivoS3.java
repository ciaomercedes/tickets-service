package cl.duoc.tickets.dto;

import java.time.Instant;

public record ArchivoS3(String clave, long tamanoBytes, Instant ultimaModificacion) {
}
