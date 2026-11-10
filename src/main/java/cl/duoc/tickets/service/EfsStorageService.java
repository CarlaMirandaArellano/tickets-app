package cl.duoc.tickets.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Almacenamiento temporal de los tickets en el EFS montado en el contenedor.
 * Estructura: {base}/tickets/{eventoId}/{usuarioId}/{codigo}.pdf
 */
@Service
public class EfsStorageService {

    private static final Logger log = LoggerFactory.getLogger(EfsStorageService.class);

    private final Path base;

    public EfsStorageService(@Value("${app.efs.base-path}") String basePath) {
        this.base = Paths.get(basePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.base.resolve("tickets"));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta del EFS en " + this.base, e);
        }
        log.info("EFS montado en {}", this.base);
    }

    public Path rutaTicket(String eventoId, String usuarioId, String codigo) {
        return base.resolve("tickets").resolve(eventoId).resolve(usuarioId).resolve(codigo + ".pdf");
    }

    public Path guardar(String eventoId, String usuarioId, String codigo, byte[] contenido) {
        Path destino = rutaTicket(eventoId, usuarioId, codigo);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, contenido);
            log.info("Ticket guardado en EFS: {}", destino);
            return destino;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir en el EFS: " + destino, e);
        }
    }

    public byte[] leer(String ruta) {
        try {
            return Files.readAllBytes(Paths.get(ruta));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer del EFS: " + ruta, e);
        }
    }

    public boolean existe(String ruta) {
        return ruta != null && Files.exists(Paths.get(ruta));
    }

    public void eliminar(String ruta) {
        if (ruta == null) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(ruta));
            log.info("Ticket eliminado del EFS: {}", ruta);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo eliminar del EFS: " + ruta, e);
        }
    }
}
