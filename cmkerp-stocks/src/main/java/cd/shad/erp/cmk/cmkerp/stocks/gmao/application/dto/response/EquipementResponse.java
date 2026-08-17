package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EquipementResponse {
  private Long id;
  private String codeInterne;
  private String designation;
  private String categorie;
  private String marque;
  private String modele;
  private String numeroSerie;
  private Long fkSite;
  private Long fkPharmacie;
  private String localisation;
  private String statut;
  private String criticite;
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
  private String etatGeneral;
  private String fonctionnement;
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
  private Long photoPrincipaleId;
  private Integer mediasCount;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
}
