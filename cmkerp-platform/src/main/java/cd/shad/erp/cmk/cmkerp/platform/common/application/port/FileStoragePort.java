package cd.shad.erp.cmk.cmkerp.platform.common.application.port;

import java.io.InputStream;

/**
 * Port pour le stockage de fichiers (abstraction S3/MinIO/Local).
 *
 * <p>Ce port définit le contrat de stockage de fichiers sans dépendre
 * des implémentations techniques (S3, MinIO, disque local, etc.).
 *
 * <p>Les implémentations de ce port (adapters) seront dans la couche infrastructure.
 *
 * <p>Ce port permet de :
 * <ul>
 *   <li>Découpler le domaine des systèmes de stockage</li>
 *   <li>Faciliter les tests (mocks/stubs)</li>
 *   <li>Permettre le changement d'implémentation (local → S3 → MinIO)</li>
 * </ul>
 */
public interface FileStoragePort {

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

