package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.service;

import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.request.ClotureInterventionRequest;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.request.InterventionRequest;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response.InterventionResponse;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Equipement;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Equipement.Statut;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Intervention;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Intervention.Priorite;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Intervention.Type;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.infrastructure.persistence.InterventionJdbcRepository;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class InterventionService {

  private static final Set<Intervention.Statut> OPEN =
      EnumSet.of(Intervention.Statut.BROUILLON, Intervention.Statut.PLANIFIEE,
          Intervention.Statut.EN_COURS);

  private final InterventionJdbcRepository repository;
  private final EquipementService equipementService;

  @Transactional(readOnly = true)
  public PageResponse<InterventionResponse> findAll(Long fkPharmacie, Long fkEquipement,
      String statut, String type, String search, int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 200);
    int offset = safePage * safeSize;
    List<Intervention> rows =
        repository.findAll(fkPharmacie, fkEquipement, statut, type, search, safeSize, offset);
    long total = repository.count(fkPharmacie, fkEquipement, statut, type, search);
    return PageResponse.of(rows.stream().map(this::toResponse).toList(), safePage, safeSize, total);
  }

  @Transactional(readOnly = true)
  public InterventionResponse findById(Long id) {
    return toResponse(require(id));
  }

  @Transactional
  public InterventionResponse create(InterventionRequest request, Long userId) {
    Equipement equipement = equipementService.require(request.getFkEquipement());
    LocalDateTime now = LocalDateTime.now();
    Intervention.Statut statut = StringUtils.hasText(request.getStatut())
        ? parseEnum(Intervention.Statut.class, request.getStatut(), "statut")
        : Intervention.Statut.BROUILLON;
    if (statut == Intervention.Statut.CLOTUREE || statut == Intervention.Statut.ANNULEE) {
      throw new BusinessException("Impossible de créer une intervention déjà clôturée ou annulée");
    }

    Intervention entity = Intervention.builder()
        .numero(repository.nextNumero(now))
        .fkEquipement(equipement.getId())
        .typeIntervention(parseEnum(Type.class, request.getTypeIntervention(), "type"))
        .priorite(parseEnum(Priorite.class, request.getPriorite(), "priorité"))
        .statut(statut)
        .titre(request.getTitre().trim())
        .description(trimToNull(request.getDescription()))
        .diagnostic(trimToNull(request.getDiagnostic()))
        .travauxRealises(trimToNull(request.getTravauxRealises()))
        .technicienNom(trimToNull(request.getTechnicienNom()))
        .technicienUserId(request.getTechnicienUserId())
        .fkPharmacie(request.getFkPharmacie() != null ? request.getFkPharmacie()
            : equipement.getFkPharmacie())
        .dateDemande(now)
        .datePlanifiee(request.getDatePlanifiee())
        .coutEstime(request.getCoutEstime())
        .coutReel(request.getCoutReel())
        .userCreateId(userId)
        .build();

    if (statut == Intervention.Statut.EN_COURS) {
      entity.setDateDebut(now);
      equipementService.updateStatut(equipement.getId(), Statut.EN_MAINTENANCE, userId);
    } else if (statut == Intervention.Statut.PLANIFIEE && entity.getDatePlanifiee() == null) {
      entity.setDatePlanifiee(now);
    }

    Long id = repository.insert(entity);
    return findById(id);
  }

  @Transactional
  public InterventionResponse update(Long id, InterventionRequest request, Long userId) {
    Intervention existing = require(id);
    assertOpen(existing);

    Equipement equipement = equipementService.require(request.getFkEquipement());
    existing.setFkEquipement(equipement.getId());
    existing.setTypeIntervention(parseEnum(Type.class, request.getTypeIntervention(), "type"));
    existing.setPriorite(parseEnum(Priorite.class, request.getPriorite(), "priorité"));
    existing.setTitre(request.getTitre().trim());
    existing.setDescription(trimToNull(request.getDescription()));
    existing.setDiagnostic(trimToNull(request.getDiagnostic()));
    existing.setTravauxRealises(trimToNull(request.getTravauxRealises()));
    existing.setTechnicienNom(trimToNull(request.getTechnicienNom()));
    existing.setTechnicienUserId(request.getTechnicienUserId());
    existing.setFkPharmacie(request.getFkPharmacie() != null ? request.getFkPharmacie()
        : equipement.getFkPharmacie());
    existing.setDatePlanifiee(request.getDatePlanifiee());
    existing.setCoutEstime(request.getCoutEstime());
    existing.setCoutReel(request.getCoutReel());
    existing.setUserUpdateId(userId);

    if (StringUtils.hasText(request.getStatut())) {
      Intervention.Statut next = parseEnum(Intervention.Statut.class, request.getStatut(), "statut");
      if (next != existing.getStatut()) {
        applyTransition(existing, next, userId);
      }
    }

    repository.update(existing);
    return findById(id);
  }

  @Transactional
  public InterventionResponse planifier(Long id, Long userId) {
    Intervention existing = require(id);
    applyTransition(existing, Intervention.Statut.PLANIFIEE, userId);
    if (existing.getDatePlanifiee() == null) {
      existing.setDatePlanifiee(LocalDateTime.now());
    }
    repository.update(existing);
    return findById(id);
  }

  @Transactional
  public InterventionResponse demarrer(Long id, Long userId) {
    Intervention existing = require(id);
    applyTransition(existing, Intervention.Statut.EN_COURS, userId);
    existing.setDateDebut(LocalDateTime.now());
    repository.update(existing);
    equipementService.updateStatut(existing.getFkEquipement(), Statut.EN_MAINTENANCE, userId);
    return findById(id);
  }

  @Transactional
  public InterventionResponse cloturer(Long id, ClotureInterventionRequest request, Long userId) {
    Intervention existing = require(id);
    applyTransition(existing, Intervention.Statut.CLOTUREE, userId);
    if (request != null) {
      if (StringUtils.hasText(request.getTravauxRealises())) {
        existing.setTravauxRealises(request.getTravauxRealises().trim());
      }
      if (StringUtils.hasText(request.getDiagnostic())) {
        existing.setDiagnostic(request.getDiagnostic().trim());
      }
      if (request.getCoutReel() != null) {
        existing.setCoutReel(request.getCoutReel());
      }
    }
    existing.setDateCloture(LocalDateTime.now());
    if (existing.getDateDebut() == null) {
      existing.setDateDebut(existing.getDateCloture());
    }
    existing.setUserUpdateId(userId);
    repository.update(existing);

    boolean remettre = request == null || request.getRemettreEnService() == null
        || Boolean.TRUE.equals(request.getRemettreEnService());
    equipementService.updateStatut(existing.getFkEquipement(),
        remettre ? Statut.EN_SERVICE : Statut.EN_PANNE, userId);
    return findById(id);
  }

  @Transactional
  public InterventionResponse annuler(Long id, Long userId) {
    Intervention existing = require(id);
    applyTransition(existing, Intervention.Statut.ANNULEE, userId);
    existing.setUserUpdateId(userId);
    repository.update(existing);
    return findById(id);
  }

  private void applyTransition(Intervention intervention, Intervention.Statut next, Long userId) {
    Intervention.Statut current = intervention.getStatut();
    boolean allowed = switch (next) {
      case PLANIFIEE -> current == Intervention.Statut.BROUILLON
          || current == Intervention.Statut.PLANIFIEE;
      case EN_COURS -> current == Intervention.Statut.BROUILLON
          || current == Intervention.Statut.PLANIFIEE
          || current == Intervention.Statut.EN_COURS;
      case CLOTUREE -> current == Intervention.Statut.EN_COURS
          || current == Intervention.Statut.PLANIFIEE
          || current == Intervention.Statut.BROUILLON;
      case ANNULEE -> OPEN.contains(current);
      case BROUILLON -> current == Intervention.Statut.BROUILLON;
    };
    if (!allowed) {
      throw new BusinessException(
          "Transition interdite : " + current + " → " + next);
    }
    intervention.setStatut(next);
    intervention.setUserUpdateId(userId);
  }

  private void assertOpen(Intervention intervention) {
    if (!OPEN.contains(intervention.getStatut())) {
      throw new BusinessException(
          "Intervention " + intervention.getNumero() + " n'est plus modifiable ("
              + intervention.getStatut() + ")");
    }
  }

  private Intervention require(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> NotFoundException.entity("Intervention", id));
  }

  private InterventionResponse toResponse(Intervention i) {
    return InterventionResponse.builder()
        .id(i.getId())
        .numero(i.getNumero())
        .fkEquipement(i.getFkEquipement())
        .equipementCode(i.getEquipementCode())
        .equipementDesignation(i.getEquipementDesignation())
        .typeIntervention(i.getTypeIntervention().name())
        .priorite(i.getPriorite().name())
        .statut(i.getStatut().name())
        .titre(i.getTitre())
        .description(i.getDescription())
        .diagnostic(i.getDiagnostic())
        .travauxRealises(i.getTravauxRealises())
        .technicienNom(i.getTechnicienNom())
        .technicienUserId(i.getTechnicienUserId())
        .fkPharmacie(i.getFkPharmacie())
        .dateDemande(i.getDateDemande())
        .datePlanifiee(i.getDatePlanifiee())
        .dateDebut(i.getDateDebut())
        .dateCloture(i.getDateCloture())
        .coutEstime(i.getCoutEstime())
        .coutReel(i.getCoutReel())
        .dateCreate(i.getDateCreate())
        .dateUpdate(i.getDateUpdate())
        .build();
  }

  private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String label) {
    if (!StringUtils.hasText(value)) {
      throw new BusinessException("Le champ " + label + " est obligatoire");
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
