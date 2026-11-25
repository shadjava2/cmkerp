package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class ClinicContratFingerprintTest {

  @Test
  void sameValuesSameFingerprint() {
    assertEquals(
        ClinicContratFingerprint.of("A", "Contrat A"),
        ClinicContratFingerprint.of(" a ", "Contrat A"));
  }

  @Test
  void designationChangeChangesFingerprint() {
    assertNotEquals(
        ClinicContratFingerprint.of("A", "Contrat A"),
        ClinicContratFingerprint.of("A", "Contrat B"));
  }
}
