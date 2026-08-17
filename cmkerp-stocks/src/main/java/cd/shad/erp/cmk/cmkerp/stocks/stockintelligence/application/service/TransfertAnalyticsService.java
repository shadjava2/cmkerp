package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.TransfertAnomalyDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.TransfertDetailDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.TransfertGroupStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.TransfertKpiDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.TransfertListItemDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.TransfertPeriodStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.TransfertProduitHistoryDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.TransfertSearchCriteria;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.ConditionalOnStockIntelligenceEnabled;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.TransfertAnalyticsRepository;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnStockIntelligenceEnabled
@RequiredArgsConstructor
public class TransfertAnalyticsService {

  private final TransfertAnalyticsRepository repository;

  public TransfertKpiDTO kpis(TransfertSearchCriteria c) {
    return repository.computeKpis(c);
  }

  public List<TransfertListItemDTO> list(TransfertSearchCriteria c) {
    return repository.searchList(c);
  }

  public TransfertDetailDTO detail(long id) {
    TransfertDetailDTO d = repository.findDetail(id);
    if (d == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transfert introuvable");
    }
    return d;
  }

  public List<TransfertGroupStatDTO> byPharmacieDestination(TransfertSearchCriteria c) {
    return repository.groupByPharmacieDestination(c);
  }

  public List<TransfertGroupStatDTO> byPharmacieSource(TransfertSearchCriteria c) {
    return repository.groupByPharmacieSource(c);
  }

  public List<TransfertGroupStatDTO> byStatut(TransfertSearchCriteria c) {
    return repository.groupByStatut(c);
  }

  public List<TransfertGroupStatDTO> byUtilisateur(TransfertSearchCriteria c) {
    return repository.groupByUtilisateur(c);
  }

  public List<TransfertGroupStatDTO> topProduits(TransfertSearchCriteria c, boolean rare) {
    return repository.topProduits(c, rare);
  }

  public List<TransfertPeriodStatDTO> mensuelle(TransfertSearchCriteria c) {
    return repository.synthèseMensuelle(c);
  }

  public List<TransfertPeriodStatDTO> annuelle(TransfertSearchCriteria c) {
    return repository.synthèseAnnuelle(c);
  }

  public List<TransfertProduitHistoryDTO> historiqueProduit(long produitId, TransfertSearchCriteria c) {
    return repository.historiqueProduit(produitId, c);
  }

  public List<TransfertAnomalyDTO> anomalies(TransfertSearchCriteria c) {
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

  public static TransfertSearchCriteria fromParams(
      LocalDate dateDebut,
      LocalDate dateFin,
      Long pharmacieSourceId,
      Long pharmacieDestinationId,
      Long utilisateurId,
      Long produitId,
      String statut,
      String reference,
      String produitQ,
      BigDecimal quantiteMin,
      BigDecimal quantiteMax,
      String scope,
      String preset,
      String anomalyType,
      boolean tousStatuts,
      int limit,
      int offset) {
    return new TransfertSearchCriteria(
        dateDebut, dateFin, pharmacieSourceId, pharmacieDestinationId, utilisateurId, produitId,
        statut, reference, produitQ, quantiteMin, quantiteMax,
        scope != null ? scope : "CENTRALE",
        preset, anomalyType, tousStatuts,
        limit > 0 ? limit : 50,
        Math.max(offset, 0));
  }
}
