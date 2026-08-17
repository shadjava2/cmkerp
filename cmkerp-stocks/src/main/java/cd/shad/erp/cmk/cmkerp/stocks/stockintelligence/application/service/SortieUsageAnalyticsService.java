package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageAnomalyDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageDetailDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageGroupStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageKpiDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageListItemDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsagePeriodStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageProduitHistoryDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageSearchCriteria;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.ConditionalOnStockIntelligenceEnabled;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.SortieUsageAnalyticsRepository;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnStockIntelligenceEnabled
@RequiredArgsConstructor
public class SortieUsageAnalyticsService {

  private final SortieUsageAnalyticsRepository repository;

  public SortieUsageKpiDTO kpis(SortieUsageSearchCriteria c) {
    return repository.computeKpis(c);
  }

  public List<SortieUsageListItemDTO> list(SortieUsageSearchCriteria c) {
    return repository.searchList(c);
  }

  public SortieUsageDetailDTO detail(long id) {
    SortieUsageDetailDTO d = repository.findDetail(id);
    if (d == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sortie usage introuvable");
    }
    return d;
  }

  public List<SortieUsageGroupStatDTO> byPharmacie(SortieUsageSearchCriteria c) {
    return repository.groupByPharmacie(c);
  }

  public List<SortieUsageGroupStatDTO> byDemandeur(SortieUsageSearchCriteria c) {
    return repository.groupByDemandeur(c);
  }

  public List<SortieUsageGroupStatDTO> byRaisonSortie(SortieUsageSearchCriteria c) {
    return repository.groupByRaisonSortie(c);
  }

  public List<SortieUsageGroupStatDTO> byStatut(SortieUsageSearchCriteria c) {
    return repository.groupByStatut(c);
  }

  public List<SortieUsageGroupStatDTO> byUtilisateur(SortieUsageSearchCriteria c) {
    return repository.groupByUtilisateur(c);
  }

  public List<SortieUsageGroupStatDTO> topProduits(SortieUsageSearchCriteria c, boolean rare) {
    return repository.topProduits(c, rare);
  }

  public List<SortieUsagePeriodStatDTO> mensuelle(SortieUsageSearchCriteria c) {
    return repository.synthèseMensuelle(c);
  }

  public List<SortieUsagePeriodStatDTO> annuelle(SortieUsageSearchCriteria c) {
    return repository.synthèseAnnuelle(c);
  }

  public List<SortieUsageProduitHistoryDTO> historiqueProduit(long produitId, SortieUsageSearchCriteria c) {
    return repository.historiqueProduit(produitId, c);
  }

  public List<SortieUsageAnomalyDTO> anomalies(SortieUsageSearchCriteria c) {
    return repository.findAnomalies(c);
  }

  public List<Map<String, Object>> lookupPharmacies(String q, int limit, Long pharmacieId, String scope) {
    return repository.lookupPharmacies(q, limit, pharmacieId, scope);
  }

  public List<Map<String, Object>> lookupUtilisateurs(String q, int limit, Long pharmacieId, String scope) {
    return repository.lookupUtilisateurs(q, limit, pharmacieId, scope);
  }

  public List<Map<String, Object>> lookupProduits(String q, int limit, Long pharmacieId, String scope) {
    return repository.lookupProduits(q, limit, pharmacieId, scope);
  }

  public List<Map<String, Object>> lookupDemandeurs(String q, int limit, Long pharmacieId, String scope) {
    return repository.lookupDemandeurs(q, limit, pharmacieId, scope);
  }

  public static SortieUsageSearchCriteria fromParams(
      LocalDate dateDebut,
      LocalDate dateFin,
      Long pharmacieId,
      Long utilisateurId,
      Long produitId,
      String statut,
      String reference,
      String produitQ,
      String demandeur,
      String raisonSortie,
      BigDecimal quantiteMin,
      BigDecimal quantiteMax,
      BigDecimal montantMin,
      BigDecimal montantMax,
      String scope,
      String preset,
      String anomalyType,
      boolean tousStatuts,
      int limit,
      int offset) {
    return new SortieUsageSearchCriteria(
        dateDebut, dateFin, pharmacieId, utilisateurId, produitId,
        statut, reference, produitQ, demandeur, raisonSortie,
        quantiteMin, quantiteMax, montantMin, montantMax,
        scope != null ? scope : "CENTRALE",
        preset, anomalyType, tousStatuts,
        limit > 0 ? limit : 50,
        Math.max(offset, 0));
  }
}
