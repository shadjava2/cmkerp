package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

@Data
public class EquipementRequest {

  @NotBlank
  @Size(max = 64)
  private String codeInterne;

  @NotBlank
  @Size(max = 255)
  private String designation;

  @NotBlank
  private String categorie;

  @Size(max = 120)
  private String marque;

  @Size(max = 120)
  private String modele;

  @Size(max = 120)
  private String numeroSerie;

  private Long fkSite;
  private Long fkPharmacie;

  @Size(max = 255)
  private String localisation;

  @NotBlank
  private String statut;

  @NotBlank
  private String criticite;

  private LocalDate dateMiseEnService;
  private LocalDate dateGarantieFin;
  private LocalDate dateInventaire;

  @Size(max = 160)
  private String nomInventoriste;

  @Size(max = 160)
  private String etablissement;

  @Size(max = 160)
  private String service;

  @Size(max = 120)
  private String fabricant;

  @Size(max = 80)
  private String paysAcquisition;

  private Integer anneeFabrication;
  private LocalDate dateInstallation;

  @Size(max = 160)
  private String fournisseur;

  @Size(max = 160)
  private String fournisseurCorrespondant;

  @Size(max = 60)
  private String fournisseurTelephone;

  @Size(max = 120)
  private String fournisseurEmail;

  private String fournisseurAdresse;
  private String etatGeneral;
  private String fonctionnement;
  private Boolean contratMaintenance;

  @Size(max = 80)
  private String contratNumero;

  private LocalDate contratEcheance;
  private Boolean maintenanceInterne;
  private Boolean maintenanceExterne;
  private Integer frequenceMaintenanceJours;
  private LocalDate derniereMaintenance;
  private LocalDate prochaineMaintenance;

  @Size(max = 160)
  private String technicienResponsable;

  @Size(max = 160)
  private String technicienContact;

  private Boolean consommablesDisponibles;
  private Boolean piecesRechangeDisponibles;
  private Boolean manuelUtilisateur;
  private Boolean manuelTechnique;
  private Boolean accessoiresComplets;

  @Size(max = 160)
  private String responsableService;

  @Size(max = 160)
  private String ingenieurBiomedical;

  private String notes;
  private Boolean actif;
}
