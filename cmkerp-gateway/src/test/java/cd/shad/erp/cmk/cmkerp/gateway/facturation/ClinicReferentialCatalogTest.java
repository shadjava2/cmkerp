package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Definition;
import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Kind;
import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Mode;

class ClinicReferentialCatalogTest {

  @Test
  void catalogueExclutSecretEtBloqueSortiesInterdites() {
    assertEquals(14, ClinicReferentialCatalog.all().size());
    for (Definition def : ClinicReferentialCatalog.all()) {
      assertTrue(def.column("MOTDEPASSE").isEmpty(), def.key());
      assertTrue(def.column("ACTIF").isEmpty(), def.key());
      String select = ClinicReferentialLegacyReader.selectList(def);
      assertFalse(select.contains("*"), def.key());
      assertFalse(select.toUpperCase().contains("MOTDEPASSE"), def.key());
      if (def.mode() != Mode.BIDIRECTIONAL) {
        assertFalse(def.outbound(), def.key());
        assertFalse(def.outboundInsert(), def.key());
      }
    }

    Definition stock = ClinicReferentialCatalog.byKey("stock").orElseThrow();
    assertEquals(Mode.PROJECTION, stock.mode());
    assertTrue(stock.column("STINV").isPresent());
    assertTrue(stock.column("PAU").isPresent());
    assertEquals(20, stock.columns().stream().filter(c -> c.name().startsWith("STOCK")).count());

    Definition users = ClinicReferentialCatalog.byKey("utilisateurs").orElseThrow();
    assertTrue(users.outbound());
    assertFalse(users.outboundInsert());
    String insert = ClinicReferentialLegacyWriter.insertSql(users);
    assertFalse(insert.contains("MOTDEPASSE"));
    assertTrue(insert.contains("[NOM]"));
    assertTrue(insert.contains("[CATEGORIE]"));
    assertFalse(ClinicReferentialLegacyWriter.updateSql(users).contains("SET [CODE]"));

    assertEquals(Mode.IMPORT_ONLY, ClinicReferentialCatalog.byKey("prestations").orElseThrow().mode());
    assertEquals(Mode.IMPORT_ONLY, ClinicReferentialCatalog.byKey("medicaments").orElseThrow().mode());
    assertEquals(Kind.STRING, ClinicReferentialCatalog.byKey("classes").orElseThrow().column("SERVICE").orElseThrow().kind());
    assertEquals(Kind.INT, ClinicReferentialCatalog.byKey("locations").orElseThrow().keyColumnDef().kind());
    assertEquals(2, ClinicReferentialCatalog.byKey("services").orElseThrow().keyColumnDef().maxLength());
    assertEquals(7, ClinicReferentialCatalog.byKey("imputations").orElseThrow().keyColumnDef().maxLength());
  }

  @Test
  void empreinteStableSansActif() {
    Definition services = ClinicReferentialCatalog.byKey("services").orElseThrow();
    Map<String, Object> fields = ClinicReferentialValues.normalize(
        services, Map.of("CODE", "01", "LIBELLE", "Urgences", "actif", true), false, true);
    assertFalse(fields.containsKey("actif"));
    assertFalse(fields.containsKey("ACTIF"));
    String first = ClinicReferentialValues.fingerprint(services, fields);
    String second = ClinicReferentialValues.fingerprint(services, Map.copyOf(fields));
    assertEquals(first, second);
    assertEquals(64, first.length());
  }
}
