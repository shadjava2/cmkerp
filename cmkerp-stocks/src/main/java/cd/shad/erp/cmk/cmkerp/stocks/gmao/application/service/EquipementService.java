package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.service;

import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.request.EquipementRequest;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response.EquipementResponse;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Equipement;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Equipement.Categorie;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Equipement.Criticite;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Equipement.EtatGeneral;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Equipement.Fonctionnement;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Equipement.Statut;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.infrastructure.persistence.EquipementJdbcRepository;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.infrastructure.persistence.EquipementMediaJdbcRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EquipementService {

  private final EquipementJdbcRepository repository;
  private final EquipementMediaJdbcRepository mediaRepository;

  @Transactional(readOnly = true)
  public PageResponse<EquipementResponse> findAll(Long fkPharmacie, String statut, String categorie,
      String search, Boolean actif, int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 200);
    int offset = safePage * safeSize;
    List<Equipement> rows =
        repository.findAll(fkPharmacie, statut, categorie, search, actif, safeSize, offset);
    long total = repository.count(fkPharmacie, statut, categorie, search, actif);
    return PageResponse.of(rows.stream().map(this::toResponse).toList(), safePage, safeSize, total);
  }

  @Transactional(readOnly = true)
  public EquipementResponse findById(Long id) {
    return toResponse(require(id));
  }

  @Transactional
  public EquipementResponse create(EquipementRequest request, Long userId) {
    validateEnums(request);
    String code = request.getCodeInterne().trim().toUpperCase();
    if (repository.findByCode(code).isPresent()) {
      throw new BusinessException("Un équipement avec le code " + code + " existe déjà");
    }
    Equipement entity = fromRequest(request, code);
    entity.setUserCreateId(userId);
    entity.setActif(request.getActif() == null || request.getActif());
    Long id = repository.insert(entity);
    return findById(id);
  }

  @Transactional
  public EquipementResponse update(Long id, EquipementRequest request, Long userId) {
    Equipement existing = require(id);
    validateEnums(request);
    String code = request.getCodeInterne().trim().toUpperCase();
    repository.findByCode(code).ifPresent(other -> {
      if (!other.getId().equals(id)) {
        throw new BusinessException("Un équipement avec le code " + code + " existe déjà");
      }
    });
    Equipement entity = fromRequest(request, code);
    entity.setId(id);
    entity.setUserUpdateId(userId);
    entity.setActif(request.getActif() != null ? request.getActif() : existing.isActif());
    repository.update(entity);
    return findById(id);
  }

  @Transactional
  public void softDelete(Long id, Long userId) {
    Equipement existing = require(id);
    existing.setActif(false);
    existing.setUserUpdateId(userId);
    repository.update(existing);
  }

  @Transactional
  public void updateStatut(Long id, Statut statut, Long userId) {
    Equipement existing = require(id);
    existing.setStatut(statut);
    existing.setUserUpdateId(userId);
    repository.update(existing);
  }

  public Equipement require(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> NotFoundException.entity("Equipement", id));
  }

  private void validateEnums(EquipementRequest request) {
    parseEnum(Categorie.class, request.getCategorie(), "catégorie");
    parseEnum(Statut.class, request.getStatut(), "statut");
    parseEnum(Criticite.class, request.getCriticite(), "criticité");
    parseOptionalEnum(EtatGeneral.class, request.getEtatGeneral(), "état général");
    parseOptionalEnum(Fonctionnement.class, request.getFonctionnement(), "fonctionnement");
  }

  private Equipement fromRequest(EquipementRequest request, String code) {
    return Equipement.builder()
        .codeInterne(code)
        .designation(request.getDesignation().trim())
        .categorie(parseEnum(Categorie.class, request.getCategorie(), "catégorie"))
        .marque(trimToNull(request.getMarque()))
        .modele(trimToNull(request.getModele()))
        .numeroSerie(trimToNull(request.getNumeroSerie()))
        .fkSite(request.getFkSite())
        .fkPharmacie(request.getFkPharmacie())
        .localisation(trimToNull(request.getLocalisation()))
        .statut(parseEnum(Statut.class, request.getStatut(), "statut"))
        .criticite(parseEnum(Criticite.class, request.getCriticite(), "criticité"))
        .dateMiseEnService(request.getDateMiseEnService())
        .dateGarantieFin(request.getDateGarantieFin())
        .dateInventaire(request.getDateInventaire())
        .nomInventoriste(trimToNull(request.getNomInventoriste()))
        .etablissement(trimToNull(request.getEtablissement()))
        .service(trimToNull(request.getService()))
        .fabricant(trimToNull(request.getFabricant()))
        .paysAcquisition(trimToNull(request.getPaysAcquisition()))
        .anneeFabrication(request.getAnneeFabrication())
        .dateInstallation(request.getDateInstallation())
        .fournisseur(trimToNull(request.getFournisseur()))
        .fournisseurCorrespondant(trimToNull(request.getFournisseurCorrespondant()))
        .fournisseurTelephone(trimToNull(request.getFournisseurTelephone()))
        .fournisseurEmail(trimToNull(request.getFournisseurEmail()))
        .fournisseurAdresse(trimToNull(request.getFournisseurAdresse()))
        .etatGeneral(parseOptionalEnum(EtatGeneral.class, request.getEtatGeneral(), "état général"))
        .fonctionnement(parseOptionalEnum(Fonctionnement.class, request.getFonctionnement(), "fonctionnement"))
        .contratMaintenance(Boolean.TRUE.equals(request.getContratMaintenance()))
        .contratNumero(trimToNull(request.getContratNumero()))
        .contratEcheance(request.getContratEcheance())
        .maintenanceInterne(Boolean.TRUE.equals(request.getMaintenanceInterne()))
        .maintenanceExterne(Boolean.TRUE.equals(request.getMaintenanceExterne()))
        .frequenceMaintenanceJours(request.getFrequenceMaintenanceJours())
        .derniereMaintenance(request.getDerniereMaintenance())
        .prochaineMaintenance(request.getProchaineMaintenance())
        .technicienResponsable(trimToNull(request.getTechnicienResponsable()))
        .technicienContact(trimToNull(request.getTechnicienContact()))
        .consommablesDisponibles(Boolean.TRUE.equals(request.getConsommablesDisponibles()))
        .piecesRechangeDisponibles(Boolean.TRUE.equals(request.getPiecesRechangeDisponibles()))
        .manuelUtilisateur(Boolean.TRUE.equals(request.getManuelUtilisateur()))
        .manuelTechnique(Boolean.TRUE.equals(request.getManuelTechnique()))
        .accessoiresComplets(Boolean.TRUE.equals(request.getAccessoiresComplets()))
        .responsableService(trimToNull(request.getResponsableService()))
        .ingenieurBiomedical(trimToNull(request.getIngenieurBiomedical()))
        .notes(trimToNull(request.getNotes()))
        .build();
  }

  private EquipementResponse toResponse(Equipement e) {
    return EquipementResponse.builder()
        .id(e.getId())
        .codeInterne(e.getCodeInterne())
        .designation(e.getDesignation())
        .categorie(e.getCategorie().name())
        .marque(e.getMarque())
        .modele(e.getModele())
        .numeroSerie(e.getNumeroSerie())
        .fkSite(e.getFkSite())
        .fkPharmacie(e.getFkPharmacie())
        .localisation(e.getLocalisation())
        .statut(e.getStatut().name())
        .criticite(e.getCriticite().name())
        .dateMiseEnService(e.getDateMiseEnService())
        .dateGarantieFin(e.getDateGarantieFin())
        .dateInventaire(e.getDateInventaire())
        .nomInventoriste(e.getNomInventoriste())
        .etablissement(e.getEtablissement())
        .service(e.getService())
        .fabricant(e.getFabricant())
        .paysAcquisition(e.getPaysAcquisition())
        .anneeFabrication(e.getAnneeFabrication())
        .dateInstallation(e.getDateInstallation())
        .fournisseur(e.getFournisseur())
        .fournisseurCorrespondant(e.getFournisseurCorrespondant())
        .fournisseurTelephone(e.getFournisseurTelephone())
        .fournisseurEmail(e.getFournisseurEmail())
        .fournisseurAdresse(e.getFournisseurAdresse())
        .etatGeneral(e.getEtatGeneral() != null ? e.getEtatGeneral().name() : null)
        .fonctionnement(e.getFonctionnement() != null ? e.getFonctionnement().name() : null)
        .contratMaintenance(e.isContratMaintenance())
        .contratNumero(e.getContratNumero())
        .contratEcheance(e.getContratEcheance())
        .maintenanceInterne(e.isMaintenanceInterne())
        .maintenanceExterne(e.isMaintenanceExterne())
        .frequenceMaintenanceJours(e.getFrequenceMaintenanceJours())
        .derniereMaintenance(e.getDerniereMaintenance())
        .prochaineMaintenance(e.getProchaineMaintenance())
        .technicienResponsable(e.getTechnicienResponsable())
        .technicienContact(e.getTechnicienContact())
        .consommablesDisponibles(e.isConsommablesDisponibles())
        .piecesRechangeDisponibles(e.isPiecesRechangeDisponibles())
        .manuelUtilisateur(e.isManuelUtilisateur())
        .manuelTechnique(e.isManuelTechnique())
        .accessoiresComplets(e.isAccessoiresComplets())
        .responsableService(e.getResponsableService())
        .ingenieurBiomedical(e.getIngenieurBiomedical())
        .notes(e.getNotes())
        .actif(e.isActif())
        .photoPrincipaleId(mediaRepository.findPrincipalId(e.getId()))
        .mediasCount(mediaRepository.countByEquipement(e.getId()))
        .dateCreate(e.getDateCreate())
        .dateUpdate(e.getDateUpdate())
        .build();
  }

  private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String label) {
    if (!StringUtils.hasText(value)) {
      throw new BusinessException("La " + label + " est obligatoire");
    }
    try {
      return Enum.valueOf(type, value.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new BusinessException("Valeur invalide pour " + label + " : " + value);
    }
  }

  private static <E extends Enum<E>> E parseOptionalEnum(Class<E> type, String value, String label) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return Enum.valueOf(type, value.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new BusinessException("Valeur invalide pour " + label + " : " + value);
    }
  }

  private static String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
