package cd.shad.erp.cmk.cmkerp.gateway.security;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.SystemRoleConstants;

/**
 * Applique le socle RBAC hiérarchique lorsque Flyway est désactivé (base
 * legacy).
 *
 * <p>
 * Idempotent : colonnes, catalogue MODULE_/SIDEBAR_, sync ADMIN.
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
      seedPhase3AdminConsole();
      seedFunctionalCatalogSql();
      seedModuleFacturation();
      seedModuleFacturationV2();
      syncAdmin();
      ensureAdminProtectTrigger();
      ensurePermissionFkCascade();
      Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM permissions", Integer.class);
      log.info("RBAC : schéma hiérarchique vérifié / synchronisé — {} permissions en catalogue", total);
    } catch (Exception ex) {
      log.warn("RBAC : initialisation partielle — {}", ex.getMessage());
    }
  }

  private void seedCatalog() {
    Object[][] roots = {
        { 436L, "MODULE_ADMINISTRATION", "Accéder au module Administration", 0L },
        { 437L, "MODULE_PILOTAGE_STOCK", "Accéder au module Pilotage", 0L },
        { 438L, "MODULE_STOCK", "Accéder au module Stock", 0L },
        { 439L, "MODULE_GMAO", "Accéder au module GMAO", 0L },
        { 440L, "MODULE_DMI", "Accéder au module DMI", 0L },
    };
    for (Object[] row : roots) {
      upsertPermission((Long) row[0], (String) row[1], (String) row[2], (Long) row[3]);
    }

    Long dmiId = findPermissionId("MODULE_DMI");
    if (dmiId == null) {
      return;
    }
    Object[][] sidebars = {
        { 441L, "SIDEBAR_ACCUEIL", "Menu Accueil DMI" },
        { 442L, "SIDEBAR_RECEPTION", "Menu Réception DMI" },
        { 443L, "SIDEBAR_CARDEX", "Menu Cardex DMI" },
        { 444L, "SIDEBAR_TRIAGE", "Menu Triage DMI" },
        { 445L, "SIDEBAR_CABINET", "Menu Cabinet médical DMI" },
        { 446L, "SIDEBAR_URGENCES", "Menu Urgences DMI" },
        { 447L, "SIDEBAR_HOSPITALISATION", "Menu Hospitalisation DMI" },
        { 448L, "SIDEBAR_SOINSRAPPROCHE", "Menu Soins rapprochés DMI" },
        { 449L, "SIDEBAR_PRELEVEMENT", "Menu Prélèvement DMI" },
        { 450L, "SIDEBAR_PHARMACIE", "Menu Pharmacie DMI" },
        { 451L, "SIDEBAR_FACTURATION", "Menu Facturation DMI" },
    };
    for (Object[] row : sidebars) {
      upsertPermission((Long) row[0], (String) row[1], (String) row[2], dmiId);
    }

    String[][] children = {
        { "DMI_ACCUEIL_VIEW", "Consulter l'accueil DMI", "SIDEBAR_ACCUEIL" },
        { "DMI_RECEPTION_PATIENT_VIEW", "Consulter les patients (réception)", "SIDEBAR_RECEPTION" },
        { "DMI_RECEPTION_PATIENT_CREATE", "Enregistrer un patient", "SIDEBAR_RECEPTION" },
        { "DMI_RECEPTION_PATIENT_UPDATE", "Modifier un patient", "SIDEBAR_RECEPTION" },
        { "DMI_CARDEX_VIEW", "Consulter le cardex", "SIDEBAR_CARDEX" },
        { "DMI_PATIENT_DUPLICATE_DETECT", "Détecter les doublons patients avant création", "SIDEBAR_CARDEX" },
        { "DMI_PATIENT_DUPLICATE_COMPARE", "Comparer les identités patient détectées", "SIDEBAR_CARDEX" },
        { "DMI_PATIENT_DUPLICATE_USE_EXISTING", "Utiliser un dossier patient existant proposé", "SIDEBAR_CARDEX" },
        { "DMI_PATIENT_DUPLICATE_OVERRIDE", "Continuer la création malgré une correspondance forte", "SIDEBAR_CARDEX" },
        { "DMI_PATIENT_DUPLICATE_AUDIT_VIEW", "Consulter l'audit des décisions anti-doublons", "SIDEBAR_CARDEX" },
        { "DMI_TRIAGE_VIEW", "Accéder au triage", "SIDEBAR_TRIAGE" },
        { "DMI_CABINET_PATIENT_VIEW", "Consulter le dossier patient", "SIDEBAR_CABINET" },
        { "DMI_CABINET_CONSULTATION_CREATE", "Créer une consultation", "SIDEBAR_CABINET" },
        { "DMI_CABINET_PRESCRIPTION_CREATE", "Prescrire un médicament", "SIDEBAR_CABINET" },
        { "DMI_CABINET_CONSULTATION_CANCEL", "Annuler une consultation", "SIDEBAR_CABINET" },
        { "DMI_URGENCES_VIEW", "Accéder aux urgences", "SIDEBAR_URGENCES" },
        { "DMI_HOSPITALISATION_VIEW", "Accéder à l'hospitalisation", "SIDEBAR_HOSPITALISATION" },
        { "DMI_SOINSRAPPROCHE_VIEW", "Accéder aux soins rapprochés", "SIDEBAR_SOINSRAPPROCHE" },
        { "DMI_PRELEVEMENT_VIEW", "Accéder aux prélèvements", "SIDEBAR_PRELEVEMENT" },
        { "DMI_PHARMACIE_VIEW", "Accéder à la pharmacie DMI", "SIDEBAR_PHARMACIE" },
        { "DMI_FACTURATION_VIEW", "Consulter la facturation des soins", "SIDEBAR_FACTURATION" },
        { "ADMIN_USERS_VIEW", "Consulter les utilisateurs", "MODULE_ADMINISTRATION" },
        { "ADMIN_USERS_CREATE", "Créer un utilisateur", "MODULE_ADMINISTRATION" },
        { "ADMIN_USERS_UPDATE", "Modifier un utilisateur", "MODULE_ADMINISTRATION" },
        { "ADMIN_ROLES_MANAGE", "Gérer les rôles et permissions", "MODULE_ADMINISTRATION" },
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
        { "STOCK_VIEW", "Consulter le module stock", "MODULE_STOCK" },
        { "STOCK_PRODUIT_VIEW", "Consulter les produits", "MODULE_STOCK" },
        { "STOCK_PRODUIT_CREATE", "Créer un produit", "MODULE_STOCK" },
        { "STOCK_PRODUIT_UPDATE", "Modifier un produit", "MODULE_STOCK" },
        { "STOCK_PRODUIT_DELETE", "Supprimer un produit", "MODULE_STOCK" },
        { "STOCK_PRODUIT_FUSION", "Fusionner des produits CLINIQUE", "MODULE_STOCK" },
        { "STOCK_PRIX_AJUSTER", "Ajuster les prix / marges", "MODULE_STOCK" },
        { "STOCK_FACTURATION_SYNC", "Synchroniser facturation Beach↔CH", "MODULE_STOCK" },
        { "STOCK_APPROV_VIEW", "Consulter les approvisionnements", "MODULE_STOCK" },
        { "STOCK_APPROV_CREATE", "Créer un approvisionnement", "MODULE_STOCK" },
        { "STOCK_APPROV_UPDATE", "Modifier un approvisionnement", "MODULE_STOCK" },
        { "STOCK_APPROV_CANCEL", "Annuler un approvisionnement", "MODULE_STOCK" },
        { "STOCK_TRANSFERT_VIEW", "Consulter les transferts", "MODULE_STOCK" },
        { "STOCK_TRANSFERT_CREATE", "Créer un transfert", "MODULE_STOCK" },
        { "STOCK_TRANSFERT_VALIDATE", "Valider un transfert", "MODULE_STOCK" },
        { "STOCK_REQUISITION_VIEW", "Consulter les réquisitions", "MODULE_STOCK" },
        { "STOCK_REQUISITION_CREATE", "Créer une réquisition", "MODULE_STOCK" },
        { "STOCK_TRANSFERT_INTERNE", "Gérer les transferts internes", "MODULE_STOCK" },
        { "STOCK_RECEPTION_TI", "Réceptionner un transfert interne", "MODULE_STOCK" },
        { "STOCK_INVENTAIRE_VIEW", "Consulter les inventaires", "MODULE_STOCK" },
        { "STOCK_INVENTAIRE_CREATE", "Créer un inventaire", "MODULE_STOCK" },
        { "STOCK_VENTE_VIEW", "Consulter les ventes / sorties", "MODULE_STOCK" },
        { "STOCK_VENTE_CREATE", "Créer une vente / sortie usage", "MODULE_STOCK" },
        { "STOCK_FOURNISSEUR_VIEW", "Consulter les fournisseurs", "MODULE_STOCK" },
        { "STOCK_PERIMABLE_VIEW", "Consulter les alertes péremption", "MODULE_STOCK" },
        { "STOCK_ORDONNANCE_VIEW", "Consulter les ordonnances stock", "MODULE_STOCK" },
        { "STOCK_POINT_APPEL", "Gérer le point d'appel", "MODULE_STOCK" },
        { "STOCK_DASHBOARD_VIEW", "Consulter le dashboard stock", "MODULE_STOCK" },
        { "STOCK_REPORT_EXPORT", "Exporter / imprimer rapports stock", "MODULE_STOCK" },
        { "STOCK_AUTORISATION_DECIDE", "Décider les autorisations opérations", "MODULE_STOCK" },
        { "PILOTAGE_VIEW", "Consulter le pilotage stock", "MODULE_PILOTAGE_STOCK" },
        { "PILOTAGE_REPORT_RUN", "Lancer les rapports pilotage", "MODULE_PILOTAGE_STOCK" },
        { "GMAO_VIEW", "Consulter le module GMAO", "MODULE_GMAO" },
        { "GMAO_EQUIPEMENT_VIEW", "Consulter les équipements", "MODULE_GMAO" },
        { "GMAO_EQUIPEMENT_CREATE", "Créer un équipement", "MODULE_GMAO" },
        { "GMAO_EQUIPEMENT_UPDATE", "Modifier un équipement", "MODULE_GMAO" },
        { "GMAO_EQUIPEMENT_DELETE", "Supprimer un équipement", "MODULE_GMAO" },
        { "GMAO_INTERVENTION_VIEW", "Consulter les interventions", "MODULE_GMAO" },
        { "GMAO_INTERVENTION_CREATE", "Créer une intervention", "MODULE_GMAO" },
        { "GMAO_PLAN_VIEW", "Consulter les plans préventifs", "MODULE_GMAO" },
        { "GMAO_INVENTAIRE_VIEW", "Consulter les inventaires GMAO", "MODULE_GMAO" },
        { "ADMIN_USAGE_VIEW", "Consulter l'historique d'utilisation", "MODULE_ADMINISTRATION" },
        { "OUVRIR_MODULE_GMAO", "Alias legacy ouvrir GMAO", "MODULE_GMAO" },
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

  /** Permissions de gouvernance de la console Administration (V26). */
  private void seedPhase3AdminConsole() {
    String[][] rows = {
        { "ADMIN_ROLES_VIEW", "Consulter les rôles", "MODULE_ADMINISTRATION" },
        { "ADMIN_ROLES_CREATE", "Créer un rôle métier", "MODULE_ADMINISTRATION" },
        { "ADMIN_ROLES_UPDATE", "Modifier un rôle métier", "MODULE_ADMINISTRATION" },
        { "ADMIN_ROLES_DELETE", "Supprimer un rôle métier", "MODULE_ADMINISTRATION" },
        { "ADMIN_ROLES_DUPLICATE", "Dupliquer un rôle métier", "MODULE_ADMINISTRATION" },
        { "ADMIN_ROLES_PERMISSIONS_UPDATE", "Modifier les permissions d'un rôle", "MODULE_ADMINISTRATION" },
        { "ADMIN_PERMISSIONS_CATALOG_VIEW", "Consulter le catalogue des permissions", "MODULE_ADMINISTRATION" },
        { "ADMIN_USERS_ROLE_ASSIGN", "Attribuer un rôle à un utilisateur", "MODULE_ADMINISTRATION" },
        { "ADMIN_USERS_PERMISSIONS_VIEW", "Consulter les permissions effectives d'un utilisateur",
            "MODULE_ADMINISTRATION" },
        { "ADMIN_RBAC_AUDIT_VIEW", "Consulter l'audit des modifications de droits", "MODULE_ADMINISTRATION" },
        { "ADMIN_RBAC_INTEGRITY_VIEW", "Vérifier l'intégrité du rôle ADMIN", "MODULE_ADMINISTRATION" },
        { "ADMIN_RBAC_INTEGRITY_REPAIR", "Réparer la synchronisation ADMIN", "MODULE_ADMINISTRATION" },
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

  /**
   * Applique le catalogue fonctionnel exhaustif (V27) lorsque Flyway est
   * désactivé.
   * Idempotent : INSERT … WHERE NOT EXISTS.
   */
  private void seedFunctionalCatalogSql() {
    ClassPathResource resource = new ClassPathResource("db/migration/V27__rbac_functional_catalog_exhaustive.sql");
    if (!resource.exists()) {
      log.warn("RBAC : V27 catalogue fonctionnel introuvable sur le classpath");
      return;
    }
    try (Connection connection = jdbc.getDataSource().getConnection()) {
      ScriptUtils.executeSqlScript(connection, resource);
      log.info("RBAC : catalogue fonctionnel V27 appliqué / vérifié");
    } catch (Exception ex) {
      log.warn("RBAC : échec application V27 — {}", ex.getMessage());
    }
  }

  /**
   * Module Facturation autonome — racine + menu Accueil / Tableau de bord
   * (capacités ventes / sync réutilisent STOCK_*). Sync ADMIN via
   * {@link #syncAdmin()}.
   */
  private void seedModuleFacturation() {
    if (findPermissionId("MODULE_FACTURATION") == null) {
      jdbc.update(
          "INSERT INTO permissions (nom, description, parent, datecreate, dateupdate) VALUES (?,?,0,NOW(),NOW())",
          "MODULE_FACTURATION", "Accéder au module Facturation");
    } else {
      jdbc.update(
          "UPDATE permissions SET description = ?, parent = 0, dateupdate = NOW() WHERE nom = ?",
          "Accéder au module Facturation", "MODULE_FACTURATION");
    }
    Long moduleId = findPermissionId("MODULE_FACTURATION");
    if (moduleId == null) {
      return;
    }
    if (findPermissionId("SIDEBAR_FACTURATION_ACCUEIL") == null) {
      jdbc.update(
          "INSERT INTO permissions (nom, description, parent, datecreate, dateupdate) VALUES (?,?,?,NOW(),NOW())",
          "SIDEBAR_FACTURATION_ACCUEIL", "Menu Tableau de bord du module Facturation", moduleId);
    } else {
      jdbc.update(
          "UPDATE permissions SET description = ?, parent = ?, dateupdate = NOW() WHERE nom = ?",
          "Menu Tableau de bord du module Facturation", moduleId, "SIDEBAR_FACTURATION_ACCUEIL");
    }
  }

  /**
   * Facturation V2 — menus SIDEBAR_* + KPI dashboard branchés sur données
   * réelles.
   */
  private void seedModuleFacturationV2() {
    Long moduleId = findPermissionId("MODULE_FACTURATION");
    if (moduleId == null) {
      return;
    }
    String[][] sidebars = {
        { "SIDEBAR_FACTURATION_CONTRATS", "Menu Contrats du module Facturation" },
        { "SIDEBAR_FACTURATION_AYANTSDROIT", "Menu Ayants droit du module Facturation" },
        { "SIDEBAR_FACTURATION_SOCIETES", "Menu Sociétés du module Facturation" },
        { "SIDEBAR_FACTURATION_PRESTATIONS", "Menu Prestations du module Facturation" },
        { "SIDEBAR_FACTURATION_MEDICAMENTS", "Menu Médicaments du module Facturation" },
        { "SIDEBAR_FACTURATION_TARIFS", "Menu Tarifs du module Facturation" },
        { "SIDEBAR_FACTURATION_IMPUTATIONS", "Menu Imputations du module Facturation" },
        { "SIDEBAR_FACTURATION_CLASSES", "Menu Classes du module Facturation" },
        { "SIDEBAR_FACTURATION_MEDECINS", "Menu Médecins du module Facturation" },
        { "SIDEBAR_FACTURATION_SERVICES", "Menu Services du module Facturation" },
        { "SIDEBAR_FACTURATION_PHARMACIES", "Menu Pharmacies du module Facturation" },
        { "SIDEBAR_FACTURATION_PRODUITS", "Menu Produits du module Facturation" },
        { "SIDEBAR_FACTURATION_STOCK", "Menu Stock du module Facturation" },
        { "SIDEBAR_FACTURATION_UTILISATEURS", "Menu Utilisateurs du module Facturation" },
        { "SIDEBAR_FACTURATION_LOCATIONS", "Menu Locations du module Facturation" },
        { "SIDEBAR_FACTURATION_SYNCHRONISATION", "Menu Synchronisation CLINIQUE" },
    };
    for (String[] row : sidebars) {
      upsertPermission(row[0], row[1], moduleId);
    }
    Long accueilId = findPermissionId("SIDEBAR_FACTURATION_ACCUEIL");
    if (accueilId != null) {
      String[][] dash = {
          { "FACTURATION_DASHBOARD_KPI_MEDICAMENTS_VIEW", "Voir le KPI médicaments facturés (dashboard)" },
          { "FACTURATION_DASHBOARD_KPI_PRIVES_VIEW", "Voir le KPI patients privés (dashboard)" },
          { "FACTURATION_DASHBOARD_KPI_CONTRATS_USAGE_VIEW", "Voir le KPI facturation sous contrat (dashboard)" },
          { "FACTURATION_DASHBOARD_CHART_EVOLUTION_VIEW", "Voir le graphique d'évolution (dashboard)" },
          { "FACTURATION_DASHBOARD_TABLE_PRODUITS_VIEW", "Voir le tableau produits facturés (dashboard)" },
      };
      for (String[] row : dash) {
        upsertPermission(row[0], row[1], accueilId);
      }
    }
    Long contratsId = findPermissionId("SIDEBAR_FACTURATION_CONTRATS");
    if (contratsId != null) {
      String[][] contrats = {
          { "FACTURATION_CONTRATS_VIEW", "Consulter les contrats facturation" },
          { "FACTURATION_CONTRATS_CREATE", "Créer un contrat facturation" },
          { "FACTURATION_CONTRATS_UPDATE", "Modifier un contrat facturation" },
          { "FACTURATION_CONTRATS_DELETE", "Supprimer un contrat facturation (contrôlé)" },
          { "FACTURATION_CONTRATS_STATUS_UPDATE", "Activer / désactiver un contrat (ERP seulement)" },
          { "FACTURATION_CONTRATS_IMPORT", "Importer les contrats depuis CLINIQUE" },
          { "FACTURATION_CONTRATS_EXPORT", "Exporter les contrats" },
          { "FACTURATION_CONTRATS_HISTORY_VIEW", "Consulter l'historique des contrats" },
      };
      for (String[] row : contrats) {
        upsertPermission(row[0], row[1], contratsId);
      }
    }
    Long syncId = findPermissionId("SIDEBAR_FACTURATION_SYNCHRONISATION");
    if (syncId != null) {
      String[][] sync = {
          { "FACTURATION_SYNC_VIEW", "Consulter la supervision synchronisation CLINIQUE" },
          { "FACTURATION_SYNC_DRY_RUN", "Exécuter un Dry Run d'import CLINIQUE" },
          { "FACTURATION_SYNC_IMPORT", "Lancer un import CLINIQUE vers MySQL" },
          { "FACTURATION_SYNC_RETRY", "Relancer une opération de synchronisation" },
          { "FACTURATION_SYNC_CONFLICT_RESOLVE", "Résoudre un conflit de synchronisation" },
          { "FACTURATION_SYNC_HISTORY_VIEW", "Consulter l'historique de synchronisation" },
      };
      for (String[] row : sync) {
        upsertPermission(row[0], row[1], syncId);
      }
    }
    seedClinicReferentialPermissions();
  }

  private void seedClinicReferentialPermissions() {
    String[][] refs = {
        { "SIDEBAR_FACTURATION_SERVICES", "FACTURATION_SERVICES", "les services", "1" },
        { "SIDEBAR_FACTURATION_IMPUTATIONS", "FACTURATION_IMPUTATIONS", "les imputations", "1" },
        { "SIDEBAR_FACTURATION_LOCATIONS", "FACTURATION_LOCATIONS", "les locations", "1" },
        { "SIDEBAR_FACTURATION_SOCIETES", "FACTURATION_SOCIETES", "les sociétés", "1" },
        { "SIDEBAR_FACTURATION_CLASSES", "FACTURATION_CLASSES", "les classes", "1" },
        { "SIDEBAR_FACTURATION_TARIFS", "FACTURATION_TARIFS", "les tarifs", "1" },
        { "SIDEBAR_FACTURATION_MEDECINS", "FACTURATION_MEDECINS", "les médecins", "1" },
        { "SIDEBAR_FACTURATION_AYANTSDROIT", "FACTURATION_AYANTSDROIT", "les ayants droit", "1" },
        { "SIDEBAR_FACTURATION_UTILISATEURS", "FACTURATION_UTILISATEURS", "les utilisateurs", "1" },
        { "SIDEBAR_FACTURATION_PHARMACIES", "FACTURATION_PHARMACIES", "les pharmacies", "1" },
        { "SIDEBAR_FACTURATION_PRODUITS", "FACTURATION_PRODUITS", "les produits", "1" },
        { "SIDEBAR_FACTURATION_STOCK", "FACTURATION_STOCK", "le stock", "0" },
        { "SIDEBAR_FACTURATION_PRESTATIONS", "FACTURATION_PRESTATIONS", "les prestations", "0" },
        { "SIDEBAR_FACTURATION_MEDICAMENTS", "FACTURATION_MEDICAMENTS", "les médicaments", "0" },
    };
    for (String[] ref : refs) {
      Long parent = findPermissionId(ref[0]);
      if (parent == null) {
        continue;
      }
      String prefix = ref[1];
      String label = ref[2];
      upsertPermission(prefix + "_VIEW", "Consulter " + label, parent);
      upsertPermission(prefix + "_IMPORT", "Importer " + label + " depuis CLINIQUE", parent);
      upsertPermission(prefix + "_STATUS_UPDATE", "Activer ou désactiver " + label + " (ERP seulement)", parent);
      if ("1".equals(ref[3])) {
        upsertPermission(prefix + "_CREATE", "Créer " + label, parent);
        upsertPermission(prefix + "_UPDATE", "Modifier " + label, parent);
      }
    }
  }

  private void upsertPermission(String nom, String description, Long parentId) {
    if (findPermissionId(nom) == null) {
      jdbc.update(
          "INSERT INTO permissions (nom, description, parent, datecreate, dateupdate) VALUES (?,?,?,NOW(),NOW())",
          nom, description, parentId);
    } else {
      jdbc.update(
          "UPDATE permissions SET description = ?, parent = ?, dateupdate = NOW() WHERE nom = ?",
          description, parentId, nom);
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
