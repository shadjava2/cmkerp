package cd.shad.erp.cmk.cmkerp.platform.storage;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;

/**
 * Implémentation S3-compatible du stockage de fichiers (MinIO, OVH Object Storage, AWS S3).
 *
 * <p>
 * Utilise le SDK MinIO pour interagir avec tout stockage S3-compatible.
 *
 * <p>
 * Configuration :
 *
 * <pre>{@code
 * storage:
 *   type: s3
 *   endpoint: ${CMK_S3_ENDPOINT:http://localhost:9000}
 *   access-key: ${CMK_S3_ACCESS_KEY:cmkerp}
 *   secret-key: ${CMK_S3_SECRET_KEY:cmkerp}
 *   bucket: ${CMK_S3_BUCKET:cmkerp-files}
 * }</pre>
 *

 */
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

  private static final Logger log = LoggerFactory.getLogger(S3FileStorageService.class);

  @Value("${storage.endpoint:http://localhost:9000}")
  private String endpoint;

  @Value("${storage.access-key:}")
  private String accessKey;

  @Value("${storage.secret-key:}")
  private String secretKey;

  @Value("${storage.bucket:cmkerp-files}")
  private String defaultBucket;

  private MinioClient minioClient;

  /**
   * Initialise le client MinIO après construction.
   */
  @PostConstruct
  public void init() {
    try {
      this.minioClient =
          MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();

      // Créer le bucket par défaut s'il n'existe pas
      ensureBucketExists(defaultBucket);
      log.info("S3FileStorageService initialisé - endpoint: {}, bucket: {}", endpoint,
          defaultBucket);
    } catch (Exception e) {
      log.error("Erreur lors de l'initialisation du client MinIO", e);
      throw new RuntimeException("Erreur lors de l'initialisation du client MinIO", e);
    }
  }

  /**
   * S'assure que le bucket existe, le crée si nécessaire.
   */
  private void ensureBucketExists(String bucket) {
    try {
      boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
      if (!found) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        log.info("Bucket créé : {}", bucket);
      }
    } catch (Exception e) {
      log.error("Erreur lors de la vérification/création du bucket : {}", bucket, e);
      throw new RuntimeException("Erreur lors de la vérification/création du bucket", e);
    }
  }

  @Override
  public String upload(String bucket, String key, InputStream inputStream) {
    // Pour les petits fichiers, upload synchrone (comportement par défaut)
    return uploadSync(bucket, key, inputStream);
  }

  /**
   * Upload synchrone (pour petits fichiers ou compatibilité).
   */
  private String uploadSync(String bucket, String key, InputStream inputStream) {
    try {
      ensureBucketExists(bucket);

      long size = inputStream.available();
      String contentType = detectContentType(key);

      minioClient.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
          .stream(inputStream, size, -1).contentType(contentType).build());

      log.debug("Fichier uploadé : {}/{}", bucket, key);

      // Retourner l'URL du fichier
      return String.format("%s/%s/%s", endpoint, bucket, key);
    } catch (Exception e) {
      log.error("Erreur lors de l'upload du fichier {}/{}", bucket, key, e);
      throw new RuntimeException("Erreur lors de l'upload du fichier", e);
    }
  }

  /**
   * Upload asynchrone pour fichiers volumineux.
   *
   * @param bucket le bucket S3
   * @param key la clé du fichier
   * @param inputStream le flux d'entrée
   * @return CompletableFuture avec l'URL du fichier uploadé
   */
  @Async("cmkerpAsyncExecutor")
  public CompletableFuture<String> uploadAsync(String bucket, String key, InputStream inputStream) {
    try {
      String url = uploadSync(bucket, key, inputStream);
      log.info("Fichier uploadé de manière asynchrone : {}/{}", bucket, key);
      return CompletableFuture.completedFuture(url);
    } catch (Exception e) {
      log.error("Erreur lors de l'upload asynchrone du fichier {}/{}", bucket, key, e);
      CompletableFuture<String> future = new CompletableFuture<>();
      future.completeExceptionally(e);
      return future;
    }
  }

  @Override
  public InputStream download(String bucket, String key) {
    try {
      return minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());
    } catch (Exception e) {
      log.error("Erreur lors du téléchargement du fichier {}/{}", bucket, key, e);
      throw new RuntimeException("Fichier non trouvé", e);
    }
  }

  @Override
  public void delete(String bucket, String key) {
    try {
      minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
      log.debug("Fichier supprimé : {}/{}", bucket, key);
    } catch (Exception e) {
      log.error("Erreur lors de la suppression du fichier {}/{}", bucket, key, e);
      throw new RuntimeException("Erreur lors de la suppression du fichier", e);
    }
  }

  @Override
  public boolean exists(String bucket, String key) {
    try {
      minioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(key).build());
      return true;
    } catch (ErrorResponseException e) {
      if (e.errorResponse().code().equals("NoSuchKey")) {
        return false;
      }
      log.error("Erreur lors de la vérification de l'existence du fichier {}/{}", bucket, key, e);
      throw new RuntimeException("Erreur lors de la vérification de l'existence du fichier", e);
    } catch (Exception e) {
      log.error("Erreur lors de la vérification de l'existence du fichier {}/{}", bucket, key, e);
      return false;
    }
  }

  @Override
  public String getSignedUrl(String bucket, String key, int expirationMinutes) {
    try {
      return minioClient.getPresignedObjectUrl(io.minio.GetPresignedObjectUrlArgs.builder()
          .method(Method.GET).bucket(bucket).object(key).expiry(expirationMinutes * 60).build());
    } catch (Exception e) {
      log.error("Erreur lors de la génération de l'URL signée pour {}/{}", bucket, key, e);
      throw new RuntimeException("Erreur lors de la génération de l'URL signée", e);
    }
  }

  /**
   * Détecte le content type à partir de l'extension du fichier.
   */
  private String detectContentType(String key) {
    String extension = key.substring(key.lastIndexOf('.') + 1).toLowerCase();
    Map<String, String> contentTypes = Map.of("pdf", "application/pdf", "jpg", "image/jpeg", "jpeg",
        "image/jpeg", "png", "image/png", "gif", "image/gif", "txt", "text/plain", "json",
        "application/json", "xml", "application/xml");
    return contentTypes.getOrDefault(extension, "application/octet-stream");
  }
}

