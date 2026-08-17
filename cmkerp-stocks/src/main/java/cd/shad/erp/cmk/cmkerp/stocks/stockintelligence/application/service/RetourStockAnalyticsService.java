package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockAnomalyDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockDetailDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockGroupStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockKpiDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockListItemDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockPeriodStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockProduitHistoryDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockSearchCriteria;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.ConditionalOnStockIntelligenceEnabled;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.RetourStockAnalyticsRepository;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnStockIntelligenceEnabled
@RequiredArgsConstructor
public class RetourStockAnalyticsService {

  private final RetourStockAnalyticsRepository repository;

  public RetourStockKpiDTO kpis(RetourStockSearchCriteria c) {
    return repository.computeKpis(c);
  }

  public List<RetourStockListItemDTO> list(RetourStockSearchCriteria c) {
    return repository.searchList(c);
  }

  public RetourStockDetailDTO detail(long id) {
    RetourStockDetailDTO d = repository.findDetail(id);
    if (d == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Retour stock introuvable");
    }
    return d;
  }

  public List<RetourStockGroupStatDTO> byPharmacieSource(RetourStockSearchCriteria c) {
    return repository.groupByPharmacieSource(c);
  }

  public List<RetourStockGroupStatDTO> byPharmacieDestination(RetourStockSearchCriteria c) {
    return repository.groupByPharmacieDestination(c);
  }

  public List<RetourStockGroupStatDTO> byStatut(RetourStockSearchCriteria c) {
    return repository.groupByStatut(c);
  }

  public List<RetourStockGroupStatDTO> byStatutReception(RetourStockSearchCriteria c) {
    return repository.groupByStatutReception(c);
  }

  public List<RetourStockGroupStatDTO> byPerime(RetourStockSearchCriteria c) {
    return repository.groupByPerime(c);
  }

  public List<RetourStockGroupStatDTO> byUtilisateur(RetourStockSearchCriteria c) {
    return repository.groupByUtilisateur(c);
  }

  public List<RetourStockGroupStatDTO> topProduits(RetourStockSearchCriteria c, boolean rare) {
    return repository.topProduits(c, rare);
  }

  public List<RetourStockPeriodStatDTO> mensuelle(RetourStockSearchCriteria c) {
    return repository.synthèseMensuelle(c);
  }

  public List<RetourStockPeriodStatDTO> annuelle(RetourStockSearchCriteria c) {
    return repository.synthèseAnnuelle(c);
  }

  public List<RetourStockProduitHistoryDTO> historiqueProduit(long produitId, RetourStockSearchCriteria c) {
    return repository.historiqueProduit(produitId, c);
  }

  public List<RetourStockAnomalyDTO> anomalies(RetourStockSearchCriteria c) {
    return repository.findAnomalies(c);
  }

  public List<Map<String, Object>> lookupPharmaciesSource(String q, int limit, Long pharmacieId, String scope) {
    return repository.lookupPharmaciesSource(q, limit, pharmacieId, scope);
  }

  public List<Map<String, Object>> lookupPharmaciesDestination(String q, int limit, Long pharmacieId, String scope) {
    return repository.lookupPharmaciesDestination(q, limit, pharmacieId, scope);
  }

  public List<Map<String, Object>> lookupUtilisateurs(String q, int limit, Long pharmacieId, String scope) {
    return repository.lookupUtilisateurs(q, limit, pharmacieId, scope);
  }

  public List<Map<String, Object>> lookupProduits(String q, int limit, Long pharmacieId, String scope) {
    return repository.lookupProduits(q, limit, pharmacieId, scope);
  }

  public static RetourStockSearchCriteria fromParams(
      LocalDate dateDebut,
      LocalDate dateFin,
      Long pharmacieSourceId,
      Long pharmacieDestinationId,
      Long utilisateurId,
      Long produitId,
      String statut,
      String statutReception,
      String reference,
      String produitQ,
      Boolean perime,
      BigDecimal quantiteMin,
      BigDecimal quantiteMax,
      String scope,
      String preset,
      String anomalyType,
      boolean tousStatuts,
      int limit,
      int offset) {
    return new RetourStockSearchCriteria(
        dateDebut, dateFin, pharmacieSourceId, pharmacieDestinationId, utilisateurId, produitId,
        statut, statutReception, reference, produitQ, perime,
        quantiteMin, quantiteMax,
        scope != null ? scope : "CENTRALE",
        preset, anomalyType, tousStatuts,
        limit > 0 ? limit : 50,
        Math.max(offset, 0));
  }
}
