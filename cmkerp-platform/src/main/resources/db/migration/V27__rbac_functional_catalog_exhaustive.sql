-- CMK ERP — Seed catalogue RBAC fonctionnel (idempotent)
-- Cible : cmkerp-v24prod (Flyway désactivé → appliquer manuellement ou via RbacSchemaInitializer)
-- Ne détruit pas les permissions existantes (436–451).

SET @dbname = DATABASE();

-- ---------------------------------------------------------------------------
-- Helper : INSERT si absent, parent résolu par nom
-- ---------------------------------------------------------------------------
-- Accueil DMI
INSERT INTO permissions (nom, description, parent, datecreate, dateupdate)
SELECT v.nom, v.description, COALESCE(p.id, 0), NOW(), NOW()
FROM (
  SELECT 'DMI_ACCUEIL_RECHERCHE_PATIENT' AS nom, 'Rechercher un patient depuis l''accueil' AS description, 'SIDEBAR_ACCUEIL' AS parent_nom
  UNION ALL SELECT 'DMI_ACCUEIL_RECHERCHE_AVANCEE', 'Ouvrir la recherche avancée patient', 'SIDEBAR_ACCUEIL'
  UNION ALL SELECT 'DMI_ACCUEIL_NOUVEAU_DOSSIER', 'Créer un nouveau dossier patient', 'SIDEBAR_ACCUEIL'
  UNION ALL SELECT 'DMI_ACCUEIL_CARTE_RECEPTION', 'Afficher la carte d''accès Réception', 'SIDEBAR_ACCUEIL'
  UNION ALL SELECT 'DMI_ACCUEIL_CARTE_CABINET', 'Afficher la carte d''accès Cabinet', 'SIDEBAR_ACCUEIL'
  UNION ALL SELECT 'DMI_ACCUEIL_CARTE_URGENCES', 'Afficher la carte d''accès Urgences', 'SIDEBAR_ACCUEIL'
  UNION ALL SELECT 'DMI_ACCUEIL_CARTE_HOSPITALISATION', 'Afficher la carte d''accès Hospitalisation', 'SIDEBAR_ACCUEIL'
  UNION ALL SELECT 'DMI_ACCUEIL_FILIERE_AMBULATOIRE', 'Accéder à la filière Ambulatoire', 'SIDEBAR_ACCUEIL'
  UNION ALL SELECT 'DMI_ACCUEIL_FILIERE_URGENCES', 'Accéder à la filière Urgences', 'SIDEBAR_ACCUEIL'
  UNION ALL SELECT 'DMI_ACCUEIL_FILIERE_HOSPITALISATION', 'Accéder à la filière Hospitalisation', 'SIDEBAR_ACCUEIL'
  UNION ALL SELECT 'DMI_ACCUEIL_FILIERE_DIAGNOSTICS', 'Accéder à la filière Diagnostics', 'SIDEBAR_ACCUEIL'
  UNION ALL SELECT 'DMI_ACCUEIL_FILIERE_SOINS_DOMICILE', 'Accéder à la filière Soins à domicile', 'SIDEBAR_ACCUEIL'
  UNION ALL SELECT 'DMI_ACCUEIL_FILIERE_THERAPIE_AMBULATOIRE', 'Accéder à la filière Thérapie ambulatoire', 'SIDEBAR_ACCUEIL'
  -- Cabinet
  UNION ALL SELECT 'DMI_CABINET_PRISE_EN_CHARGE_CREATE', 'Ouvrir une prise en charge ambulatoire', 'SIDEBAR_CABINET'
  UNION ALL SELECT 'DMI_CABINET_FILE_ACTIVE_VIEW', 'Consulter la file active du cabinet', 'SIDEBAR_CABINET'
  UNION ALL SELECT 'DMI_CABINET_SALLE_ATTENTE_VIEW', 'Accéder à la salle d''attente', 'SIDEBAR_CABINET'
  UNION ALL SELECT 'DMI_CABINET_RENDEZ_VOUS_VIEW', 'Accéder aux rendez-vous du cabinet', 'SIDEBAR_CABINET'
  UNION ALL SELECT 'DMI_CABINET_REGISTRE_CONSULTATIONS_VIEW', 'Consulter le registre des consultations', 'SIDEBAR_CABINET'
  UNION ALL SELECT 'DMI_CABINET_TABLE_PRISES_EN_CHARGE_VIEW', 'Afficher le tableau des prises en charge', 'SIDEBAR_CABINET'
  UNION ALL SELECT 'DMI_CABINET_FICHE_OPEN', 'Ouvrir une fiche patient depuis le cabinet', 'SIDEBAR_CABINET'
  UNION ALL SELECT 'DMI_CABINET_ACTUALISER', 'Actualiser les prises en charge', 'SIDEBAR_CABINET'
  UNION ALL SELECT 'DMI_CABINET_CONSULTATION_CREATE', 'Créer une consultation', 'SIDEBAR_CABINET'
  UNION ALL SELECT 'DMI_CABINET_PRESCRIPTION_CREATE', 'Prescrire un médicament', 'SIDEBAR_CABINET'
  UNION ALL SELECT 'DMI_CABINET_CONSULTATION_CANCEL', 'Annuler une consultation', 'SIDEBAR_CABINET'
  UNION ALL SELECT 'DMI_CABINET_PATIENT_VIEW', 'Consulter le dossier patient en cabinet', 'SIDEBAR_CABINET'
  -- Réception
  UNION ALL SELECT 'DMI_RECEPTION_PATIENT_VIEW', 'Consulter les patients (réception)', 'SIDEBAR_RECEPTION'
  UNION ALL SELECT 'DMI_RECEPTION_PATIENT_CREATE', 'Enregistrer un patient', 'SIDEBAR_RECEPTION'
  UNION ALL SELECT 'DMI_RECEPTION_PATIENT_UPDATE', 'Modifier un patient', 'SIDEBAR_RECEPTION'
  UNION ALL SELECT 'DMI_RECEPTION_NOUVEAU_DOSSIER', 'Nouveau dossier depuis la réception', 'SIDEBAR_RECEPTION'
  UNION ALL SELECT 'DMI_RECEPTION_REGISTRE_PATIENTS', 'Consulter le registre des patients', 'SIDEBAR_RECEPTION'
  UNION ALL SELECT 'DMI_RECEPTION_REGISTRE_DOSSIERS', 'Consulter le registre des dossiers', 'SIDEBAR_RECEPTION'
  UNION ALL SELECT 'DMI_RECEPTION_RENDEZ_VOUS', 'Gérer les rendez-vous réception', 'SIDEBAR_RECEPTION'
  UNION ALL SELECT 'DMI_RECEPTION_TICKET', 'Gérer les tickets de file', 'SIDEBAR_RECEPTION'
  UNION ALL SELECT 'DMI_RECEPTION_ORIENTER', 'Orienter un patient', 'SIDEBAR_RECEPTION'
  -- Cardex
  UNION ALL SELECT 'DMI_CARDEX_VIEW', 'Consulter le cardex', 'SIDEBAR_CARDEX'
  UNION ALL SELECT 'DMI_CARDEX_RECHERCHE', 'Rechercher dans le cardex', 'SIDEBAR_CARDEX'
  UNION ALL SELECT 'DMI_CARDEX_NOUVEAU_DOSSIER', 'Créer un dossier depuis le cardex', 'SIDEBAR_CARDEX'
  UNION ALL SELECT 'DMI_CARDEX_REGISTRE_PATIENTS', 'Registre patients cardex', 'SIDEBAR_CARDEX'
  -- Triage
  UNION ALL SELECT 'DMI_TRIAGE_VIEW', 'Accéder au triage', 'SIDEBAR_TRIAGE'
  UNION ALL SELECT 'DMI_TRIAGE_ENREGISTRER', 'Enregistrer le triage', 'SIDEBAR_TRIAGE'
  UNION ALL SELECT 'DMI_TRIAGE_CONSTANTES', 'Saisir les constantes', 'SIDEBAR_TRIAGE'
  UNION ALL SELECT 'DMI_TRIAGE_DISPENSAIRE', 'Actes infirmiers dispensaire', 'SIDEBAR_TRIAGE'
  -- Urgences
  UNION ALL SELECT 'DMI_URGENCES_VIEW', 'Accéder aux urgences', 'SIDEBAR_URGENCES'
  UNION ALL SELECT 'DMI_URGENCES_PRISE_EN_CHARGE_CREATE', 'Ouvrir une prise en charge urgences', 'SIDEBAR_URGENCES'
  UNION ALL SELECT 'DMI_URGENCES_FILE_ACTIVE_VIEW', 'File active urgences', 'SIDEBAR_URGENCES'
  UNION ALL SELECT 'DMI_URGENCES_FICHE_OPEN', 'Ouvrir une fiche urgences', 'SIDEBAR_URGENCES'
  UNION ALL SELECT 'DMI_URGENCES_ACTUALISER', 'Actualiser la file urgences', 'SIDEBAR_URGENCES'
  -- Hospitalisation
  UNION ALL SELECT 'DMI_HOSPITALISATION_VIEW', 'Accéder à l''hospitalisation', 'SIDEBAR_HOSPITALISATION'
  UNION ALL SELECT 'DMI_HOSPITALISATION_PRISE_EN_CHARGE_CREATE', 'Ouvrir une hospitalisation', 'SIDEBAR_HOSPITALISATION'
  UNION ALL SELECT 'DMI_HOSPITALISATION_FILE_ACTIVE_VIEW', 'File active hospitalisation', 'SIDEBAR_HOSPITALISATION'
  UNION ALL SELECT 'DMI_HOSPITALISATION_FICHE_OPEN', 'Ouvrir une fiche hospitalisation', 'SIDEBAR_HOSPITALISATION'
  -- Soins rapprochés
  UNION ALL SELECT 'DMI_SOINSRAPPROCHE_VIEW', 'Accéder aux soins rapprochés', 'SIDEBAR_SOINSRAPPROCHE'
  UNION ALL SELECT 'DMI_SOINSRAPPROCHE_ADMINISTRER', 'Tracer une administration', 'SIDEBAR_SOINSRAPPROCHE'
  UNION ALL SELECT 'DMI_SOINSRAPPROCHE_TRANSMISSION', 'Ajouter une transmission', 'SIDEBAR_SOINSRAPPROCHE'
  -- Prélèvement
  UNION ALL SELECT 'DMI_PRELEVEMENT_VIEW', 'Accéder aux prélèvements', 'SIDEBAR_PRELEVEMENT'
  UNION ALL SELECT 'DMI_PRELEVEMENT_ENREGISTRER', 'Enregistrer un prélèvement', 'SIDEBAR_PRELEVEMENT'
  UNION ALL SELECT 'DMI_PRELEVEMENT_RESULTAT', 'Transmettre un résultat', 'SIDEBAR_PRELEVEMENT'
  -- Pharmacie DMI
  UNION ALL SELECT 'DMI_PHARMACIE_VIEW', 'Accéder à la pharmacie DMI', 'SIDEBAR_PHARMACIE'
  UNION ALL SELECT 'DMI_PHARMACIE_DELIVRER', 'Confirmer une délivrance', 'SIDEBAR_PHARMACIE'
  -- Facturation
  UNION ALL SELECT 'DMI_FACTURATION_VIEW', 'Consulter la facturation des soins', 'SIDEBAR_FACTURATION'
  UNION ALL SELECT 'DMI_FACTURATION_CLOTURER', 'Clôturer une prise en charge', 'SIDEBAR_FACTURATION'
  -- Administration console
  UNION ALL SELECT 'ADMIN_USERS_VIEW', 'Consulter les utilisateurs', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_USERS_CREATE', 'Créer un utilisateur', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_USERS_UPDATE', 'Modifier un utilisateur', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_USERS_ROLE_ASSIGN', 'Attribuer un rôle à un utilisateur', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_USERS_PERMISSIONS_VIEW', 'Consulter les permissions effectives', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_ROLES_MANAGE', 'Gérer les rôles et permissions', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_ROLES_VIEW', 'Consulter les rôles', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_ROLES_CREATE', 'Créer un rôle métier', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_ROLES_UPDATE', 'Modifier un rôle métier', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_ROLES_DELETE', 'Supprimer un rôle métier', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_ROLES_DUPLICATE', 'Dupliquer un rôle métier', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_ROLES_PERMISSIONS_UPDATE', 'Modifier les permissions d''un rôle', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_PERMISSIONS_CATALOG_VIEW', 'Consulter le catalogue des permissions', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_RBAC_AUDIT_VIEW', 'Consulter l''audit des modifications de droits', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_RBAC_INTEGRITY_VIEW', 'Vérifier l''intégrité du rôle ADMIN', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_RBAC_INTEGRITY_REPAIR', 'Réparer la synchronisation ADMIN', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_USAGE_VIEW', 'Consulter l''historique d''utilisation', 'MODULE_ADMINISTRATION'
  -- Stock
  UNION ALL SELECT 'STOCK_VIEW', 'Consulter le module stock', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_PRODUIT_VIEW', 'Consulter les produits', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_PRODUIT_CREATE', 'Créer un produit', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_PRODUIT_UPDATE', 'Modifier un produit', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_PRODUIT_DELETE', 'Supprimer un produit', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_PRODUIT_FUSION', 'Fusionner des produits CLINIQUE', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_PRIX_AJUSTER', 'Ajuster les prix / marges', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_FACTURATION_SYNC', 'Synchroniser facturation Beach↔CH', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_APPROV_VIEW', 'Consulter les approvisionnements', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_APPROV_CREATE', 'Créer un approvisionnement', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_APPROV_UPDATE', 'Modifier un approvisionnement', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_APPROV_CANCEL', 'Annuler un approvisionnement', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_TRANSFERT_VIEW', 'Consulter les transferts', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_TRANSFERT_CREATE', 'Créer un transfert', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_TRANSFERT_VALIDATE', 'Valider un transfert', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_REQUISITION_VIEW', 'Consulter les réquisitions', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_REQUISITION_CREATE', 'Créer une réquisition', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_TRANSFERT_INTERNE', 'Gérer les transferts internes', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_RECEPTION_TI', 'Réceptionner un transfert interne', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_INVENTAIRE_VIEW', 'Consulter les inventaires', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_INVENTAIRE_CREATE', 'Créer un inventaire', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_VENTE_VIEW', 'Consulter les ventes / sorties', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_VENTE_CREATE', 'Créer une vente / sortie usage', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_FOURNISSEUR_VIEW', 'Consulter les fournisseurs', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_PERIMABLE_VIEW', 'Consulter les alertes péremption', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_ORDONNANCE_VIEW', 'Consulter les ordonnances stock', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_POINT_APPEL', 'Gérer le point d''appel', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_DASHBOARD_VIEW', 'Consulter le dashboard stock', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_REPORT_EXPORT', 'Exporter / imprimer rapports stock', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_AUTORISATION_DECIDE', 'Décider les autorisations opérations', 'MODULE_STOCK'
  -- Pilotage
  UNION ALL SELECT 'PILOTAGE_VIEW', 'Consulter le pilotage stock', 'MODULE_PILOTAGE_STOCK'
  UNION ALL SELECT 'PILOTAGE_REPORT_RUN', 'Lancer les rapports pilotage', 'MODULE_PILOTAGE_STOCK'
  -- GMAO
  UNION ALL SELECT 'GMAO_VIEW', 'Consulter le module GMAO', 'MODULE_GMAO'
  UNION ALL SELECT 'GMAO_EQUIPEMENT_VIEW', 'Consulter les équipements', 'MODULE_GMAO'
  UNION ALL SELECT 'GMAO_EQUIPEMENT_CREATE', 'Créer un équipement', 'MODULE_GMAO'
  UNION ALL SELECT 'GMAO_EQUIPEMENT_UPDATE', 'Modifier un équipement', 'MODULE_GMAO'
  UNION ALL SELECT 'GMAO_EQUIPEMENT_DELETE', 'Supprimer un équipement', 'MODULE_GMAO'
  UNION ALL SELECT 'GMAO_INTERVENTION_VIEW', 'Consulter les interventions', 'MODULE_GMAO'
  UNION ALL SELECT 'GMAO_INTERVENTION_CREATE', 'Créer une intervention', 'MODULE_GMAO'
  UNION ALL SELECT 'GMAO_PLAN_VIEW', 'Consulter les plans préventifs', 'MODULE_GMAO'
  UNION ALL SELECT 'GMAO_INVENTAIRE_VIEW', 'Consulter les inventaires GMAO', 'MODULE_GMAO'
  UNION ALL SELECT 'OUVRIR_MODULE_GMAO', 'Alias legacy ouvrir GMAO', 'MODULE_GMAO'
) AS v
LEFT JOIN permissions p ON p.nom = v.parent_nom
WHERE NOT EXISTS (SELECT 1 FROM permissions x WHERE x.nom = v.nom);

-- Sync ADMIN : toutes les permissions manquantes
INSERT INTO roles_permissions (fkRole, fkPermission, datecreate, dateupdate)
SELECT 5, p.id, NOW(), NOW()
FROM permissions p
WHERE NOT EXISTS (
  SELECT 1 FROM roles_permissions rp
  WHERE rp.fkRole = 5 AND rp.fkPermission = p.id
);

-- Preuves
SELECT COUNT(*) AS total_permissions FROM permissions;
SELECT COUNT(*) AS admin_permissions FROM roles_permissions WHERE fkRole = 5;
SELECT p.id, p.nom, p.parent
FROM permissions p
WHERE NOT EXISTS (
  SELECT 1 FROM roles_permissions rp WHERE rp.fkRole = 5 AND rp.fkPermission = p.id
);
