package cd.shad.erp.cmk.cmkerp.platform.common.infrastructure.adapter;

import cd.shad.erp.cmk.cmkerp.platform.common.application.port.FileStoragePort;
import cd.shad.erp.cmk.cmkerp.platform.storage.LocalFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Adapter pour le stockage de fichiers local (pour développement).
 *
 * <p>Implémente le port FileStoragePort en utilisant LocalFileStorageService
 * (qui utilise le système de fichiers local).
 *
 * <p>Cet adapter permet de découpler le domaine des détails d'implémentation
 * du stockage local.
 */
@Component
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class LocalFileStorageAdapter implements FileStoragePort {

    private final LocalFileStorageService localFileStorageService;

    @Override
    public String upload(String bucket, String key, InputStream inputStream) {
        log.debug("Upload de fichier via LocalFileStorageAdapter -> bucket: {}, key: {}", bucket, key);
        return localFileStorageService.upload(bucket, key, inputStream);
    }

    @Override
    public InputStream download(String bucket, String key) {
        log.debug("Download de fichier via LocalFileStorageAdapter -> bucket: {}, key: {}", bucket, key);
        return localFileStorageService.download(bucket, key);
    }

    @Override
    public void delete(String bucket, String key) {
        log.debug("Suppression de fichier via LocalFileStorageAdapter -> bucket: {}, key: {}", bucket, key);
        localFileStorageService.delete(bucket, key);
    }

    @Override
    public boolean exists(String bucket, String key) {
        return localFileStorageService.exists(bucket, key);
    }

    @Override
    public String getSignedUrl(String bucket, String key, int expirationMinutes) {
        log.debug("Génération d'URL signée via LocalFileStorageAdapter -> bucket: {}, key: {}", bucket, key);
        return localFileStorageService.getSignedUrl(bucket, key, expirationMinutes);
    }
}

