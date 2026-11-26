package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Definition;

class ClinicReferentialLot4CatalogTest {

  @Test
  void lot4DefinitionsExcludePasswordsAndMatchTables() {
    Definition medecins = ClinicReferentialCatalog.byKey("medecins").orElseThrow();
    assertEquals("TMEDECIN", medecins.beachTable());
    assertTrue(medecins.column("SPECIALITE").isPresent());

    Definition ayants = ClinicReferentialCatalog.byKey("ayants-droit").orElseThrow();
    assertEquals("TAYANTDROIT", ayants.beachTable());
    assertEquals("MATRICULE", ayants.keyColumn());
    assertFalse(ayants.column("MOTDEPASSE").isPresent());

    Definition users = ClinicReferentialCatalog.byKey("utilisateurs").orElseThrow();
    assertEquals("TUTILISATEUR", users.beachTable());
    assertFalse(users.outboundInsert());
    assertFalse(users.column("MOTDEPASSE").isPresent());
  }

  @Test
  void ayantDroitNaturalKeyIsMatriculeString() {
    Definition def = ClinicReferentialCatalog.byKey("ayants-droit").orElseThrow();
    Map<String, Object> normalized = ClinicReferentialValues.normalize(
        def,
        Map.of("MATRICULE", "18PP05740", "NOM", "PANUKA", "SEXE", "M", "CODESOC", "0419"),
        false,
        true);
    assertEquals("18PP05740", ClinicReferentialValues.naturalKey(def, normalized));
  }

  @Test
  void ayantDroitAcceptsDateOnlyBirthDate() {
    Definition def = ClinicReferentialCatalog.byKey("ayants-droit").orElseThrow();
    Map<String, Object> normalized = ClinicReferentialValues.normalize(
        def,
        Map.of(
            "MATRICULE", "26PP07848",
            "NOM", "TEST",
            "DATENAIS", "2024-11-01",
            "DATSAIS", "2026-11-26T08:13:46",
            "SEXE", "M"),
        false,
        false);
    assertEquals("2024-11-01T00:00:00", normalized.get("DATENAIS"));
    assertEquals("2026-11-26T08:13:46", normalized.get("DATSAIS"));
  }

  @Test
  void supervisionSkipsDetailedCompareForVeryLargeReferentials() {
    assertTrue(ClinicReferentialCatalog.SUPERVISION_DETAILED_COMPARE_LIMIT < 30_000);
    assertTrue(ClinicReferentialCatalog.SUPERVISION_DETAILED_COMPARE_LIMIT >= 1000);
  }
}
