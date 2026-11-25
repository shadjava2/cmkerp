package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Catalogue des référentiels CLINIQUE Beach restants (hors TCONTRAT).
 *
 * <p>Colonnes issues du schéma Beach. {@code MOTDEPASSE} n'est jamais déclaré.
 * {@code actif} n'est pas une colonne Beach.
 */
public final class ClinicReferentialCatalog {

  public static final int FULL_COMPARE_LIMIT = 40_000;
  /** Taille max d'une sélection d'import (tarifs ~2k → lots successifs côté client). */
  public static final int IMPORT_SELECTION_LIMIT = 500;
  /** Page max pour la liste des candidats d'import. */
  public static final int IMPORT_CANDIDATES_PAGE_MAX = 500;

  public enum Mode {
    /** Création / modification ERP journalisées vers Beach (INSERT/UPDATE). */
    BIDIRECTIONAL,
    /** Import sélectif uniquement — aucune écriture SQL Server. */
    IMPORT_ONLY,
    /** Projection de lecture. Import sélectif, aucune écriture SQL Server. */
    PROJECTION
  }

  public enum Kind {
    STRING, INT, DECIMAL, BIT, DATETIME
  }

  public record Column(String name, Kind kind, int maxLength, boolean required) {
  }

  public record Definition(
      String key,
      String code,
      String label,
      String entityType,
      String beachTable,
      String permissionPrefix,
      String sidebarPermission,
      Mode mode,
      boolean outboundInsert,
      String keyColumn,
      String labelColumn,
      List<Column> columns) {

    public Definition {
      columns = List.copyOf(columns);
      if (!key.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
        throw new IllegalArgumentException("Clé référentiel invalide: " + key);
      }
      if (!beachTable.matches("[A-Z][A-Z0-9_]{0,40}")) {
        throw new IllegalArgumentException("Table Beach invalide: " + beachTable);
      }
      if (columns.isEmpty()) {
        throw new IllegalArgumentException("Colonnes absentes: " + key);
      }
      if (mode != Mode.BIDIRECTIONAL && outboundInsert) {
        throw new IllegalArgumentException("Insertion sortante interdite: " + key);
      }
      boolean keySeen = false;
      boolean labelSeen = false;
      for (Column column : columns) {
        if (!column.name().matches("[A-Z][A-Z0-9_]{0,40}")) {
          throw new IllegalArgumentException("Colonne invalide: " + column.name());
        }
        if ("MOTDEPASSE".equals(column.name()) || "ACTIF".equals(column.name())) {
          throw new IllegalArgumentException("Colonne interdite: " + column.name());
        }
        if (column.name().equals(keyColumn)) {
          keySeen = true;
        }
        if (column.name().equals(labelColumn)) {
          labelSeen = true;
        }
      }
      if (!keySeen || !labelSeen) {
        throw new IllegalArgumentException("Clé ou libellé absent: " + key);
      }
    }

    public boolean outbound() {
      return mode == Mode.BIDIRECTIONAL;
    }

    public Column keyColumnDef() {
      return columns.stream().filter(c -> c.name().equals(keyColumn)).findFirst().orElseThrow();
    }

    public Optional<Column> column(String name) {
      if (name == null) {
        return Optional.empty();
      }
      String upper = name.trim().toUpperCase(Locale.ROOT);
      return columns.stream().filter(c -> c.name().equals(upper)).findFirst();
    }

    public String qualifiedTable() {
      return "dbo." + bracket(beachTable);
    }

    public static String bracket(String ident) {
      if (ident == null || !ident.matches("[A-Z][A-Z0-9_]{0,40}")) {
        throw new IllegalArgumentException("Identifiant SQL refusé");
      }
      return "[" + ident + "]";
    }
  }

  private static final Map<String, Definition> BY_KEY = new LinkedHashMap<>();
  private static final Map<String, Definition> BY_ENTITY = new LinkedHashMap<>();
  private static final Map<String, Definition> BY_CODE = new LinkedHashMap<>();

