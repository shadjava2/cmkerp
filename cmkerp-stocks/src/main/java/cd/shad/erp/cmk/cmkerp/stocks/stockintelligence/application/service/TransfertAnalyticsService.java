package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

  private static final String[] MOIS_ABBR = {
      "Jan", "Fév", "Mar", "Avr", "Mai", "Juin",
      "Juil", "Aoû", "Sep", "Oct", "Nov", "Déc"
  };

  /**
   * Rapport consommation (sorties transfert) mois × mois pour une année civile,
   * avec trimestres T1–T4 et total annuel.
   */
  public Map<String, Object> rapportConsommationAnnuelle(
      Integer annee, Long categorieId, Long pharmacieId) {
    if (categorieId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categorieId obligatoire");
    }
    int year = annee != null ? annee : LocalDate.now().getYear();
    if (year < 2000 || year > 2100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "année invalide");
    }

    String yy = String.format("%02d", year % 100);
    List<String> moisCols = new ArrayList<>();
    for (String abbr : MOIS_ABBR) {
      moisCols.add(abbr + "-" + yy);
    }
    List<String> colonnes = new ArrayList<>();
    for (int i = 0; i < 12; i++) {
      colonnes.add(moisCols.get(i));
      if (i == 2) {
        colonnes.add("T1");
      } else if (i == 5) {
        colonnes.add("T2");
      } else if (i == 8) {
        colonnes.add("T3");
      } else if (i == 11) {
        colonnes.add("T4");
      }
    }
    colonnes.add("total_" + year);

    List<Map<String, Object>> cells =
        repository.rapportConsommationMensuelleCells(year, categorieId, pharmacieId);

    Map<Long, Map<String, Object>> byProduit = new LinkedHashMap<>();
    for (Map<String, Object> cell : cells) {
      Long produitId = toLong(cell.get("produit_id"));
      if (produitId == null) {
        continue;
      }
      Map<String, Object> row =
          byProduit.computeIfAbsent(
              produitId,
              id -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("produitId", id);
                m.put("nomCommercial", str(cell.get("nom_commercial")));
                m.put("nomScientifique", str(cell.get("nom_scientifique")));
                m.put("forme", str(cell.get("forme")));
                m.put("dosage", str(cell.get("dosage")));
                m.put("conditionnement", str(cell.get("conditionnement")));
                m.put("prixAchat", cell.get("prix_achat"));
                m.put("stockActuel", cell.get("stock_actuel"));
                m.put("categorie", str(cell.get("categorie")));
                Map<String, Double> parMois = new LinkedHashMap<>();
                for (String col : moisCols) {
                  parMois.put(col, 0d);
                }
                m.put("parMois", parMois);
                m.put("t1", 0d);
                m.put("t2", 0d);
                m.put("t3", 0d);
                m.put("t4", 0d);
                m.put("total", 0d);
                return m;
              });

      Integer mois = toInt(cell.get("mois"));
      double qty = toDouble(cell.get("quantite"));
      if (mois == null || mois < 1 || mois > 12) {
        continue;
      }
      String col = moisCols.get(mois - 1);
      @SuppressWarnings("unchecked")
      Map<String, Double> parMois = (Map<String, Double>) row.get("parMois");
      parMois.put(col, parMois.getOrDefault(col, 0d) + qty);
      row.put("total", toDouble(row.get("total")) + qty);
      if (mois <= 3) {
        row.put("t1", toDouble(row.get("t1")) + qty);
      } else if (mois <= 6) {
        row.put("t2", toDouble(row.get("t2")) + qty);
      } else if (mois <= 9) {
        row.put("t3", toDouble(row.get("t3")) + qty);
      } else {
        row.put("t4", toDouble(row.get("t4")) + qty);
      }
    }

    List<Map<String, Object>> lignes = new ArrayList<>(byProduit.values());
    lignes.sort(
        (a, b) ->
            String.valueOf(a.get("nomCommercial"))
                .compareToIgnoreCase(String.valueOf(b.get("nomCommercial"))));

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("annee", year);
    out.put("categorieId", categorieId);
    out.put("pharmacieId", pharmacieId);
    out.put("mois", moisCols);
    out.put("colonnes", colonnes);
    out.put("lignes", lignes);
    return out;
  }

  private static Long toLong(Object v) {
    if (v == null) {
      return null;
    }
    if (v instanceof Long l) {
      return l;
    }
    if (v instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(v.toString());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static Integer toInt(Object v) {
    if (v == null) {
      return null;
    }
    if (v instanceof Integer i) {
      return i;
    }
    if (v instanceof Number n) {
      return n.intValue();
    }
    try {
      return Integer.parseInt(v.toString());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static double toDouble(Object v) {
    if (v == null) {
      return 0d;
    }
    if (v instanceof Number n) {
      return n.doubleValue();
    }
    try {
      return Double.parseDouble(v.toString());
    } catch (NumberFormatException e) {
      return 0d;
    }
  }

  private static String str(Object v) {
    return v == null ? null : v.toString();
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
