package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Definition;
import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Mode;

class ClinicReferentialLot5CatalogTest {

  @Test
  void lot5DefinitionsMatchModesAndTables() {
    Definition pharmacies = ClinicReferentialCatalog.byKey("pharmacies").orElseThrow();
    assertEquals("TPHARMACIE", pharmacies.beachTable());
    assertTrue(pharmacies.outboundInsert());

    Definition produits = ClinicReferentialCatalog.byKey("produits").orElseThrow();
    assertEquals("TPRODUIT", produits.beachTable());
    assertEquals("DESIGNATION", produits.labelColumn());
    assertTrue(produits.column("DCI").isPresent());

    Definition stock = ClinicReferentialCatalog.byKey("stock").orElseThrow();
    assertEquals("TSTOCK", stock.beachTable());
    assertEquals(Mode.PROJECTION, stock.mode());
    assertFalse(stock.outboundInsert());
    assertTrue(stock.column("STOCK1").isPresent());
    assertTrue(stock.column("PAU").isPresent());

    Definition medicaments = ClinicReferentialCatalog.byKey("medicaments").orElseThrow();
    assertEquals("TMEDICAMENT", medicaments.beachTable());
    assertEquals(Mode.IMPORT_ONLY, medicaments.mode());
    assertFalse(medicaments.outboundInsert());
    assertEquals("IDM", medicaments.keyColumn());
  }
}
