package cd.shad.erp.cmk.cmkerp.platform.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Implémentation locale du stockage de fichiers (pour développement).
 *
 * <p>
 * Stocke les fichiers sur le système de fichiers local.
 * Utilisé uniquement en développement.
 *
 * <p>
 * Configuration :
 * <pre>{@code
 * storage:
 *   type: local
 *   base-path: ${STORAGE_BASE_PATH:/tmp/cmkerp-storage}
 * }</pre>
 *

 */
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    @Value("${storage.base-path:/tmp/cmkerp-storage}")
    private String basePath;

    @Override
    public String upload(String bucket, String key, InputStream inputStream) {
        try {
            Path filePath = Paths.get(basePath, bucket, key);
            Files.createDirectories(filePath.getParent());
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Fichier uploadé : {}", filePath);
            return filePath.toUri().toString();
        } catch (IOException e) {
            log.error("Erreur lors de l'upload du fichier {}/{}", bucket, key, e);
            throw new RuntimeException("Erreur lors de l'upload du fichier", e);
        }
    }

    @Override
    public InputStream download(String bucket, String key) {
        try {
            Path filePath = Paths.get(basePath, bucket, key);
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            log.error("Erreur lors du téléchargement du fichier {}/{}", bucket, key, e);
            throw new RuntimeException("Fichier non trouvé", e);
        }
    }

    @Override
    public void delete(String bucket, String key) {
        try {
            Path filePath = Paths.get(basePath, bucket, key);
            Files.deleteIfExists(filePath);
            log.debug("Fichier supprimé : {}", filePath);
        } catch (IOException e) {
            log.error("Erreur lors de la suppression du fichier {}/{}", bucket, key, e);
        }
    }

    @Override
    public boolean exists(String bucket, String key) {
        Path filePath = Paths.get(basePath, bucket, key);
        return Files.exists(filePath);
    }

    @Override
    public String getSignedUrl(String bucket, String key, int expirationMinutes) {
        // Pour le stockage local, retourner l'URL du fichier
        Path filePath = Paths.get(basePath, bucket, key);
        return filePath.toUri().toString();
    }
}

