package cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Equipement {

  public enum Categorie {
    BIOMEDICAL, INFRASTRUCTURE, INFORMATIQUE, VEHICULE, AUTRE
  }

  public enum Statut {
    EN_SERVICE, EN_PANNE, EN_MAINTENANCE, HORS_SERVICE, REFORME
  }

  public enum Criticite {
    BASSE, MOYENNE, HAUTE, CRITIQUE
  }

  public enum EtatGeneral {
    EXCELLENT, BON, MOYEN, MAUVAIS, HORS_SERVICE
  }

  public enum Fonctionnement {
    FONCTIONNEL, FONCTIONNEL_RESERVE, EN_PANNE, REFORME
  }

  private Long id;
  private String codeInterne;
  private String designation;
  private Categorie categorie;
  private String marque;
  private String modele;
  private String numeroSerie;
  private Long fkSite;
  private Long fkPharmacie;
  private String localisation;
  private Statut statut;
  private Criticite criticite;
  private LocalDate dateMiseEnService;
  private LocalDate dateGarantieFin;
  private LocalDate dateInventaire;
  private String nomInventoriste;
  private String etablissement;
  private String service;
  private String fabricant;
  private String paysAcquisition;
  private Integer anneeFabrication;
  private LocalDate dateInstallation;
  private String fournisseur;
  private String fournisseurCorrespondant;
  private String fournisseurTelephone;
  private String fournisseurEmail;
  private String fournisseurAdresse;
  private EtatGeneral etatGeneral;
  private Fonctionnement fonctionnement;
  private boolean contratMaintenance;
  private String contratNumero;
  private LocalDate contratEcheance;
  private boolean maintenanceInterne;
  private boolean maintenanceExterne;
  private Integer frequenceMaintenanceJours;
  private LocalDate derniereMaintenance;
  private LocalDate prochaineMaintenance;
  private String technicienResponsable;
  private String technicienContact;
  private boolean consommablesDisponibles;
  private boolean piecesRechangeDisponibles;
  private boolean manuelUtilisateur;
  private boolean manuelTechnique;
  private boolean accessoiresComplets;
  private String responsableService;
  private String ingenieurBiomedical;
  private String notes;
  private boolean actif;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
  private Long userCreateId;
  private Long userUpdateId;
}
