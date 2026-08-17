package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.service;

import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.request.InterventionRequest;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.request.PlanPreventifRequest;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response.InterventionResponse;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response.PlanPreventifResponse;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.PlanPreventif;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.infrastructure.persistence.PlanPreventifJdbcRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PlanPreventifService {

  private final PlanPreventifJdbcRepository repository;
  private final EquipementService equipementService;
  private final InterventionService interventionService;

  @Transactional(readOnly = true)
  public PageResponse<PlanPreventifResponse> findAll(Long fkEquipement, Boolean actif,
      Boolean enRetardOnly, int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 200);
    int offset = safePage * safeSize;
    List<PlanPreventif> rows =
        repository.findAll(fkEquipement, actif, enRetardOnly, safeSize, offset);
    long total = repository.count(fkEquipement, actif, enRetardOnly);
    return PageResponse.of(rows.stream().map(this::toResponse).toList(), safePage, safeSize, total);
  }

  @Transactional(readOnly = true)
  public PlanPreventifResponse findById(Long id) {
    return toResponse(require(id));
  }

  @Transactional
  public PlanPreventifResponse create(PlanPreventifRequest request, Long userId) {
    equipementService.require(request.getFkEquipement());
    if (request.getFrequenceJours() < 1) {
      throw new BusinessException("La fréquence doit être d'au moins 1 jour");
    }
    PlanPreventif plan = PlanPreventif.builder()
        .fkEquipement(request.getFkEquipement())
        .libelle(request.getLibelle().trim())
        .frequenceJours(request.getFrequenceJours())
        .prochaineEcheance(request.getProchaineEcheance())
        .derniereExecution(request.getDerniereExecution())
        .actif(request.getActif() == null || request.getActif())
        .notes(trimToNull(request.getNotes()))
        .userCreateId(userId)
        .build();
    Long id = repository.insert(plan);
    return findById(id);
  }

  @Transactional
  public PlanPreventifResponse update(Long id, PlanPreventifRequest request, Long userId) {
    PlanPreventif existing = require(id);
    equipementService.require(request.getFkEquipement());
    existing.setFkEquipement(request.getFkEquipement());
    existing.setLibelle(request.getLibelle().trim());
    existing.setFrequenceJours(request.getFrequenceJours());
    existing.setProchaineEcheance(request.getProchaineEcheance());
    existing.setDerniereExecution(request.getDerniereExecution());
    existing.setActif(request.getActif() != null ? request.getActif() : existing.isActif());
    existing.setNotes(trimToNull(request.getNotes()));
    existing.setUserUpdateId(userId);
    repository.update(existing);
    return findById(id);
  }

  @Transactional
  public InterventionResponse genererIntervention(Long planId, Long userId) {
    PlanPreventif plan = require(planId);
    if (!plan.isActif()) {
      throw new BusinessException("Le plan préventif est inactif");
    }
    InterventionRequest request = new InterventionRequest();
    request.setFkEquipement(plan.getFkEquipement());
    request.setTypeIntervention("PREVENTIVE");
    request.setPriorite("NORMALE");
    request.setStatut("PLANIFIEE");
    request.setTitre("Préventif : " + plan.getLibelle());
    request.setDescription("Généré depuis le plan #" + plan.getId() + " — " + plan.getLibelle());
    InterventionResponse created = interventionService.create(request, userId);

    LocalDate today = LocalDate.now();
    plan.setDerniereExecution(today);
    plan.setProchaineEcheance(today.plusDays(plan.getFrequenceJours()));
    plan.setUserUpdateId(userId);
    repository.update(plan);
    return created;
  }

  private PlanPreventif require(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> NotFoundException.entity("PlanPreventif", id));
  }

  private PlanPreventifResponse toResponse(PlanPreventif p) {
    return PlanPreventifResponse.builder()
        .id(p.getId())
        .fkEquipement(p.getFkEquipement())
        .equipementCode(p.getEquipementCode())
        .equipementDesignation(p.getEquipementDesignation())
        .libelle(p.getLibelle())
        .frequenceJours(p.getFrequenceJours())
        .prochaineEcheance(p.getProchaineEcheance())
        .derniereExecution(p.getDerniereExecution())
        .actif(p.isActif())
        .enRetard(p.isEnRetard())
        .notes(p.getNotes())
        .dateCreate(p.getDateCreate())
        .dateUpdate(p.getDateUpdate())
        .build();
  }

  private static String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
