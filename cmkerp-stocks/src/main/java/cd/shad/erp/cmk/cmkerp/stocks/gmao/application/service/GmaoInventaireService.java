package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.service;

import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.request.InventaireCampagneRequest;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.request.InventaireLigneRequest;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response.InventaireCampagneResponse;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response.InventaireLigneResponse;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Equipement;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.InventaireCampagne;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.InventaireCampagne.Statut;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.InventaireLigne;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.InventaireLigne.Resultat;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.infrastructure.persistence.EquipementJdbcRepository;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.infrastructure.persistence.InventaireCampagneJdbcRepository;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.infrastructure.persistence.InventaireLigneJdbcRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class GmaoInventaireService {

  private final InventaireCampagneJdbcRepository campagneRepository;
  private final InventaireLigneJdbcRepository ligneRepository;
  private final EquipementJdbcRepository equipementRepository;

  @Transactional(readOnly = true)
  public PageResponse<InventaireCampagneResponse> findCampagnes(String statut, String search,
      int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 100);
    List<InventaireCampagne> rows =
        campagneRepository.findAll(statut, search, safeSize, safePage * safeSize);
    long total = campagneRepository.count(statut, search);
    return PageResponse.of(rows.stream().map(this::toCampagneResponse).toList(), safePage, safeSize,
        total);
  }

  @Transactional(readOnly = true)
  public InventaireCampagneResponse findCampagne(Long id) {
    return toCampagneResponse(requireCampagne(id));
  }

  @Transactional
  public InventaireCampagneResponse createCampagne(InventaireCampagneRequest request, Long userId) {
    InventaireCampagne entity = InventaireCampagne.builder()
        .numero(nextNumero())
        .libelle(request.getLibelle().trim())
        .dateDebut(request.getDateDebut())
        .dateFinPrevue(request.getDateFinPrevue())
        .statut(Statut.BROUILLON)
        .perimetreService(trim(request.getPerimetreService()))
        .perimetreCategorie(trimUpper(request.getPerimetreCategorie()))
        .responsable(trim(request.getResponsable()))
        .notes(trim(request.getNotes()))
        .userCreateId(userId)
        .build();
    Long id = campagneRepository.insert(entity);
    if (Boolean.TRUE.equals(request.getGenererLignes())) {
      genererLignes(id, userId);
    }
    return findCampagne(id);
  }

  @Transactional
  public InventaireCampagneResponse demarrer(Long id, Long userId) {
    InventaireCampagne c = requireCampagne(id);
    if (c.getStatut() == Statut.CLOTURE || c.getStatut() == Statut.ANNULE) {
      throw new BusinessException("Impossible de démarrer une campagne " + c.getStatut().name());
    }
    if (ligneRepository.countByCampagne(id, null, null) == 0) {
      genererLignes(id, userId);
    }
    c.setStatut(Statut.EN_COURS);
    c.setUserUpdateId(userId);
    campagneRepository.update(c);
    return findCampagne(id);
  }

  @Transactional
  public InventaireCampagneResponse cloturer(Long id, Long userId) {
    InventaireCampagne c = requireCampagne(id);
    if (c.getStatut() != Statut.EN_COURS && c.getStatut() != Statut.BROUILLON) {
      throw new BusinessException("Seule une campagne en cours ou brouillon peut être clôturée");
    }
    long restants = ligneRepository.countByResultat(id, Resultat.A_VERIFIER.name());
    if (restants > 0) {
      throw new BusinessException(
          restants + " ligne(s) encore à vérifier — terminez le contrôle avant clôture");
    }
    c.setStatut(Statut.CLOTURE);
    c.setDateCloture(LocalDateTime.now());
    c.setUserUpdateId(userId);
    campagneRepository.update(c);

    // Propager les infos inventaire sur les équipements contrôlés
    List<InventaireLigne> lignes = ligneRepository.findByCampagne(id, null, null, 10_000, 0);
    for (InventaireLigne ligne : lignes) {
      if (ligne.getResultat() == Resultat.A_VERIFIER) {
        continue;
      }
      try {
        Equipement eq = equipementRepository.findById(ligne.getFkEquipement()).orElse(null);
        if (eq == null) {
          continue;
        }
        eq.setDateInventaire(ligne.getDateControle() != null
            ? ligne.getDateControle().toLocalDate() : LocalDate.now());
        eq.setNomInventoriste(ligne.getInventoriste());
        if (StringUtils.hasText(ligne.getLocalisationConstatee())
            && ligne.getResultat() == Resultat.DEPLACE) {
          eq.setLocalisation(ligne.getLocalisationConstatee());
        }
        if (ligne.getResultat() == Resultat.ABSENT) {
          // ne change pas le statut automatiquement — écart à traiter
        } else if (ligne.getResultat() == Resultat.HORS_SERVICE) {
          eq.setStatut(Equipement.Statut.HORS_SERVICE);
        }
        if (Boolean.TRUE.equals(ligne.getConsommablesOk())) {
          eq.setConsommablesDisponibles(true);
        } else if (Boolean.FALSE.equals(ligne.getConsommablesOk())) {
          eq.setConsommablesDisponibles(false);
        }
        if (Boolean.TRUE.equals(ligne.getPiecesOk())) {
          eq.setPiecesRechangeDisponibles(true);
        } else if (Boolean.FALSE.equals(ligne.getPiecesOk())) {
          eq.setPiecesRechangeDisponibles(false);
        }
        if (Boolean.TRUE.equals(ligne.getManuelUtilisateurOk())) {
          eq.setManuelUtilisateur(true);
        } else if (Boolean.FALSE.equals(ligne.getManuelUtilisateurOk())) {
          eq.setManuelUtilisateur(false);
        }
        if (Boolean.TRUE.equals(ligne.getManuelTechniqueOk())) {
          eq.setManuelTechnique(true);
        } else if (Boolean.FALSE.equals(ligne.getManuelTechniqueOk())) {
          eq.setManuelTechnique(false);
        }
        if (Boolean.TRUE.equals(ligne.getAccessoiresOk())) {
          eq.setAccessoiresComplets(true);
        } else if (Boolean.FALSE.equals(ligne.getAccessoiresOk())) {
          eq.setAccessoiresComplets(false);
        }
        eq.setUserUpdateId(userId);
        equipementRepository.update(eq);
      } catch (Exception ignored) {
        // non bloquant
      }
    }
    return findCampagne(id);
  }

  @Transactional
  public InventaireCampagneResponse genererLignes(Long campagneId, Long userId) {
    InventaireCampagne c = requireCampagne(campagneId);
    if (c.getStatut() == Statut.CLOTURE || c.getStatut() == Statut.ANNULE) {
      throw new BusinessException("Campagne clôturée ou annulée");
    }
    String categorie = c.getPerimetreCategorie();
    String serviceFilter = c.getPerimetreService();
    List<Equipement> parc =
        equipementRepository.findAll(null, null, categorie, null, true, 5000, 0);
    int added = 0;
    for (Equipement eq : parc) {
      if (StringUtils.hasText(serviceFilter)
          && (eq.getService() == null
              || !eq.getService().equalsIgnoreCase(serviceFilter.trim()))) {
        continue;
      }
      if (ligneRepository.exists(campagneId, eq.getId())) {
        continue;
      }
      InventaireLigne ligne = InventaireLigne.builder()
          .fkCampagne(campagneId)
          .fkEquipement(eq.getId())
          .resultat(Resultat.A_VERIFIER)
          .localisationSysteme(eq.getLocalisation())
          .userCreateId(userId)
          .build();
      ligneRepository.insert(ligne);
      added++;
    }
    if (added == 0 && ligneRepository.countByCampagne(campagneId, null, null) == 0) {
      throw new BusinessException(
          "Aucun équipement actif trouvé pour ce périmètre — créez d'abord les fiches d'identification");
    }
    return findCampagne(campagneId);
  }

  @Transactional(readOnly = true)
  public PageResponse<InventaireLigneResponse> findLignes(Long campagneId, String resultat,
      String search, int page, int size) {
    requireCampagne(campagneId);
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 200);
    List<InventaireLigne> rows =
        ligneRepository.findByCampagne(campagneId, resultat, search, safeSize, safePage * safeSize);
    long total = ligneRepository.countByCampagne(campagneId, resultat, search);
    return PageResponse.of(rows.stream().map(this::toLigneResponse).toList(), safePage, safeSize,
        total);
  }

  @Transactional
  public InventaireLigneResponse controlerLigne(Long ligneId, InventaireLigneRequest request,
      Long userId) {
    InventaireLigne ligne = ligneRepository.findById(ligneId)
        .orElseThrow(() -> NotFoundException.entity("InventaireLigne", ligneId));
    InventaireCampagne c = requireCampagne(ligne.getFkCampagne());
    if (c.getStatut() == Statut.CLOTURE || c.getStatut() == Statut.ANNULE) {
      throw new BusinessException("Campagne non modifiable");
    }
    if (c.getStatut() == Statut.BROUILLON) {
      c.setStatut(Statut.EN_COURS);
      c.setUserUpdateId(userId);
      campagneRepository.update(c);
    }

    Resultat resultat;
    try {
      resultat = Resultat.valueOf(request.getResultat().trim().toUpperCase());
    } catch (Exception ex) {
      throw new BusinessException("Résultat d'inventaire invalide : " + request.getResultat());
    }
    if (resultat == Resultat.A_VERIFIER) {
      throw new BusinessException("Indiquez un résultat de contrôle (PRESENT, ABSENT, …)");
    }

    ligne.setResultat(resultat);
    ligne.setLocalisationConstatee(trim(request.getLocalisationConstatee()));
    ligne.setEtatConstate(trimUpper(request.getEtatConstate()));
    ligne.setFonctionnementConstate(trimUpper(request.getFonctionnementConstate()));
    ligne.setConsommablesOk(request.getConsommablesOk());
    ligne.setPiecesOk(request.getPiecesOk());
    ligne.setManuelUtilisateurOk(request.getManuelUtilisateurOk());
    ligne.setManuelTechniqueOk(request.getManuelTechniqueOk());
    ligne.setAccessoiresOk(request.getAccessoiresOk());
    ligne.setRemarque(trim(request.getRemarque()));
    ligne.setInventoriste(trim(request.getInventoriste()));
    ligne.setDateControle(LocalDateTime.now());
    ligne.setEcart(computeEcart(ligne));
    ligne.setUserUpdateId(userId);
    ligneRepository.update(ligne);
    return toLigneResponse(ligneRepository.findById(ligneId).orElse(ligne));
  }

  private boolean computeEcart(InventaireLigne ligne) {
    if (ligne.getResultat() == Resultat.ABSENT
        || ligne.getResultat() == Resultat.NON_IDENTIFIE
        || ligne.getResultat() == Resultat.HORS_SERVICE) {
      return true;
    }
    if (ligne.getResultat() == Resultat.DEPLACE) {
      return true;
    }
    if (StringUtils.hasText(ligne.getLocalisationConstatee())
        && StringUtils.hasText(ligne.getLocalisationSysteme())
        && !Objects.equals(
            ligne.getLocalisationConstatee().trim().toLowerCase(),
            ligne.getLocalisationSysteme().trim().toLowerCase())) {
      return true;
    }
    return false;
  }

  private InventaireCampagne requireCampagne(Long id) {
    return campagneRepository.findById(id)
        .orElseThrow(() -> NotFoundException.entity("InventaireCampagne", id));
  }

  private String nextNumero() {
    String prefix = "INV-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
    long seq = campagneRepository.countByPrefix(prefix) + 1;
    return prefix + String.format("%03d", seq);
  }

  private InventaireCampagneResponse toCampagneResponse(InventaireCampagne c) {
    long total = ligneRepository.countByCampagne(c.getId(), null, null);
    return InventaireCampagneResponse.builder()
        .id(c.getId())
        .numero(c.getNumero())
        .libelle(c.getLibelle())
        .dateDebut(c.getDateDebut())
        .dateFinPrevue(c.getDateFinPrevue())
        .dateCloture(c.getDateCloture())
        .statut(c.getStatut().name())
        .perimetreService(c.getPerimetreService())
        .perimetreCategorie(c.getPerimetreCategorie())
        .responsable(c.getResponsable())
        .notes(c.getNotes())
        .totalLignes(total)
        .aVerifier(ligneRepository.countByResultat(c.getId(), Resultat.A_VERIFIER.name()))
        .presentes(ligneRepository.countByResultat(c.getId(), Resultat.PRESENT.name()))
        .absentes(ligneRepository.countByResultat(c.getId(), Resultat.ABSENT.name()))
        .deplacees(ligneRepository.countByResultat(c.getId(), Resultat.DEPLACE.name()))
        .ecarts(ligneRepository.countEcarts(c.getId()))
        .dateCreate(c.getDateCreate())
        .dateUpdate(c.getDateUpdate())
        .build();
  }

  private InventaireLigneResponse toLigneResponse(InventaireLigne l) {
    return InventaireLigneResponse.builder()
        .id(l.getId())
        .fkCampagne(l.getFkCampagne())
        .fkEquipement(l.getFkEquipement())
        .equipementCode(l.getEquipementCode())
        .equipementDesignation(l.getEquipementDesignation())
        .equipementService(l.getEquipementService())
        .equipementStatut(l.getEquipementStatut())
        .resultat(l.getResultat().name())
        .localisationSysteme(l.getLocalisationSysteme())
        .localisationConstatee(l.getLocalisationConstatee())
        .etatConstate(l.getEtatConstate())
        .fonctionnementConstate(l.getFonctionnementConstate())
        .consommablesOk(l.getConsommablesOk())
        .piecesOk(l.getPiecesOk())
        .manuelUtilisateurOk(l.getManuelUtilisateurOk())
        .manuelTechniqueOk(l.getManuelTechniqueOk())
        .accessoiresOk(l.getAccessoiresOk())
        .remarque(l.getRemarque())
        .inventoriste(l.getInventoriste())
        .dateControle(l.getDateControle())
        .ecart(l.isEcart())
        .dateCreate(l.getDateCreate())
        .dateUpdate(l.getDateUpdate())
        .build();
  }

  private static String trim(String v) {
    return StringUtils.hasText(v) ? v.trim() : null;
  }

  private static String trimUpper(String v) {
    return StringUtils.hasText(v) ? v.trim().toUpperCase() : null;
  }
}
