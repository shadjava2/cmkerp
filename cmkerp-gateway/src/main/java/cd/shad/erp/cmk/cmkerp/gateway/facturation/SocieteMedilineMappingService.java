package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Persistance exclusive MySQL ERP des correspondances société ↔ compte Mediline.
 *
 * <p>Utilise {@code REQUIRES_NEW} : les filtres JWT/RBAC ouvrent des transactions JPA
 * {@code readOnly} qui laissent la connexion en {@code autocommit=0}. Sans transaction
 * d'écriture dédiée, l'{@code UPDATE} est exécuté puis annulé par {@code ROLLBACK}.
 */
@Service
public class SocieteMedilineMappingService {

  private final ErpFacturationRelationalStore store;

  public SocieteMedilineMappingService(ErpFacturationRelationalStore store) {
    this.store = store;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Map<String, Object> bind(long societeId, String compteCode, String compteIntitule, Long verifiedBy) {
    if (compteCode == null || compteCode.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Compte Mediline obligatoire.");
    }
    int updated = store.bindSocieteMedilineCompte(societeId, compteCode, compteIntitule, verifiedBy);
    if (updated == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Société introuvable.");
    }
    Map<String, Object> row = store.findSocieteMedilineMapping(societeId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Société introuvable."));
    if (!"MATCHED".equalsIgnoreCase(String.valueOf(row.get("medilineMappingStatus")))
        || row.get("medilineCompteCode") == null
        || String.valueOf(row.get("medilineCompteCode")).isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "La correspondance n’a pas été persistée. Réessayez.");
    }
    return row;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Map<String, Object> clear(long societeId) {
    int updated = store.clearSocieteMedilineCompte(societeId);
    if (updated == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Société introuvable.");
    }
    Map<String, Object> row = store.findSocieteMedilineMapping(societeId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Société introuvable."));
    if (!"UNMATCHED".equalsIgnoreCase(String.valueOf(row.get("medilineMappingStatus")))
        || (row.get("medilineCompteCode") != null
            && !String.valueOf(row.get("medilineCompteCode")).isBlank())) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Le retrait de correspondance n’a pas été persisté. Réessayez.");
    }
    return row;
  }
}
