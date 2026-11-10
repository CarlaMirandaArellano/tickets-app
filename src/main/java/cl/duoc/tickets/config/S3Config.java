package cl.duoc.tickets.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    /**
     * Cliente S3 con la cadena de credenciales por defecto del SDK:
     * 1) Variables de entorno AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY / AWS_SESSION_TOKEN
     * 2) Rol de la instancia EC2 (LabInstanceProfile en AWS Academy)
     */
    @Bean
    public S3Client s3Client(@Value("${app.s3.region}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
