package cd.shad.erp.cmk.cmkerp.stocks.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain.CliniquePrixInfo;
import cd.shad.erp.cmk.cmkerp.platform.external.clinique.infrastructure.SqlServerProduitJdbcSupport;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.request.FacturationSyncApplyRequest;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.FacturationSyncApplyResult;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.FacturationSyncCompareResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Comparaison / synchro TPRODUIT + TSTOCK entre Beach et CH (même schéma CLINIQUE).
 * Ne touche pas à la fusion ERP ↔ Beach.
 */
@Service
@Slf4j
public class FacturationSyncService {

  private static final BigDecimal EPS = new BigDecimal("0.005");
  private static final int MAX_SCAN = 40_000;

  private final SqlServerProduitJdbcSupport beachSupport;
  private final SqlServerProduitJdbcSupport chSupport;

  public FacturationSyncService(
      @Autowired(required = false) @Qualifier("cliniqueBeachProduitSupport")
          SqlServerProduitJdbcSupport beachSupport,
      @Autowired(required = false) @Qualifier("cliniqueChProduitSupport")
          SqlServerProduitJdbcSupport chSupport,
      @Autowired(required = false) @Qualifier("cliniqueJdbcTemplate") JdbcTemplate beachJdbc,
      @Autowired(required = false) @Qualifier("cliniqueChJdbcTemplate") JdbcTemplate chJdbc) {
    this.beachSupport =
        beachSupport != null
            ? beachSupport
            : (beachJdbc != null ? new SqlServerProduitJdbcSupport(beachJdbc, "CLINIQUE-BEACH") : null);
    this.chSupport =
        chSupport != null
            ? chSupport
            : (chJdbc != null ? new SqlServerProduitJdbcSupport(chJdbc, "CLINIQUE-CH") : null);
  }

  public FacturationSyncCompareResponse compare(String query) {
    SqlServerProduitJdbcSupport beach = beachSupport;
    SqlServerProduitJdbcSupport ch = chSupport;
    boolean beachOk = beach != null && beach.isAvailable();
    boolean chOk = ch != null && ch.isAvailable();
    if (beach == null) {
      log.warn("Sync facturation: support Beach absent (bean cliniqueBeachProduitSupport / cliniqueJdbcTemplate)");
    }
    if (ch == null) {
      log.warn("Sync facturation: support CH absent (bean cliniqueChProduitSupport / cliniqueChJdbcTemplate)");
    }

    if (!beachOk || !chOk) {
      return FacturationSyncCompareResponse.builder()
          .beachAvailable(beachOk)
          .chAvailable(chOk)
          .build();
    }

    Map<String, CliniquePrixInfo> beachByCode = indexByCode(beach.listAllPrix(MAX_SCAN));
    Map<String, CliniquePrixInfo> chByCode = indexByCode(ch.listAllPrix(MAX_SCAN));

    String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

    List<FacturationSyncCompareResponse.PriceDiffItem> diffs = new ArrayList<>();
    List<FacturationSyncCompareResponse.MissingProductItem> missingOnBeach = new ArrayList<>();
    List<FacturationSyncCompareResponse.MissingProductItem> missingOnCh = new ArrayList<>();
    int equal = 0;

    Set<String> all = new HashSet<>();
    all.addAll(beachByCode.keySet());
    all.addAll(chByCode.keySet());

    for (String code : all) {
      CliniquePrixInfo b = beachByCode.get(code);
      CliniquePrixInfo c = chByCode.get(code);
      if (!matchesQuery(q, code, b, c)) {
        continue;
      }
      if (b == null) {
        missingOnBeach.add(toMissing(c));
        continue;
      }
      if (c == null) {
        missingOnCh.add(toMissing(b));
        continue;
      }
      BigDecimal pb = pauBd(b.pau());
      BigDecimal pc = pauBd(c.pau());
      BigDecimal ecart = pb.subtract(pc).setScale(4, RoundingMode.HALF_UP);
      if (ecart.abs().compareTo(EPS) <= 0) {
        equal++;
      } else {
        diffs.add(FacturationSyncCompareResponse.PriceDiffItem.builder()
            .code(code)
            .designation(prefer(b.designation(), c.designation()))
            .forme(prefer(b.forme(), c.forme()))
            .dosage(prefer(b.dosage(), c.dosage()))
            .pauBeach(b.pau())
            .pauCh(c.pau())
            .ecart(ecart.doubleValue())
            .build());
      }
    }

    diffs.sort((a, x) -> String.valueOf(a.getCode()).compareToIgnoreCase(String.valueOf(x.getCode())));
    missingOnBeach.sort((a, x) -> String.valueOf(a.getCode()).compareToIgnoreCase(String.valueOf(x.getCode())));
    missingOnCh.sort((a, x) -> String.valueOf(a.getCode()).compareToIgnoreCase(String.valueOf(x.getCode())));

    return FacturationSyncCompareResponse.builder()
        .beachAvailable(true)
        .chAvailable(true)
        .priceDiffCount(diffs.size())
        .missingOnBeachCount(missingOnBeach.size())
        .missingOnChCount(missingOnCh.size())
        .equalPriceCount(equal)
        .priceDiffs(diffs.size() > 3000 ? diffs.subList(0, 3000) : diffs)
        .missingOnBeach(missingOnBeach.size() > 3000 ? missingOnBeach.subList(0, 3000) : missingOnBeach)
        .missingOnCh(missingOnCh.size() > 3000 ? missingOnCh.subList(0, 3000) : missingOnCh)
        .build();
  }

