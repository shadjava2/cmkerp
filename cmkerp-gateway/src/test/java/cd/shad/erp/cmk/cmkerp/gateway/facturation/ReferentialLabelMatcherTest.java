package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReferentialLabelMatcherTest {

  @Test
  void exactAfterCompactNormalization() {
    assertEquals(100, ReferentialLabelMatcher.scorePercent("C.M.K.", "CMK"));
    assertEquals(100, ReferentialLabelMatcher.scorePercent("CITI GROUP", "CITIGROUP"));
    assertEquals(100, ReferentialLabelMatcher.scorePercent("BRALIMA", "Bralima"));
    assertEquals(100, ReferentialLabelMatcher.scorePercent("CFAO-MOTORS", "CFAO MOTORS"));
  }

  @Test
  void neverInventExactFromLooseSimilarity() {
    int score = ReferentialLabelMatcher.scorePercent("RAWBANK SA", "RAWBANK KINSHASA");
    assertTrue(score < 100);
    assertTrue(score > 0);
    int distant = ReferentialLabelMatcher.scorePercent("BRALIMA", "RAWBANK");
    assertEquals(0, distant);
  }

  @Test
  void levelBands() {
    assertEquals("EXACT", ReferentialLabelMatcher.levelOf(100));
    assertEquals("VERY_PROBABLE", ReferentialLabelMatcher.levelOf(95));
    assertEquals("PROBABLE", ReferentialLabelMatcher.levelOf(85));
    assertEquals("UNCERTAIN", ReferentialLabelMatcher.levelOf(70));
    assertEquals("WEAK", ReferentialLabelMatcher.levelOf(40));
    assertEquals("NONE", ReferentialLabelMatcher.levelOf(0));
  }
}
