package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Definition;

class ClinicReferentialLot3CatalogTest {

  @Test
  void lot3DefinitionsMatchHistoricalTables() {
    Definition societes = ClinicReferentialCatalog.byKey("societes").orElseThrow();
    assertEquals("TSOCIETE", societes.beachTable());
    assertEquals("CODESOC", societes.keyColumn());
    assertTrue(societes.column("CONTRAT").isPresent());

    Definition classes = ClinicReferentialCatalog.byKey("classes").orElseThrow();
    assertEquals("TCLASSE", classes.beachTable());
    assertTrue(classes.column("SERVICE").isPresent());

    Definition tarifs = ClinicReferentialCatalog.byKey("tarifs").orElseThrow();
    assertEquals("TARIF", tarifs.beachTable());
    assertEquals("CODEPRES", tarifs.keyColumn());
    assertTrue(tarifs.column("MONTANT").isPresent());
    assertTrue(tarifs.column("IMPUTATION").isPresent());
  }

  @Test
  void tarifMontantsStayNumericInFingerprint() {
    Definition def = ClinicReferentialCatalog.byKey("tarifs").orElseThrow();
    Map<String, Object> normalized = ClinicReferentialValues.normalize(
        def,
        Map.of(
            "CODEPRES", "001",
            "LIBPRES", "CONSULT. GENERALISTE",
            "CODESERV", "01",
            "MONTANT", 15,
            "MONTPRIVE", 0,
            "IMPUTATION", "7061010"),
        false,
        true);
    assertEquals("001", ClinicReferentialValues.naturalKey(def, normalized));
    assertEquals("CONSULT. GENERALISTE", ClinicReferentialValues.label(def, normalized));
  }

  @Test
  void importSelectionLimitSupportsLargeTarifBatches() {
    assertTrue(ClinicReferentialCatalog.IMPORT_SELECTION_LIMIT >= 500);
    assertTrue(ClinicReferentialCatalog.IMPORT_CANDIDATES_PAGE_MAX >= 500);
  }
}
