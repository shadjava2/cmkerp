package cd.shad.erp.cmk.cmkerp.stocks.gmao.config;

import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

/**
 * Crée les tables GMAO sur la base ERP legacy (sans Flyway global).
 * Assure la présence des permissions MODULE_GMAO / OUVRIR_MODULE_GMAO dans le catalogue
 * (attribution via roles_permissions / sync ADMIN — aucun hardcode utilisateur).
 * <p>
 * Ne doit jamais faire échouer le démarrage du gateway : erreurs de seed = warning uniquement.
 */
@Component
@Order(55)
public class GmaoSchemaInitializer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(GmaoSchemaInitializer.class);
  private static final String PERMISSION_GMAO = "OUVRIR_MODULE_GMAO";
  private static final String PERMISSION_MODULE_GMAO = "MODULE_GMAO";

  private final JdbcTemplate jdbcTemplate;
  private final DataSource dataSource;

  public GmaoSchemaInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
    this.jdbcTemplate = jdbcTemplate;
    this.dataSource = dataSource;
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      ensureTables();
      ensureGmaoPermissionsInCatalog();
    } catch (Exception ex) {
      log.warn("GMAO : initialisation non bloquante en échec — {}", ex.getMessage(), ex);
    }
  }

  private void ensureTables() throws Exception {
    if (!tableExists("gmao_equipement")) {
      log.info("GMAO DDL : application de sql/gmao_tables.sql");
      try (var connection = dataSource.getConnection()) {
        connection.setAutoCommit(true);
        ScriptUtils.executeSqlScript(connection, new ClassPathResource("sql/gmao_tables.sql"));
      }
      log.info("GMAO DDL : tables créées");
    }
    ensureEquipementFicheColumns();
    ensureMediaTable();
    ensureInventaireTables();
  }

  private void ensureInventaireTables() {
    if (!tableExists("gmao_inventaire_campagne")) {
      try {
        jdbcTemplate.execute("""
            CREATE TABLE gmao_inventaire_campagne (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              numero VARCHAR(40) NOT NULL,
              libelle VARCHAR(255) NOT NULL,
              date_debut DATE NOT NULL,
              date_fin_prevue DATE NULL,
              date_cloture TIMESTAMP NULL,
              statut VARCHAR(30) NOT NULL DEFAULT 'BROUILLON',
              perimetre_service VARCHAR(160) NULL,
              perimetre_categorie VARCHAR(40) NULL,
              responsable VARCHAR(160) NULL,
              notes TEXT NULL,
              datecreate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              dateupdate TIMESTAMP NULL,
              usercreateid BIGINT NULL,
              userupdateid BIGINT NULL,
              UNIQUE KEY uk_gmao_inv_campagne_numero (numero),
              INDEX idx_gmao_inv_campagne_statut (statut)
            )
            """);
        log.info("GMAO DDL : table gmao_inventaire_campagne créée");
      } catch (Exception ex) {
        log.warn("GMAO DDL : inventaire campagne — {}", ex.getMessage());
      }
    }
    if (!tableExists("gmao_inventaire_ligne") && tableExists("gmao_inventaire_campagne")) {
      try {
        jdbcTemplate.execute("""
            CREATE TABLE gmao_inventaire_ligne (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              fk_campagne BIGINT NOT NULL,
              fk_equipement BIGINT NOT NULL,
              resultat VARCHAR(30) NOT NULL DEFAULT 'A_VERIFIER',
              localisation_systeme VARCHAR(255) NULL,
              localisation_constatee VARCHAR(255) NULL,
              etat_constate VARCHAR(30) NULL,
              fonctionnement_constate VARCHAR(40) NULL,
              consommables_ok TINYINT(1) NULL,
              pieces_ok TINYINT(1) NULL,
              manuel_utilisateur_ok TINYINT(1) NULL,
              manuel_technique_ok TINYINT(1) NULL,
              accessoires_ok TINYINT(1) NULL,
              remarque TEXT NULL,
              inventoriste VARCHAR(160) NULL,
              date_controle TIMESTAMP NULL,
              ecart TINYINT(1) NOT NULL DEFAULT 0,
              datecreate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              dateupdate TIMESTAMP NULL,
              usercreateid BIGINT NULL,
              userupdateid BIGINT NULL,
              UNIQUE KEY uk_gmao_inv_ligne (fk_campagne, fk_equipement),
              INDEX idx_gmao_inv_ligne_campagne (fk_campagne),
              INDEX idx_gmao_inv_ligne_resultat (resultat),
              CONSTRAINT fk_gmao_inv_ligne_campagne
                FOREIGN KEY (fk_campagne) REFERENCES gmao_inventaire_campagne (id),
              CONSTRAINT fk_gmao_inv_ligne_equipement
                FOREIGN KEY (fk_equipement) REFERENCES gmao_equipement (id)
            )
            """);
        log.info("GMAO DDL : table gmao_inventaire_ligne créée");
      } catch (Exception ex) {
        log.warn("GMAO DDL : inventaire ligne — {}", ex.getMessage());
      }
    }
  }

  private void ensureMediaTable() {
    if (tableExists("gmao_equipement_media")) {
      return;
    }
    try {
      jdbcTemplate.execute("""
          CREATE TABLE gmao_equipement_media (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            fk_equipement BIGINT NOT NULL,
            type_media VARCHAR(30) NOT NULL DEFAULT 'PHOTO',
            nom_fichier VARCHAR(255) NOT NULL,
            nom_original VARCHAR(255) NOT NULL,
            content_type VARCHAR(120) NOT NULL,
            taille_octets BIGINT NOT NULL DEFAULT 0,
            storage_key VARCHAR(500) NOT NULL,
            legende VARCHAR(255) NULL,
            est_principal TINYINT(1) NOT NULL DEFAULT 0,
            datecreate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            usercreateid BIGINT NULL,
            INDEX idx_gmao_media_equipement (fk_equipement),
            INDEX idx_gmao_media_principal (fk_equipement, est_principal),
            CONSTRAINT fk_gmao_media_equipement
              FOREIGN KEY (fk_equipement) REFERENCES gmao_equipement (id)
          )
          """);
      log.info("GMAO DDL : table gmao_equipement_media créée");
    } catch (Exception ex) {
      log.warn("GMAO DDL : table media — {}", ex.getMessage());
    }
  }

  private void ensureEquipementFicheColumns() {
    if (!tableExists("gmao_equipement")) {
      return;
    }
    String[][] columns = {
        {"date_inventaire", "DATE NULL"},
        {"nom_inventoriste", "VARCHAR(160) NULL"},
        {"etablissement", "VARCHAR(160) NULL"},
        {"service", "VARCHAR(160) NULL"},
        {"fabricant", "VARCHAR(120) NULL"},
        {"pays_acquisition", "VARCHAR(80) NULL"},
        {"annee_fabrication", "SMALLINT NULL"},
        {"date_installation", "DATE NULL"},
        {"fournisseur", "VARCHAR(160) NULL"},
        {"fournisseur_correspondant", "VARCHAR(160) NULL"},
        {"fournisseur_telephone", "VARCHAR(60) NULL"},
        {"fournisseur_email", "VARCHAR(120) NULL"},
        {"fournisseur_adresse", "TEXT NULL"},
        {"etat_general", "VARCHAR(30) NULL"},
        {"fonctionnement", "VARCHAR(40) NULL"},
        {"contrat_maintenance", "TINYINT(1) NOT NULL DEFAULT 0"},
        {"contrat_numero", "VARCHAR(80) NULL"},
        {"contrat_echeance", "DATE NULL"},
        {"maintenance_interne", "TINYINT(1) NOT NULL DEFAULT 0"},
        {"maintenance_externe", "TINYINT(1) NOT NULL DEFAULT 0"},
        {"frequence_maintenance_jours", "INT NULL"},
        {"derniere_maintenance", "DATE NULL"},
        {"prochaine_maintenance", "DATE NULL"},
        {"technicien_responsable", "VARCHAR(160) NULL"},
        {"technicien_contact", "VARCHAR(160) NULL"},
        {"consommables_disponibles", "TINYINT(1) NOT NULL DEFAULT 0"},
        {"pieces_rechange_disponibles", "TINYINT(1) NOT NULL DEFAULT 0"},
        {"manuel_utilisateur", "TINYINT(1) NOT NULL DEFAULT 0"},
        {"manuel_technique", "TINYINT(1) NOT NULL DEFAULT 0"},
        {"accessoires_complets", "TINYINT(1) NOT NULL DEFAULT 0"},
        {"responsable_service", "VARCHAR(160) NULL"},
        {"ingenieur_biomedical", "VARCHAR(160) NULL"},
    };
    for (String[] col : columns) {
      ensureColumn("gmao_equipement", col[0], col[1]);
    }
  }

  private void ensureColumn(String table, String column, String definition) {
    if (columnExists(table, column)) {
      return;
    }
    try {
      jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
      log.info("GMAO DDL : colonne {}.{} ajoutée", table, column);
    } catch (Exception ex) {
      log.warn("GMAO DDL : impossible d'ajouter {}.{} — {}", table, column, ex.getMessage());
    }
  }

  private boolean columnExists(String tableName, String columnName) {
    Integer count = jdbcTemplate.queryForObject(
        """
            SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
            """,
        Integer.class, tableName, columnName);
    return count != null && count > 0;
  }

  /** Catalogue uniquement — pas d'attribution hardcodée à un utilisateur. */
  private void ensureGmaoPermissionsInCatalog() {
    if (!tableExists("permissions")) {
      return;
    }
    Long moduleId = findPermissionId(PERMISSION_MODULE_GMAO);
    if (moduleId == null) {
      moduleId = insertPermission(PERMISSION_MODULE_GMAO, "Module GMAO", 0L);
      if (moduleId == null) {
        moduleId = findPermissionId(PERMISSION_MODULE_GMAO);
      }
    }
    Long legacyId = findPermissionId(PERMISSION_GMAO);
    if (legacyId == null) {
      insertPermission(PERMISSION_GMAO, "Ouvrir le module GMAO", moduleId != null ? moduleId : 0L);
      legacyId = findPermissionId(PERMISSION_GMAO);
    }
    if (moduleId != null && legacyId != null && columnExists("permissions", "parent")) {
      jdbcTemplate.update("UPDATE permissions SET parent = ?, dateupdate = NOW() WHERE id = ?",
          moduleId, legacyId);
    }
    // Sync ADMIN si présent
    if (tableExists("roles_permissions") && tableExists("roles")) {
      jdbcTemplate.update(
          """
              INSERT IGNORE INTO roles_permissions (fkRole, fkPermission, datecreate, dateupdate)
              SELECT 5, p.id, NOW(), NOW() FROM permissions p
              WHERE p.nom IN (?, ?)
                AND EXISTS (SELECT 1 FROM roles WHERE id = 5)
                AND NOT EXISTS (
                  SELECT 1 FROM roles_permissions rp
                  WHERE rp.fkRole = 5 AND rp.fkPermission = p.id
                )
              """,
          PERMISSION_MODULE_GMAO, PERMISSION_GMAO);
    }
    log.info("GMAO : permissions catalogue vérifiées ({}, {})", PERMISSION_MODULE_GMAO, PERMISSION_GMAO);
  }

  private Long insertPermission(String nom, String description, Long parent) {
    boolean hasParent = columnExists("permissions", "parent");
    String sql = hasParent
        ? "INSERT INTO permissions (nom, description, parent, datecreate, dateupdate) VALUES (?, ?, ?, NOW(), NOW())"
        : "INSERT INTO permissions (nom, description, datecreate) VALUES (?, ?, NOW())";
    KeyHolder keyHolder = new GeneratedKeyHolder();
    try {
      jdbcTemplate.update(con -> {
        var ps = con.prepareStatement(sql, new String[] {"id"});
        ps.setString(1, nom);
        ps.setString(2, description);
        if (hasParent) {
          ps.setLong(3, parent == null ? 0L : parent);
        }
        return ps;
      }, keyHolder);
      Number key = keyHolder.getKey();
      if (key != null) {
        log.info("GMAO : permission {} créée (id={})", nom, key.longValue());
        return key.longValue();
      }
      log.info("GMAO : permission {} créée", nom);
    } catch (Exception ex) {
      log.debug("GMAO : insert permission {} — {}", nom, ex.getMessage());
    }
    return null;
  }

  private Long findPermissionId(String nom) {
    List<Long> ids = jdbcTemplate.query(
        "SELECT id FROM permissions WHERE nom = ? LIMIT 1",
        (rs, rowNum) -> rs.getLong("id"),
        nom);
    return ids.isEmpty() ? null : ids.get(0);
  }

  private boolean tableExists(String tableName) {
    Integer count = jdbcTemplate.queryForObject(
        """
            SELECT COUNT(*) FROM information_schema.tables
            WHERE table_schema = DATABASE() AND table_name = ?
            """,
        Integer.class, tableName);
    return count != null && count > 0;
  }
}
