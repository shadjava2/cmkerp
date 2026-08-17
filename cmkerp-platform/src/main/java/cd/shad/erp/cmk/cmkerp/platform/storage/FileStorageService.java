package cd.shad.erp.cmk.cmkerp.platform.storage;

import java.io.InputStream;

/**
 * Interface pour le stockage de fichiers (abstraction S3/MinIO).
 *
 * <p>
 * Permet de basculer entre un stockage local (dev) et un stockage objet distribué (prod)
 * sans changer le code métier.
 *

 */
public interface FileStorageService {

    /**
     * Upload un fichier.
     *
     * @param bucket le bucket/conteneur
     * @param key la clé (chemin) du fichier
     * @param inputStream le flux du fichier
     * @return l'URL publique du fichier (si disponible)
     */
    String upload(String bucket, String key, InputStream inputStream);

    /**
     * Télécharge un fichier.
     *
     * @param bucket le bucket/conteneur
     * @param key la clé (chemin) du fichier
     * @return le flux du fichier
     */
    InputStream download(String bucket, String key);

    /**
     * Supprime un fichier.
     *
     * @param bucket le bucket/conteneur
     * @param key la clé (chemin) du fichier
     */
    void delete(String bucket, String key);

    /**
     * Vérifie si un fichier existe.
     *
     * @param bucket le bucket/conteneur
     * @param key la clé (chemin) du fichier
     * @return true si le fichier existe
     */
    boolean exists(String bucket, String key);

    /**
     * Génère une URL signée (pour accès temporaire).
     *
     * @param bucket le bucket/conteneur
     * @param key la clé (chemin) du fichier
     * @param expirationMinutes durée de validité en minutes
     * @return l'URL signée
     */
    String getSignedUrl(String bucket, String key, int expirationMinutes);
}