  static {
    add(def(
        "services", "SERVICES", "Services", "TSERVICE", "FACTURATION_SERVICES",
        "SIDEBAR_FACTURATION_SERVICES", Mode.BIDIRECTIONAL, true, "CODE", "LIBELLE",
        s("CODE", 2, true), s("LIBELLE", 50, false)));
    add(def(
        "imputations", "IMPUTATIONS", "Imputations", "TIMPUTATION", "FACTURATION_IMPUTATIONS",
        "SIDEBAR_FACTURATION_IMPUTATIONS", Mode.BIDIRECTIONAL, true, "CODE", "DESIGNATION",
        s("CODE", 7, true), s("DESIGNATION", 50, false)));
    add(def(
        "locations", "LOCATIONS", "Locations", "TLOCATION", "FACTURATION_LOCATIONS",
        "SIDEBAR_FACTURATION_LOCATIONS", Mode.BIDIRECTIONAL, true, "CODE", "LIBELLE",
        i("CODE", true), s("LIBELLE", 50, false), s("ADRESSE", 50, false)));
    add(def(
        "societes", "SOCIETES", "Sociétés", "TSOCIETE", "FACTURATION_SOCIETES",
        "SIDEBAR_FACTURATION_SOCIETES", Mode.BIDIRECTIONAL, true, "CODESOC", "NOMSOC",
        s("CODESOC", 4, true), s("NOMSOC", 50, false), s("ADRESSE", 50, false),
        s("N_IMPOT", 10, false), s("NRC", 10, false), s("ID_NAT", 10, false), s("CONTRAT", 3, false)));
    add(def(
        "classes", "CLASSES", "Classes", "TCLASSE", "FACTURATION_CLASSES",
        "SIDEBAR_FACTURATION_CLASSES", Mode.BIDIRECTIONAL, true, "CODE", "LIBELLE",
        s("CODE", 2, true), s("LIBELLE", 50, false), s("SERVICE", 50, false)));
    add(def(
        "tarifs", "TARIFS", "Tarifs", "TARIF", "FACTURATION_TARIFS",
        "SIDEBAR_FACTURATION_TARIFS", Mode.BIDIRECTIONAL, true, "CODEPRES", "LIBPRES",
        s("CODEPRES", 4, true), s("LIBPRES", 50, false), s("CODESERV", 2, false),
        d("MONTANT"), d("MONTPRIVE"), s("CLASSE", 50, false), s("IMPUTATION", 7, false)));
    add(def(
        "medecins", "MEDECINS", "Médecins", "TMEDECIN", "FACTURATION_MEDECINS",
        "SIDEBAR_FACTURATION_MEDECINS", Mode.BIDIRECTIONAL, true, "CODE", "NOM",
        s("CODE", 2, true), s("NOM", 50, false), s("SPECIALITE", 50, false),
        s("CODECMK", 1, false), d("NBREPATIENT")));
    add(def(
        "ayants-droit", "AYANTS_DROIT", "Ayants droit", "TAYANTDROIT", "FACTURATION_AYANTSDROIT",
        "SIDEBAR_FACTURATION_AYANTSDROIT", Mode.BIDIRECTIONAL, true, "MATRICULE", "NOM",
        s("MATRICULE", 10, true), s("NOM", 50, false), t("DATENAIS"), s("SEXE", 1, false),
        s("NATIONALITE", 1, false), s("RELATION", 10, false), s("CODEFAM", 10, false),
        s("CODESOC", 4, false), s("CONTRAT", 3, false), t("DATSAIS"), s("SOURCE", 3, false),
        i("LOCATION", false), s("ASSURANCE", 1, false), s("COVID", 1, false)));
    add(def(
        "utilisateurs", "UTILISATEURS", "Utilisateurs", "TUTILISATEUR", "FACTURATION_UTILISATEURS",
        "SIDEBAR_FACTURATION_UTILISATEURS", Mode.BIDIRECTIONAL, false, "CODE", "NOM",
        s("CODE", 50, true), s("NOM", 50, true), s("CATEGORIE", 50, true), i("LOCATION", false)));
    add(def(
        "pharmacies", "PHARMACIES", "Pharmacies", "TPHARMACIE", "FACTURATION_PHARMACIES",
        "SIDEBAR_FACTURATION_PHARMACIES", Mode.BIDIRECTIONAL, true, "CODE", "LIBELLE",
        s("CODE", 4, true), s("LIBELLE", 50, false)));
    add(def(
        "produits", "PRODUITS", "Produits", "TPRODUIT", "FACTURATION_PRODUITS",
        "SIDEBAR_FACTURATION_PRODUITS", Mode.BIDIRECTIONAL, true, "CODE", "DESIGNATION",
        s("CODE", 10, true), s("LIBELLE", 50, false), s("FORME", 10, false), s("DOSAGE", 10, false),
        s("DESIGNATION", 50, false), b("CONTRAT2"), b("CONTRAT3"), b("CONTRAT3P"), b("CONTRAT3FO"),
        b("CONTRATA"), b("CONTRATB"), b("CONTRATC"), b("CONTRATD"), s("DCI", 50, false)));
    add(def(
        "stock", "STOCK", "Stock", "TSTOCK", "FACTURATION_STOCK",
        "SIDEBAR_FACTURATION_STOCK", Mode.PROJECTION, false, "CODE", "DESIGNATION",
        stockColumns()));
    add(def(
        "prestations", "PRESTATIONS", "Prestations", "TPRESTATION", "FACTURATION_PRESTATIONS",
        "SIDEBAR_FACTURATION_PRESTATIONS", Mode.IMPORT_ONLY, false, "IDMVT", "LIBPRES",
        i("IDMVT", true), t("DATEPRES"), d("NDATE"), s("NOM", 50, false), s("MATRICULE", 10, false),
        s("CODEPRES", 4, false), s("LIBPRES", 50, false), i("K", false), d("MONTANT"),
        s("CODEMED", 2, false), s("MEDECIN", 50, false), s("CODESERV", 2, false),
        s("CODESOC", 4, false), s("LIBSOC", 50, false), s("CONTRASOC", 4, false),
        s("CODEFAM", 10, false), s("RELATION", 10, false), s("CODECMK", 1, false),
        i("SERIE", false), s("AFACT", 1, false), t("DATESAIS"), s("SOURCE", 3, false),
        i("LOCATION", false), s("UTILISATEUR", 20, false), s("IMPUTATION", 7, false)));
    add(def(
        "medicaments", "MEDICAMENTS", "Médicaments", "TMEDICAMENT", "FACTURATION_MEDICAMENTS",
        "SIDEBAR_FACTURATION_MEDICAMENTS", Mode.IMPORT_ONLY, false, "IDM", "DESIGNATION",
        i("IDM", true), s("MATRICULE", 10, false), s("NOM", 50, false), s("CODESOC", 4, false),
        s("LIBSOC", 50, false), s("CONTRASOC", 3, false), s("CODEPROD", 10, false),
        s("DESIGNATION", 50, false), i("QTE", false), s("UNITGEST", 10, false), d("PAU"),
        d("MONTANT"), t("DATEMVT"), d("NDATE"), s("AFACT", 1, false), s("CODEMED", 2, false),
        s("MEDECIN", 50, false), s("CODESERV", 2, false), t("DATESAIS"), s("SOURCE", 3, false),
        s("CODEPHARM", 2, false), i("LOCATION", false), s("UTILISATEUR", 20, false)));
  }

