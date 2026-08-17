package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovAnomalyDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovDetailDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovGroupStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovKpiDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovListItemDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovPeriodStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovProduitHistoryDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovSearchCriteria;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.ConditionalOnStockIntelligenceEnabled;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.ApprovAnalyticsRepository;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnStockIntelligenceEnabled
@RequiredArgsConstructor
public class ApprovAnalyticsService {

  private final ApprovAnalyticsRepository repository;

  public ApprovKpiDTO kpis(ApprovSearchCriteria c) {
    return repository.computeKpis(c);
  }

  public List<ApprovListItemDTO> list(ApprovSearchCriteria c) {
    return repository.searchList(c);
  }

  public ApprovDetailDTO detail(long id) {
    ApprovDetailDTO d = repository.findDetail(id);
    if (d == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Approvisionnement introuvable");
    }
    return d;
  }

  public List<ApprovGroupStatDTO> byFournisseur(ApprovSearchCriteria c) {
    return repository.groupByFournisseur(c);
  }

  public List<ApprovGroupStatDTO> byPharmacie(ApprovSearchCriteria c) {
    return repository.groupByPharmacie(c);
  }

  public List<ApprovGroupStatDTO> byStatut(ApprovSearchCriteria c) {
    return repository.groupByStatut(c);
  }

  public List<ApprovGroupStatDTO> byUtilisateur(ApprovSearchCriteria c) {
    return repository.groupByUtilisateur(c);
  }

  public List<ApprovGroupStatDTO> topProduits(ApprovSearchCriteria c, boolean rare) {
    return repository.topProduits(c, rare);
  }

  public List<ApprovPeriodStatDTO> mensuelle(ApprovSearchCriteria c) {
    return repository.synthèseMensuelle(c);
  }

  public List<ApprovPeriodStatDTO> annuelle(ApprovSearchCriteria c) {
    return repository.synthèseAnnuelle(c);
  }

  public List<ApprovProduitHistoryDTO> historiqueProduit(long produitId, ApprovSearchCriteria c) {
    return repository.historiqueProduit(produitId, c);
  }

  public List<ApprovAnomalyDTO> anomalies(ApprovSearchCriteria c) {
    return repository.findAnomalies(c);
  }

  public List<Map<String, Object>> lookupFournisseurs(String q, int limit, Long pharmacieId, String scope) {
    return repository.lookupFournisseurs(q, limit, pharmacieId, scope);
  }

  public List<Map<String, Object>> lookupUtilisateurs(String q, int limit, Long pharmacieId, String scope) {
    return repository.lookupUtilisateurs(q, limit, pharmacieId, scope);
  }

  public List<Map<String, Object>> lookupProduits(String q, int limit, Long pharmacieId, String scope) {
    return repository.lookupProduits(q, limit, pharmacieId, scope);
  }

  public static ApprovSearchCriteria fromParams(
      LocalDate dateDebut,
      LocalDate dateFin,
      Long fournisseurId,
      Long pharmacieId,
      Long utilisateurId,
      Long produitId,
      String statut,
      String reference,
      String produitQ,
      BigDecimal montantMin,
      BigDecimal montantMax,
      String scope,
      String preset,
      String anomalyType,
      int limit,
      int offset) {
    return new ApprovSearchCriteria(
        dateDebut, dateFin, fournisseurId, pharmacieId, utilisateurId, produitId,
        statut, reference, produitQ, montantMin, montantMax,
        scope != null ? scope : "CENTRALE",
        preset, anomalyType,
        limit > 0 ? limit : 50,
        Math.max(offset, 0));
  }
}
