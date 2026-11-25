package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Definition;
import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Mode;

class ClinicReferentialLot6CatalogTest {

  @Test
  void prestationsAreImportOnlyFromTprestation() {
    Definition prestations = ClinicReferentialCatalog.byKey("prestations").orElseThrow();
    assertEquals("TPRESTATION", prestations.beachTable());
    assertEquals(Mode.IMPORT_ONLY, prestations.mode());
    assertFalse(prestations.outboundInsert());
    assertEquals("IDMVT", prestations.keyColumn());
    assertEquals("LIBPRES", prestations.labelColumn());
  }
}