  public FacturationSyncApplyResult apply(FacturationSyncApplyRequest request) {
    SqlServerProduitJdbcSupport beach = require(beachSupport, "Beach");
    SqlServerProduitJdbcSupport ch = require(chSupport, "CH");
    if (!beach.isAvailable()) {
      throw new BusinessException("Serveur facturation Beach indisponible");
    }
    if (!ch.isAvailable()) {
      throw new BusinessException("Serveur facturation CH indisponible");
    }

    List<String> codes = request.getCodes() == null ? List.of() : request.getCodes();
    List<String> failures = new ArrayList<>();
    int success = 0;
    int skipped = 0;

    for (String raw : codes) {
      String code = raw == null ? "" : raw.trim();
      if (code.isBlank()) {
        skipped++;
        continue;
      }
      try {
        boolean ok = switch (request.getAction()) {
          case PRICE_BEACH_TO_CH -> syncPrice(beach, ch, code);
          case PRICE_CH_TO_BEACH -> syncPrice(ch, beach, code);
          case IMPORT_CH_TO_BEACH -> importProduit(ch, beach, code);
          case IMPORT_BEACH_TO_CH -> importProduit(beach, ch, code);
        };
        if (ok) {
          success++;
        } else {
          failures.add(code + " : opération échouée");
        }
      } catch (Exception ex) {
        failures.add(code + " : " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
      }
    }

    return FacturationSyncApplyResult.builder()
        .requested(codes.size())
        .successCount(success)
        .skippedCount(skipped)
        .failureCount(failures.size())
        .failures(failures)
        .build();
  }

  private boolean syncPrice(SqlServerProduitJdbcSupport source, SqlServerProduitJdbcSupport target, String code) {
    List<CliniquePrixInfo> infos = source.findPrixByCodes(List.of(code));
    if (infos.isEmpty() || infos.get(0).pau() == null) {
      return false;
    }
    return target.updatePau(code, infos.get(0).pau());
  }

  private boolean importProduit(SqlServerProduitJdbcSupport source, SqlServerProduitJdbcSupport target, String code) {
    Map<String, Object> row = source.loadProduitForCopy(code);
    if (row == null) {
      return false;
    }
    return target.upsertProduitFromRow(row);
  }

  private static SqlServerProduitJdbcSupport require(SqlServerProduitJdbcSupport s, String label) {
    if (s == null) {
      throw new BusinessException("Datasource facturation " + label + " non configurée");
    }
    return s;
  }

  private static Map<String, CliniquePrixInfo> indexByCode(List<CliniquePrixInfo> list) {
    Map<String, CliniquePrixInfo> map = new HashMap<>();
    for (CliniquePrixInfo info : list) {
      if (info.code() != null && !info.code().isBlank()) {
        map.put(info.code().trim(), info);
      }
    }
    return map;
  }

  private static boolean matchesQuery(String q, String code, CliniquePrixInfo b, CliniquePrixInfo c) {
    if (q == null || q.isBlank()) {
      return true;
    }
    if (code.toLowerCase(Locale.ROOT).contains(q)) {
      return true;
    }
    return contains(q, b) || contains(q, c);
  }

  private static boolean contains(String q, CliniquePrixInfo info) {
    if (info == null) {
      return false;
    }
    return containsField(q, info.designation())
        || containsField(q, info.forme())
        || containsField(q, info.dosage());
  }

  private static boolean containsField(String q, String v) {
    return v != null && v.toLowerCase(Locale.ROOT).contains(q);
  }

  private static FacturationSyncCompareResponse.MissingProductItem toMissing(CliniquePrixInfo info) {
    return FacturationSyncCompareResponse.MissingProductItem.builder()
        .code(info.code())
        .designation(info.designation())
        .forme(info.forme())
        .dosage(info.dosage())
        .pau(info.pau())
        .stock(info.stock())
        .build();
  }

  private static BigDecimal pauBd(Double pau) {
    return pau == null ? BigDecimal.ZERO : BigDecimal.valueOf(pau);
  }

  private static String prefer(String a, String b) {
    if (a != null && !a.isBlank()) {
      return a;
    }
    return b;
  }
}
