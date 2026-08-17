package cd.shad.erp.cmk.cmkerp.platform.common.infrastructure.adapter;

import cd.shad.erp.cmk.cmkerp.platform.common.application.port.FileStoragePort;
import cd.shad.erp.cmk.cmkerp.platform.storage.S3FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Adapter pour le stockage de fichiers via S3/MinIO.
 *
 * <p>Implémente le port FileStoragePort en utilisant S3FileStorageService
 * (qui utilise MinioClient en interne).
 *
 * <p>Cet adapter permet de découpler le domaine des détails d'implémentation
 * du stockage S3-compatible (MinIO, AWS S3, OVH Object Storage, etc.).
 */
@Component
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
@RequiredArgsConstructor
@Slf4j
public class S3FileStorageAdapter implements FileStoragePort {

    private final S3FileStorageService s3FileStorageService;

    @Override
    public String upload(String bucket, String key, InputStream inputStream) {
        log.debug("Upload de fichier via S3FileStorageAdapter -> bucket: {}, key: {}", bucket, key);
        return s3FileStorageService.upload(bucket, key, inputStream);
    }

    @Override
    public InputStream download(String bucket, String key) {
        log.debug("Download de fichier via S3FileStorageAdapter -> bucket: {}, key: {}", bucket, key);
        return s3FileStorageService.download(bucket, key);
    }

    @Override
    public void delete(String bucket, String key) {
        log.debug("Suppression de fichier via S3FileStorageAdapter -> bucket: {}, key: {}", bucket, key);
        s3FileStorageService.delete(bucket, key);
    }

    @Override
    public boolean exists(String bucket, String key) {
        return s3FileStorageService.exists(bucket, key);
    }

    @Override
    public String getSignedUrl(String bucket, String key, int expirationMinutes) {
        log.debug("Génération d'URL signée via S3FileStorageAdapter -> bucket: {}, key: {}", bucket, key);
        return s3FileStorageService.getSignedUrl(bucket, key, expirationMinutes);
    }
}

