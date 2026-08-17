package cd.shad.erp.cmk.cmkerp.stocks.commandes.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Hash SHA-256 hex + génération de tokens / codes d'accès portail.
 */
public final class Sha256Hasher {

  private static final SecureRandom RANDOM = new SecureRandom();

  private Sha256Hasher() {}

  public static String sha256Hex(String value) {
    if (value == null) {
      return null;
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 non disponible", e);
    }
  }

  /** Token public : UUID sans tirets. */
  public static String generatePublicToken() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  /** Mot de passe temporaire portail : 8 caractères alphanumériques (hors ambiguïtés). */
  public static String generateAccessCode() {
    final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    StringBuilder sb = new StringBuilder(8);
    for (int i = 0; i < 8; i++) {
      sb.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
    }
    return sb.toString();
  }

  /** Session opaque (32 bytes hex). */
  public static String generateSessionToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }
}
