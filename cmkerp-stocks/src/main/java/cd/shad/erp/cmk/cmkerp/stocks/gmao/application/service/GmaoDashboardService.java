package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.service;

import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response.GmaoDashboardStatsResponse;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.infrastructure.persistence.EquipementJdbcRepository;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.infrastructure.persistence.InterventionJdbcRepository;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.infrastructure.persistence.PlanPreventifJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GmaoDashboardService {

  private final EquipementJdbcRepository equipementJdbcRepository;
  private final InterventionJdbcRepository interventionJdbcRepository;
  private final PlanPreventifJdbcRepository planPreventifJdbcRepository;

  @Transactional(readOnly = true)
  public GmaoDashboardStatsResponse stats() {
    return GmaoDashboardStatsResponse.builder()
        .equipementsActifs(equipementJdbcRepository.countActifs())
        .equipementsEnPanne(equipementJdbcRepository.countByStatut("EN_PANNE"))
        .equipementsEnMaintenance(equipementJdbcRepository.countByStatut("EN_MAINTENANCE"))
        .interventionsOuvertes(interventionJdbcRepository.countOpen())
        .interventionsEnCours(interventionJdbcRepository.countByStatut("EN_COURS"))
        .plansEnRetard(planPreventifJdbcRepository.countEnRetard())
        .build();
  }
}
