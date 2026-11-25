package cd.shad.erp.cmk.cmkerp.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.SystemRoleConstants;

/**
 * Applique le socle RBAC hiérarchique lorsque Flyway est désactivé (base legacy).
 *
 * <p>Idempotent : colonnes, catalogue MODULE_/SIDEBAR_, sync ADMIN.
 */
@Component
@Order(55)
public class RbacSchemaInitializer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(RbacSchemaInitializer.class);

  private final JdbcTemplate jdbc;

  public RbacSchemaInitializer(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!tableExists("permissions") || !tableExists("roles_permissions")) {
      log.warn("RBAC : tables permissions/roles_permissions absentes — initialisation ignorée");
      return;
    }
    try {
      ensureColumn("permissions", "parent", "BIGINT NULL DEFAULT 0");
      ensureColumn("roles_permissions", "parent", "BIGINT NULL DEFAULT NULL");
      ensureUniqueRolePermission();
      seedCatalog();
      seedPhase2Granular();
      syncAdmin();
      ensureAdminProtectTrigger();
      ensurePermissionFkCascade();
      log.info("RBAC : schéma hiérarchique vérifié / synchronisé");
    } catch (Exception ex) {
      log.warn("RBAC : initialisation partielle — {}", ex.getMessage());
    }
  }

  private void seedCatalog() {
    Object[][] roots = {
        {436L, "MODULE_ADMINISTRATION", "Module Administration", 0L},
        {437L, "MODULE_PILOTAGE_STOCK", "Module Pilotage du stock", 0L},
        {438L, "MODULE_STOCK", "Module Stock et mouvements", 0L},
        {439L, "MODULE_GMAO", "Module GMAO", 0L},
        {440L, "MODULE_DMI", "Module Dossier médical informatisé", 0L},
    };
    for (Object[] row : roots) {
      upsertPermission((Long) row[0], (String) row[1], (String) row[2], (Long) row[3]);
    }

    Long dmiId = findPermissionId("MODULE_DMI");
    if (dmiId == null) {
      return;
    }
    Object[][] sidebars = {
        {441L, "SIDEBAR_ACCUEIL", "Menu Accueil DMI"},
        {442L, "SIDEBAR_RECEPTION", "Menu Réception DMI"},
        {443L, "SIDEBAR_CARDEX", "Menu Cardex DMI"},
        {444L, "SIDEBAR_TRIAGE", "Menu Triage DMI"},
        {445L, "SIDEBAR_CABINET", "Menu Cabinet médical DMI"},
        {446L, "SIDEBAR_URGENCES", "Menu Urgences DMI"},
        {447L, "SIDEBAR_HOSPITALISATION", "Menu Hospitalisation DMI"},
        {448L, "SIDEBAR_SOINSRAPPROCHE", "Menu Soins rapprochés DMI"},
        {449L, "SIDEBAR_PRELEVEMENT", "Menu Prélèvement DMI"},
        {450L, "SIDEBAR_PHARMACIE", "Menu Pharmacie DMI"},
        {451L, "SIDEBAR_FACTURATION", "Menu Facturation DMI"},
    };
    for (Object[] row : sidebars) {
      upsertPermission((Long) row[0], (String) row[1], (String) row[2], dmiId);
    }

    String[][] children = {
        {"DMI_ACCUEIL_VIEW", "Consulter l'accueil DMI", "SIDEBAR_ACCUEIL"},
        {"DMI_RECEPTION_PATIENT_VIEW", "Consulter les patients (réception)", "SIDEBAR_RECEPTION"},
        {"DMI_RECEPTION_PATIENT_CREATE", "Enregistrer un patient", "SIDEBAR_RECEPTION"},
        {"DMI_RECEPTION_PATIENT_UPDATE", "Modifier un patient", "SIDEBAR_RECEPTION"},
        {"DMI_CARDEX_VIEW", "Consulter le cardex", "SIDEBAR_CARDEX"},
        {"DMI_TRIAGE_VIEW", "Accéder au triage", "SIDEBAR_TRIAGE"},
        {"DMI_CABINET_PATIENT_VIEW", "Consulter le dossier patient", "SIDEBAR_CABINET"},
        {"DMI_CABINET_CONSULTATION_CREATE", "Créer une consultation", "SIDEBAR_CABINET"},
        {"DMI_CABINET_PRESCRIPTION_CREATE", "Prescrire un médicament", "SIDEBAR_CABINET"},
        {"DMI_CABINET_CONSULTATION_CANCEL", "Annuler une consultation", "SIDEBAR_CABINET"},
        {"DMI_URGENCES_VIEW", "Accéder aux urgences", "SIDEBAR_URGENCES"},
        {"DMI_HOSPITALISATION_VIEW", "Accéder à l'hospitalisation", "SIDEBAR_HOSPITALISATION"},
        {"DMI_SOINSRAPPROCHE_VIEW", "Accéder aux soins rapprochés", "SIDEBAR_SOINSRAPPROCHE"},
        {"DMI_PRELEVEMENT_VIEW", "Accéder aux prélèvements", "SIDEBAR_PRELEVEMENT"},
        {"DMI_PHARMACIE_VIEW", "Accéder à la pharmacie DMI", "SIDEBAR_PHARMACIE"},
        {"DMI_FACTURATION_VIEW", "Consulter la facturation des soins", "SIDEBAR_FACTURATION"},
        {"ADMIN_USERS_VIEW", "Consulter les utilisateurs", "MODULE_ADMINISTRATION"},
        {"ADMIN_USERS_CREATE", "Créer un utilisateur", "MODULE_ADMINISTRATION"},
        {"ADMIN_USERS_UPDATE", "Modifier un utilisateur", "MODULE_ADMINISTRATION"},
        {"ADMIN_ROLES_MANAGE", "Gérer les rôles et permissions", "MODULE_ADMINISTRATION"},
    };
    for (String[] child : children) {
      Long parentId = findPermissionId(child[2]);
      if (parentId == null) {
        continue;
      }
      if (findPermissionId(child[0]) == null) {
        jdbc.update(
            "INSERT INTO permissions (nom, description, parent, datecreate, dateupdate) VALUES (?,?,?,NOW(),NOW())",
            child[0], child[1], parentId);
      }
    }
  }

  private void upsertPermission(Long id, String nom, String description, Long parent) {
    Long existing = findPermissionId(nom);
    if (existing != null) {
      jdbc.update(
          "UPDATE permissions SET description = ?, parent = ?, dateupdate = NOW() WHERE nom = ?",
          description, parent, nom);
      return;
    }
    try {
      jdbc.update(
          "INSERT INTO permissions (id, nom, description, parent, datecreate, dateupdate) VALUES (?,?,?,?,NOW(),NOW())",
          id, nom, description, parent);
    } catch (Exception ex) {
      jdbc.update(
          "INSERT INTO permissions (nom, description, parent, datecreate, dateupdate) VALUES (?,?,?,NOW(),NOW())",
          nom, description, parent);
    }
  }

  private void seedPhase2Granular() {
    String[][] rows = {
        {"STOCK_VIEW", "Consulter le module stock", "MODULE_STOCK"},
        {"STOCK_PRODUIT_VIEW", "Consulter les produits", "MODULE_STOCK"},
        {"STOCK_PRODUIT_CREATE", "Créer un produit", "MODULE_STOCK"},
        {"STOCK_PRODUIT_UPDATE", "Modifier un produit", "MODULE_STOCK"},
        {"STOCK_PRODUIT_DELETE", "Supprimer un produit", "MODULE_STOCK"},
        {"STOCK_PRODUIT_FUSION", "Fusionner des produits CLINIQUE", "MODULE_STOCK"},
        {"STOCK_PRIX_AJUSTER", "Ajuster les prix / marges", "MODULE_STOCK"},
        {"STOCK_FACTURATION_SYNC", "Synchroniser facturation Beach↔CH", "MODULE_STOCK"},
        {"STOCK_APPROV_VIEW", "Consulter les approvisionnements", "MODULE_STOCK"},
        {"STOCK_APPROV_CREATE", "Créer un approvisionnement", "MODULE_STOCK"},
        {"STOCK_APPROV_UPDATE", "Modifier un approvisionnement", "MODULE_STOCK"},
        {"STOCK_APPROV_CANCEL", "Annuler un approvisionnement", "MODULE_STOCK"},
        {"STOCK_TRANSFERT_VIEW", "Consulter les transferts", "MODULE_STOCK"},
        {"STOCK_TRANSFERT_CREATE", "Créer un transfert", "MODULE_STOCK"},
        {"STOCK_TRANSFERT_VALIDATE", "Valider un transfert", "MODULE_STOCK"},
        {"STOCK_REQUISITION_VIEW", "Consulter les réquisitions", "MODULE_STOCK"},
        {"STOCK_REQUISITION_CREATE", "Créer une réquisition", "MODULE_STOCK"},
        {"STOCK_TRANSFERT_INTERNE", "Gérer les transferts internes", "MODULE_STOCK"},
        {"STOCK_RECEPTION_TI", "Réceptionner un transfert interne", "MODULE_STOCK"},
        {"STOCK_INVENTAIRE_VIEW", "Consulter les inventaires", "MODULE_STOCK"},
        {"STOCK_INVENTAIRE_CREATE", "Créer un inventaire", "MODULE_STOCK"},
        {"STOCK_VENTE_VIEW", "Consulter les ventes / sorties", "MODULE_STOCK"},
        {"STOCK_VENTE_CREATE", "Créer une vente / sortie usage", "MODULE_STOCK"},
        {"STOCK_FOURNISSEUR_VIEW", "Consulter les fournisseurs", "MODULE_STOCK"},
        {"STOCK_PERIMABLE_VIEW", "Consulter les alertes péremption", "MODULE_STOCK"},
        {"STOCK_ORDONNANCE_VIEW", "Consulter les ordonnances stock", "MODULE_STOCK"},
        {"STOCK_POINT_APPEL", "Gérer le point d'appel", "MODULE_STOCK"},
        {"STOCK_DASHBOARD_VIEW", "Consulter le dashboard stock", "MODULE_STOCK"},
        {"STOCK_REPORT_EXPORT", "Exporter / imprimer rapports stock", "MODULE_STOCK"},
        {"STOCK_AUTORISATION_DECIDE", "Décider les autorisations opérations", "MODULE_STOCK"},
        {"PILOTAGE_VIEW", "Consulter le pilotage stock", "MODULE_PILOTAGE_STOCK"},
        {"PILOTAGE_REPORT_RUN", "Lancer les rapports pilotage", "MODULE_PILOTAGE_STOCK"},
        {"GMAO_VIEW", "Consulter le module GMAO", "MODULE_GMAO"},
        {"GMAO_EQUIPEMENT_VIEW", "Consulter les équipements", "MODULE_GMAO"},
        {"GMAO_EQUIPEMENT_CREATE", "Créer un équipement", "MODULE_GMAO"},
        {"GMAO_EQUIPEMENT_UPDATE", "Modifier un équipement", "MODULE_GMAO"},
        {"GMAO_EQUIPEMENT_DELETE", "Supprimer un équipement", "MODULE_GMAO"},
        {"GMAO_INTERVENTION_VIEW", "Consulter les interventions", "MODULE_GMAO"},
        {"GMAO_INTERVENTION_CREATE", "Créer une intervention", "MODULE_GMAO"},
        {"GMAO_PLAN_VIEW", "Consulter les plans préventifs", "MODULE_GMAO"},
        {"GMAO_INVENTAIRE_VIEW", "Consulter les inventaires GMAO", "MODULE_GMAO"},
        {"ADMIN_USAGE_VIEW", "Consulter l'historique d'utilisation", "MODULE_ADMINISTRATION"},
        {"OUVRIR_MODULE_GMAO", "Alias legacy ouvrir GMAO", "MODULE_GMAO"},
    };
    for (String[] row : rows) {
      if (findPermissionId(row[0]) != null) {
        continue;
      }
      Long parentId = findPermissionId(row[2]);
      if (parentId == null) {
        continue;
      }
      jdbc.update(
          "INSERT INTO permissions (nom, description, parent, datecreate, dateupdate) VALUES (?,?,?,NOW(),NOW())",
          row[0], row[1], parentId);
    }
  }

  private void ensurePermissionFkCascade() {
    try {
      Integer count = jdbc.queryForObject(
          """
              SELECT COUNT(*) FROM information_schema.table_constraints
              WHERE table_schema = DATABASE()
                AND table_name = 'roles_permissions'
                AND constraint_name = 'fk_roles_permissions_permission'
                AND constraint_type = 'FOREIGN KEY'
              """,
          Integer.class);
      if (count != null && count == 0) {
        jdbc.execute(
            """
                ALTER TABLE roles_permissions
                  ADD CONSTRAINT fk_roles_permissions_permission
                  FOREIGN KEY (fkPermission) REFERENCES permissions(id)
                  ON DELETE CASCADE
                """);
        log.info("RBAC : FK ON DELETE CASCADE roles_permissions.fkPermission ajoutée");
      }
    } catch (Exception ex) {
      log.debug("RBAC : FK cascade — {}", ex.getMessage());
    }
  }

  private void syncAdmin() {
    if (!roleExists(SystemRoleConstants.ADMIN_ROLE_ID)) {
      log.warn("RBAC : rôle ADMIN id={} introuvable — sync ignorée", SystemRoleConstants.ADMIN_ROLE_ID);
      return;
    }
    int added = jdbc.update(
        """
            INSERT IGNORE INTO roles_permissions (fkRole, fkPermission, datecreate, dateupdate)
            SELECT ?, p.id, NOW(), NOW()
            FROM permissions p
            WHERE NOT EXISTS (
              SELECT 1 FROM roles_permissions rp
              WHERE rp.fkRole = ? AND rp.fkPermission = p.id
            )
            """,
        SystemRoleConstants.ADMIN_ROLE_ID, SystemRoleConstants.ADMIN_ROLE_ID);
    if (added > 0) {
      log.info("RBAC : {} permission(s) synchronisée(s) vers ADMIN", added);
    }
  }

  private void ensureAdminProtectTrigger() {
    try {
      jdbc.execute("DROP TRIGGER IF EXISTS trg_roles_permissions_before_delete_protect_admin");
      jdbc.execute(
          """
              CREATE TRIGGER trg_roles_permissions_before_delete_protect_admin
              BEFORE DELETE ON roles_permissions
              FOR EACH ROW
              BEGIN
                IF OLD.fkRole = 5
                   AND IFNULL(@cmk_rbac_bypass_admin_protect, 0) = 0
                   AND EXISTS (SELECT 1 FROM permissions WHERE id = OLD.fkPermission) THEN
                  SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'Le rôle système ADMIN (id=5) ne peut pas perdre une permission existante';
                END IF;
              END
              """);
    } catch (Exception ex) {
      log.debug("RBAC : trigger protect ADMIN non appliqué — {}", ex.getMessage());
    }
    try {
      jdbc.execute("DROP TRIGGER IF EXISTS trg_permissions_after_insert_sync_admin");
      jdbc.execute(
          """
              CREATE TRIGGER trg_permissions_after_insert_sync_admin
              AFTER INSERT ON permissions
              FOR EACH ROW
              BEGIN
                INSERT IGNORE INTO roles_permissions (fkRole, fkPermission, datecreate, dateupdate)
                VALUES (5, NEW.id, NOW(), NOW());
              END
              """);
    } catch (Exception ex) {
      log.debug("RBAC : trigger sync ADMIN insert non appliqué — {}", ex.getMessage());
    }
  }

  private void ensureUniqueRolePermission() {
    try {
      Integer count = jdbc.queryForObject(
          """
              SELECT COUNT(*) FROM information_schema.statistics
              WHERE table_schema = DATABASE()
                AND table_name = 'roles_permissions'
                AND index_name = 'uk_roles_permissions_role_perm'
              """,
          Integer.class);
      if (count != null && count == 0) {
        jdbc.execute(
            "ALTER TABLE roles_permissions ADD UNIQUE INDEX uk_roles_permissions_role_perm (fkRole, fkPermission)");
      }
    } catch (Exception ex) {
      log.debug("RBAC : index unique roles_permissions — {}", ex.getMessage());
    }
  }

  private void ensureColumn(String table, String column, String definition) {
    if (columnExists(table, column)) {
      return;
    }
    jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    log.info("RBAC : colonne {}.{} ajoutée", table, column);
  }

  private boolean columnExists(String table, String column) {
    Integer count = jdbc.queryForObject(
        """
            SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
            """,
        Integer.class, table, column);
    return count != null && count > 0;
  }

  private boolean tableExists(String table) {
    Integer count = jdbc.queryForObject(
        """
            SELECT COUNT(*) FROM information_schema.tables
            WHERE table_schema = DATABASE() AND table_name = ?
            """,
        Integer.class, table);
    return count != null && count > 0;
  }

  private boolean roleExists(long roleId) {
    Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM roles WHERE id = ?", Integer.class, roleId);
    return count != null && count > 0;
  }

  private Long findPermissionId(String nom) {
    try {
      return jdbc.queryForObject("SELECT id FROM permissions WHERE nom = ? LIMIT 1", Long.class, nom);
    } catch (Exception ex) {
      return null;
    }
  }
}