  private ClinicReferentialCatalog() {
  }

  public static List<Definition> all() {
    return List.copyOf(BY_KEY.values());
  }

  public static Optional<Definition> byKey(String key) {
    if (key == null || key.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(BY_KEY.get(key.trim().toLowerCase(Locale.ROOT)));
  }

  public static Optional<Definition> byEntity(String entityType) {
    if (entityType == null || entityType.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(BY_ENTITY.get(entityType.trim().toUpperCase(Locale.ROOT)));
  }

  public static Optional<Definition> byCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(BY_CODE.get(code.trim().toUpperCase(Locale.ROOT)));
  }

  private static void add(Definition definition) {
    if (BY_KEY.put(definition.key(), definition) != null) {
      throw new IllegalStateException("Clé dupliquée: " + definition.key());
    }
    if (BY_ENTITY.put(definition.entityType(), definition) != null) {
      throw new IllegalStateException("Entité dupliquée: " + definition.entityType());
    }
    BY_CODE.put(definition.code(), definition);
  }

  private static Definition def(
      String key, String code, String label, String entity, String permission, String sidebar,
      Mode mode, boolean outboundInsert, String keyColumn, String labelColumn, Column... columns) {
    return new Definition(
        key, code, label, entity, entity, permission, sidebar, mode, outboundInsert,
        keyColumn, labelColumn, List.of(columns));
  }

  private static Definition def(
      String key, String code, String label, String entity, String permission, String sidebar,
      Mode mode, boolean outboundInsert, String keyColumn, String labelColumn, List<Column> columns) {
    return new Definition(
        key, code, label, entity, entity, permission, sidebar, mode, outboundInsert,
        keyColumn, labelColumn, columns);
  }

  private static List<Column> stockColumns() {
    List<Column> columns = new ArrayList<>();
    columns.add(s("CODE", 10, true));
    columns.add(s("DESIGNATION", 50, false));
    columns.add(s("UNITEMB", 10, false));
    columns.add(s("UNITGEST", 10, false));
    columns.add(i("STREP", false));
    columns.add(i("STCRITIQ", false));
    columns.add(i("STINV", false));
    for (int n = 1; n <= 20; n++) {
      columns.add(i("STOCK" + n, false));
    }
    columns.add(d("PAU"));
    columns.add(s("DCI", 50, false));
    return columns;
  }

  private static Column s(String name, int max, boolean required) {
    return new Column(name, Kind.STRING, max, required);
  }

  private static Column i(String name, boolean required) {
    return new Column(name, Kind.INT, 0, required);
  }

  private static Column d(String name) {
    return new Column(name, Kind.DECIMAL, 0, false);
  }

  private static Column b(String name) {
    return new Column(name, Kind.BIT, 0, false);
  }

  private static Column t(String name) {
    return new Column(name, Kind.DATETIME, 0, false);
  }
}
