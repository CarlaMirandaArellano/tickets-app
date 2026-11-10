package cl.duoc.tickets.service;

import cl.duoc.tickets.dto.ArchivoS3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.List;
import java.util.Map;

/**
 * Operaciones sobre el bucket S3.
 * Organización de las keys: {eventoId}/{usuarioId}/{codigo}.pdf
 * (por ejemplo evento123/usuarioX/ticket001.pdf)
 */
@Service
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3;
    private final String bucket;

    public S3StorageService(S3Client s3, @Value("${app.s3.bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    public static String construirKey(String eventoId, String usuarioId, String codigo) {
        return eventoId + "/" + usuarioId + "/" + codigo + ".pdf";
    }

    public boolean configurado() {
        return bucket != null && !bucket.isBlank();
    }

    public String getBucket() {
        return bucket;
    }

    public void subir(String key, byte[] contenido, Map<String, String> metadata) {
        validarBucket();
        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("application/pdf")
                .metadata(metadata)
                .build();
        s3.putObject(req, RequestBody.fromBytes(contenido));
        log.info("Ticket subido a s3://{}/{}", bucket, key);
    }

    public byte[] descargar(String key) {
        validarBucket();
        GetObjectRequest req = GetObjectRequest.builder().bucket(bucket).key(key).build();
        return s3.getObjectAsBytes(req).asByteArray();
    }

    public void eliminar(String key) {
        validarBucket();
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        log.info("Ticket eliminado de s3://{}/{}", bucket, key);
    }

    public List<ArchivoS3> listar(String prefijo) {
        validarBucket();
        ListObjectsV2Request.Builder req = ListObjectsV2Request.builder().bucket(bucket);
        if (prefijo != null && !prefijo.isBlank()) {
            req.prefix(prefijo);
        }
        return s3.listObjectsV2Paginator(req.build()).contents().stream()
                .map(o -> new ArchivoS3(o.key(), o.size(), o.lastModified()))
                .toList();
    }

    private void validarBucket() {
        if (!configurado()) {
            throw new IllegalStateException(
                    "No hay bucket S3 configurado. Define la variable de entorno AWS_S3_BUCKET.");
        }
    }
}
