package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Résolution / validation de la couverture (PEC + catégorie Mediline) depuis l'affiliation ERP.
 */
@Service
public class AffiliationCoverageService {

  private final ErpFacturationRelationalStore store;
  private final JdbcTemplate medilineJdbc;

  public AffiliationCoverageService(
      ErpFacturationRelationalStore store,
      @Autowired(required = false) @Qualifier("medilineJdbcTemplate") JdbcTemplate medilineJdbc) {
    this.store = store;
    this.medilineJdbc = medilineJdbc;
  }

  public Map<String, Object> preview(Map<String, Object> body) {
    return resolve(body, false);
  }

  /** Valide et retourne la couverture ; lève 400 si incomplete / incohérente. */
  public Map<String, Object> requireValid(Map<String, Object> body) {
    Map<String, Object> coverage = resolve(body, true);
    @SuppressWarnings("unchecked")
    List<String> errors = (List<String>) coverage.getOrDefault("errors", List.of());
    if (!errors.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errors.getFirst());
    }
    return coverage;
  }

  public Map<String, Object> resolve(Map<String, Object> body, boolean strict) {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    Long societeId = longOrNull(body == null ? null : body.get("societeId"));
    Long contratId = longOrNull(body == null ? null : body.get("contratId"));
    String relation = str(body == null ? null : body.get("relation"));
    if (relation == null) {
      relation = "A";
    }
    String titulaireNom = str(body == null ? null : body.get("titulaireNom"));
    String titulaireIpp = str(body == null ? null : body.get("titulaireIpp"));

    Map<String, Object> societe = null;
    if (societeId == null) {
      errors.add("La société est obligatoire.");
    } else {
      societe = store.findSocieteDetail(societeId).orElse(null);
      if (societe == null) {
        errors.add("Société introuvable.");
      }
    }

    Map<String, Object> contrat = null;
    if (contratId == null) {
      errors.add("Le contrat est obligatoire.");
    } else {
      contrat = store.findContratDetail(contratId).orElse(null);
      if (contrat == null) {
        errors.add("Contrat introuvable.");
      }
    }

    if (societe != null && !store.societeMedilineMappingReady(societeId)) {
      errors.add("La société sélectionnée n’est pas reliée à un compte Mediline.");
    }

    if (!AffiliationCoverageCalculator.isPrincipal(relation)) {
      if (titulaireNom == null && titulaireIpp != null) {
        titulaireNom = resolveTitulaireNom(titulaireIpp).orElse(null);
      }
      if (titulaireNom == null && longOrNull(body.get("titulaireId")) != null) {
        Long titulaireId = longOrNull(body.get("titulaireId"));
        var tit = store.findAyantDetail(titulaireId);
        if (tit.isPresent()) {
          titulaireNom = extractFamilyName(str(tit.get().get("nomComplet")));
          if (titulaireIpp == null) {
            titulaireIpp = str(tit.get().get("medilineIpp"));
          }
        }
      }
      if (titulaireNom == null) {
        errors.add("Sélectionnez le titulaire pour générer la prise en charge.");
      }
    }

    String societeLibelle = societe == null ? null : str(societe.get("designation"));
    if (societeLibelle == null && societe != null) {
      societeLibelle = str(societe.get("code"));
    }
    String pec = AffiliationCoverageCalculator.computePec(societeLibelle, relation, titulaireNom);

    String libelleBeneficiaire = contrat == null ? null : str(contrat.get("libelleBeneficiaire"));
    String categorieCode = contrat == null ? null : str(contrat.get("medilineCategorieCode"));
    String categorieIntitule = null;

    if (contrat != null) {
      if (libelleBeneficiaire == null) {
        errors.add("Le libellé bénéficiaire du contrat n’est pas configuré.");
      }
      if (categorieCode == null) {
        errors.add("La catégorie Mediline du contrat n’est pas configurée.");
      } else {
        categorieIntitule = resolveCategorieIntitule(categorieCode).orElse(null);
        if (categorieIntitule == null && medilineJdbc != null) {
          errors.add("La catégorie Mediline « " + categorieCode + " » est introuvable.");
        }
      }
    }

    String compteCode = societeId == null ? null : store.medilineCompteCodeForSociete(societeId).orElse(null);
    String compteCategorie = null;
    if (compteCode != null && medilineJdbc != null) {
      compteCategorie = loadCompteCategorie(compteCode).orElse(null);
      if (categorieCode != null && compteCategorie != null
          && !categorieCode.equalsIgnoreCase(compteCategorie.trim())) {
        errors.add(
            "Divergence catégorie : le contrat impose « "
                + categorieCode
                + " » alors que le compte Mediline « "
                + compteCode
                + " » attend « "
                + compteCategorie.trim()
                + " ». Corrigez la correspondance ERP.");
      }
    }

    if (pec == null && errors.stream().noneMatch(e -> e.contains("titulaire") || e.contains("société") || e.contains("Société"))) {
      errors.add("Impossible de générer la PEC.");
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("pec", pec);
    result.put("relation", relation);
    result.put("titulaireNom", titulaireNom);
    result.put("titulaireIpp", titulaireIpp);
    result.put("societeId", societeId);
    result.put("societeDesignation", societeLibelle);
    result.put("contratId", contratId);
    result.put("contratCode", contrat == null ? null : contrat.get("code"));
    result.put("contratDesignation", contrat == null ? null : contrat.get("designation"));
    result.put("libelleBeneficiaire", libelleBeneficiaire);
    result.put("medilineCategorieCode", categorieCode);
    result.put("medilineCategorieIntitule", categorieIntitule);
    result.put("compteCode", compteCode);
    result.put("compteCategoriePatient", compteCategorie);
    result.put("complete", errors.isEmpty());
    result.put("errors", errors);
    result.put("warnings", warnings);
    if (strict && !errors.isEmpty()) {
      // caller may throw; preview keeps errors in payload
    }
    return result;
  }

  public List<Map<String, Object>> listMedilineCategories() {
    if (medilineJdbc == null) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Mediline indisponible.");
    }
    return medilineJdbc.query(
        """
            SELECT CODE AS code, INTITULE AS intitule
            FROM t_categorie_patient
            WHERE CODE <> '' AND CODE <> '-1' AND CHAR_LENGTH(CODE) <= 5
            ORDER BY INTITULE, CODE
            LIMIT 200
            """,
        (rs, i) -> {
          Map<String, Object> row = new LinkedHashMap<>();
          row.put("code", trim(rs.getString("code")));
          row.put("intitule", trim(rs.getString("intitule")));
          return row;
        });
  }

  public Optional<String> resolveTitulaireNom(String ipp) {
    if (ipp == null || ipp.isBlank() || medilineJdbc == null) {
      return Optional.empty();
    }
    try {
      List<String> names = medilineJdbc.query(
          """
              SELECT NOM FROM t_personne
              WHERE CODE = ? AND PERSONNE_PHYSIQUE = 1
              LIMIT 1
              """,
          (rs, i) -> rs.getString(1),
          ipp.trim());
      return names.stream().findFirst().map(AffiliationCoverageCalculator::normalizeLabel);
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  public Optional<String> resolveCategorieIntitule(String code) {
    if (code == null || code.isBlank() || medilineJdbc == null) {
      return Optional.empty();
    }
    try {
      List<String> rows = medilineJdbc.query(
          """
              SELECT INTITULE FROM t_categorie_patient WHERE CODE = ? LIMIT 1
              """,
          (rs, i) -> rs.getString(1),
          code.trim());
      return rows.stream().findFirst().map(AffiliationCoverageCalculator::normalizeLabel);
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  private Optional<String> loadCompteCategorie(String compteCode) {
    try {
      List<String> rows = medilineJdbc.query(
          """
              SELECT CATEGORIE_PATIENT FROM t_compte WHERE CODE = ? LIMIT 1
              """,
          (rs, i) -> rs.getString(1),
          compteCode);
      return rows.stream().findFirst().map(s -> s == null ? null : s.trim()).filter(s -> s != null && !s.isEmpty());
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  /** Extrait le nom de famille (premier token) d'un nom complet ERP. */
  public static String extractFamilyName(String nomComplet) {
    String normalized = AffiliationCoverageCalculator.normalizeLabel(nomComplet);
    if (normalized == null) {
      return null;
    }
    int space = normalized.indexOf(' ');
    return space < 0 ? normalized : normalized.substring(0, space);
  }

  private static Long longOrNull(Object raw) {
    if (raw == null) {
      return null;
    }
    if (raw instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(raw).trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static String str(Object raw) {
    if (raw == null) {
      return null;
    }
    String text = String.valueOf(raw).trim();
    return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
  }

  private static String trim(String raw) {
    return raw == null ? null : raw.trim();
  }
}
