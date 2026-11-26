package cd.shad.erp.cmk.cmkerp.gateway.mediline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import cd.shad.erp.cmk.cmkerp.gateway.mediline.PatientIdentityMatcher.IdentityParts;
import cd.shad.erp.cmk.cmkerp.gateway.mediline.PatientIdentityMatcher.MatchScore;

class PatientIdentityMatcherTest {

  @Test
  void exactIdentityScoresHigh() {
    IdentityParts a = parts("MPAKA", "MABI", "SCHADRACK", "M", "1990-04-15", null);
    IdentityParts b = parts("MPAKA", "MABI", "SCHADRACK", "M", "1990-04-15", null);
    MatchScore score = PatientIdentityMatcher.score(a, b);
    assertTrue(score.scorePercent() >= 95);
    assertEquals("IDENTICAL_LIKELY", score.riskBand());
  }

  @Test
  void invertedNomPostnomDetected() {
    IdentityParts a = parts("MPAKA", "MABI", "SCHADRACK", "M", "1990-04-15", null);
    IdentityParts b = parts("MABI", "MPAKA", "SCHADRACK", "M", "1990-04-15", null);
    int identity = PatientIdentityMatcher.bestIdentityScore(a, b);
    assertTrue(identity >= 95, "identity=" + identity);
    MatchScore score = PatientIdentityMatcher.score(a, b);
    assertTrue(score.reasons().stream().anyMatch(r -> r.toLowerCase().contains("inversion")));
  }

  @Test
  void invertedPrenomNomDetected() {
    IdentityParts a = parts("MPAKA", "MABI", "SCHADRACK", "M", "1990-04-15", null);
    IdentityParts b = parts("SCHADRACK", "MABI", "MPAKA", "M", "1990-04-15", null);
    assertTrue(PatientIdentityMatcher.bestIdentityScore(a, b) >= 90);
  }

  @Test
  void spellingVariantStillSimilar() {
    IdentityParts a = parts("MPAKA", "MABI", "SCHADRACK", "M", "1990-04-15", null);
    IdentityParts b = parts("MPAKA", "MABY", "SCHADRACK", "M", "1990-04-15", null);
    int identity = PatientIdentityMatcher.bestIdentityScore(a, b);
    assertTrue(identity >= 70, "identity=" + identity);
  }

  @Test
  void incompletePrenomDetected() {
    IdentityParts a = parts("MPAKA", "MABI", "SCHADRACK", "M", "1990-04-15", null);
    IdentityParts b = parts("MPAKA", "MABI", "SCHAD", "M", "1990-04-15", null);
    int identity = PatientIdentityMatcher.bestIdentityScore(a, b);
    assertTrue(identity >= 70, "identity=" + identity);
  }

  @Test
  void missingPostnomStillMatches() {
    IdentityParts a = parts("MPAKA", "MABI", "SCHADRACK", "M", "1990-04-15", null);
    IdentityParts b = parts("MPAKA", null, "SCHADRACK", "M", "1990-04-15", null);
    int identity = PatientIdentityMatcher.bestIdentityScore(a, b);
    assertTrue(identity >= 70, "identity=" + identity);
  }

  @Test
  void differentDobReducesButDoesNotExclude() {
    IdentityParts a = parts("KABONGO", "MUTOMBO", "JEAN", "M", "1990-04-15", null);
    IdentityParts b = parts("KABONGO", "MUTOMBO", "JEAN", "M", "1991-04-15", null);
    MatchScore score = PatientIdentityMatcher.score(a, b);
    assertTrue(score.identityScore() >= 95);
    assertTrue(score.scorePercent() < 100);
    assertTrue(score.reasons().stream().anyMatch(r -> r.toLowerCase().contains("année")
        || r.toLowerCase().contains("naissance")));
  }

  @Test
  void samePhoneIsComplementaryOnly() {
    IdentityParts a = parts("KABONGO", "A", "JEAN", "M", "1990-01-01", "0812345678");
    IdentityParts b = parts("ILUNGA", "B", "PAUL", "M", "1985-02-02", "0812345678");
    MatchScore score = PatientIdentityMatcher.score(a, b);
    assertTrue(score.scorePercent() < 70, "score=" + score.scorePercent());
  }

  @Test
  void absentFieldsDoNotCountAsMatch() {
    IdentityParts a = parts("KABONGO", "MUTOMBO", "JEAN", null, null, null);
    IdentityParts b = parts("KABONGO", "MUTOMBO", "JEAN", null, null, null);
    MatchScore score = PatientIdentityMatcher.score(a, b);
    assertTrue(score.completenessPercent() < 100);
    assertTrue(score.scorePercent() >= 95);
  }

  @Test
  void typographyNormalized() {
    assertEquals(
        PatientIdentityMatcher.normalizeSpaced("Mpaka-Mabi"),
        PatientIdentityMatcher.normalizeSpaced("MPAKA MABI"));
    assertEquals(
        PatientIdentityMatcher.normalizeCompact("C.M.K."),
        PatientIdentityMatcher.normalizeCompact("CMK"));
  }

  private static IdentityParts parts(
      String nom, String postnom, String prenom, String sexe, String dob, String phone) {
    return new IdentityParts(nom, postnom, prenom, sexe, dob, phone, null, null);
  }
}
