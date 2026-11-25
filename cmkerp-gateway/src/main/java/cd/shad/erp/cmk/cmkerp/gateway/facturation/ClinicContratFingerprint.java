package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/** Empreinte stable CODE|DESIGNATION pour rapprochement TCONTRAT (sans actif). */
public final class ClinicContratFingerprint {

  private ClinicContratFingerprint() {
  }

  public static String of(String code, String designation) {
    String c = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    String d = designation == null ? "" : designation.trim();
    String raw = c + "|" + d;
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(dig).substring(0, 32);
    } catch (NoSuchAlgorithmException e) {
      return Integer.toHexString(raw.hashCode());
    }
  }
}
