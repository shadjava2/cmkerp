package cd.shad.erp.cmk.cmkerp.stocks.gmao.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Stockage local dédié aux médias GMAO sous {@code {base}/gmao-media/}.
 */
@Service
public class GmaoMediaStorageService {

  private static final Logger log = LoggerFactory.getLogger(GmaoMediaStorageService.class);
  private static final String BUCKET = "gmao-media";

  private final Path root;

  public GmaoMediaStorageService(
      @Value("${storage.base-path:${user.home}/cmkerp-storage}") String basePath) {
    this.root = Paths.get(basePath, BUCKET).toAbsolutePath().normalize();
    try {
      Files.createDirectories(this.root);
    } catch (IOException e) {
      log.warn("GMAO media : impossible de créer le dossier {} — {}", this.root, e.getMessage());
    }
  }

  public void store(String storageKey, InputStream inputStream) {
    try {
      Path target = resolve(storageKey);
      Files.createDirectories(target.getParent());
      Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new IllegalStateException("Échec stockage média GMAO : " + storageKey, e);
    }
  }

  public InputStream open(String storageKey) {
    try {
      return Files.newInputStream(resolve(storageKey));
    } catch (IOException e) {
      throw new IllegalStateException("Média introuvable : " + storageKey, e);
    }
  }

  public void delete(String storageKey) {
    try {
      Files.deleteIfExists(resolve(storageKey));
    } catch (IOException e) {
      log.warn("GMAO media : suppression échouée {} — {}", storageKey, e.getMessage());
    }
  }

  public boolean exists(String storageKey) {
    return Files.exists(resolve(storageKey));
  }

  private Path resolve(String storageKey) {
    Path resolved = root.resolve(storageKey).normalize();
    if (!resolved.startsWith(root)) {
      throw new IllegalArgumentException("Clé de stockage invalide");
    }
    return resolved;
  }
}
