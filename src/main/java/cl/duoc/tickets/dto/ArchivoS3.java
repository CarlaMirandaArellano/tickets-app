package cl.duoc.tickets.dto;

import java.time.Instant;

public record ArchivoS3(String key, long tamanoBytes, Instant ultimaModificacion) {
}
