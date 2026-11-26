package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AffiliationCoverageCalculatorTest {

  @Test
  void principalUsesSocietyOnly() {
    assertEquals("CMK", AffiliationCoverageCalculator.computePec("CMK", "A", null));
    assertEquals("RAWBANK", AffiliationCoverageCalculator.computePec("RAWBANK", "A", "IGNORED"));
  }

  @Test
  void dependentUsesSocietySlashFamilyName() {
    assertEquals("CMK/MPAKA", AffiliationCoverageCalculator.computePec("CMK", "E", "MPAKA"));
    assertEquals("RAWBANK/KABONGO", AffiliationCoverageCalculator.computePec("RAWBANK", "C", "KABONGO"));
    assertEquals("BRALIMA/KALALA", AffiliationCoverageCalculator.computePec("BRALIMA", "E", "KALALA"));
  }

  @Test
  void incompleteWhenTitulaireMissing() {
    assertNull(AffiliationCoverageCalculator.computePec("CMK", "E", null));
    assertNull(AffiliationCoverageCalculator.computePec(null, "A", null));
  }

  @Test
  void principalDetection() {
    assertTrue(AffiliationCoverageCalculator.isPrincipal("A"));
    assertTrue(AffiliationCoverageCalculator.isPrincipal(null));
  }
}
