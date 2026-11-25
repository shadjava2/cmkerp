package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Definition;
import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Kind;

class ClinicReferentialLot2CatalogTest {

  @Test
  void lot2DefinitionsPreserveHistoricalKeys() {
    Definition services = ClinicReferentialCatalog.byKey("services").orElseThrow();
    assertEquals("TSERVICE", services.beachTable());
    assertEquals("CODE", services.keyColumn());
    assertEquals(Kind.STRING, services.keyColumnDef().kind());
    assertEquals(2, services.keyColumnDef().maxLength());

    Definition imputations = ClinicReferentialCatalog.byKey("imputations").orElseThrow();
    assertEquals("TIMPUTATION", imputations.beachTable());
    assertEquals(Kind.STRING, imputations.keyColumnDef().kind());
    assertEquals(7, imputations.keyColumnDef().maxLength());

    Definition locations = ClinicReferentialCatalog.byKey("locations").orElseThrow();
    assertEquals("TLOCATION", locations.beachTable());
    assertEquals(Kind.INT, locations.keyColumnDef().kind());
    assertTrue(locations.column("ADRESSE").isPresent());
  }

  @Test
  void imputationCodesRemainStringsInFingerprint() {
    Definition def = ClinicReferentialCatalog.byKey("imputations").orElseThrow();
    Map<String, Object> normalized = ClinicReferentialValues.normalize(
        def, Map.of("CODE", "01A", "DESIGNATION", "Test"), false, true);
    assertEquals("01A", ClinicReferentialValues.naturalKey(def, normalized));
    assertFalse(ClinicReferentialValues.fingerprint(def, normalized).isBlank());
  }

  @Test
  void locationNaturalKeyIsNumericStringWithoutAssumingMysqlId() {
    Definition def = ClinicReferentialCatalog.byKey("locations").orElseThrow();
    Map<String, Object> normalized = ClinicReferentialValues.normalize(
        def, Map.of("CODE", 3, "LIBELLE", "Site test", "ADRESSE", "Avenue"), false, true);
    assertEquals("3", ClinicReferentialValues.naturalKey(def, normalized));
    assertEquals("Site test", ClinicReferentialValues.label(def, normalized));
  }
}
