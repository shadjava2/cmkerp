package cd.shad.erp.cmk.cmkerp.platform.external.clinique.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain.CliniqueLookupRepository;
import cd.shad.erp.cmk.cmkerp.platform.external.clinique.infrastructure.SqlServerProduitJdbcSupport;

/**
 * Écrit {@code TSTOCK.PAU} sur Beach et, si configuré, sur CH (même code produit).
 */
@Service
public class CliniquePauDualWriteService {

  private static final Logger log = LoggerFactory.getLogger(CliniquePauDualWriteService.class);

  private final CliniqueLookupRepository beach;
  private final SqlServerProduitJdbcSupport chSupport;

  public CliniquePauDualWriteService(
      CliniqueLookupRepository beach,
      @Autowired(required = false) @Qualifier("cliniqueChProduitSupport")
          SqlServerProduitJdbcSupport chSupport) {
    this.beach = beach;
    this.chSupport = chSupport;
  }

  /**
   * @return true si Beach a bien écrit ; un échec CH est loggé mais n'annule pas Beach
   *         (le résultat détaillé est dans {@link DualWriteResult}).
   */
  public DualWriteResult updatePau(String code, double pau) {
    boolean beachOk = beach.updatePau(code, pau);
    Boolean chOk = null;
    if (chSupport != null) {
      if (chSupport.isAvailable()) {
        chOk = chSupport.updatePau(code, pau);
        if (!Boolean.TRUE.equals(chOk)) {
          log.warn("PAU CH non écrit pour code={}", code);
        }
      } else {
        chOk = false;
        log.warn("CLINIQUE-CH indisponible pour PAU code={}", code);
      }
    }
    return new DualWriteResult(beachOk, chOk);
  }

  public boolean isChConfigured() {
    return chSupport != null;
  }

  public record DualWriteResult(boolean beachOk, Boolean chOk) {
    public boolean beachSucceeded() {
      return beachOk;
    }

    /** true si CH non configuré, ou écriture CH OK. */
    public boolean chSucceededOrSkipped() {
      return chOk == null || Boolean.TRUE.equals(chOk);
    }
  }
}
