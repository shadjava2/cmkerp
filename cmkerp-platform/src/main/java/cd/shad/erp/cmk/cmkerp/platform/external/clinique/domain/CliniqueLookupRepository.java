package cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain;

import java.util.List;
import java.util.Map;

/**
 * Accès lecture CLINIQUE (SQL Server). Aucune écriture / migration / DDL.
 * Les lignes sont renvoyées en {@code Map} tant que le mapping métier n'est pas figé.
 */
public interface CliniqueLookupRepository {

  /** Ping JDBC ({@code SELECT 1}). */
  boolean isAvailable();

  /** Nombre de lignes {@code dbo.TMEDICAMENT} (0 si table absente). */
  long countMedicaments();

  /** Lecture limitée {@code dbo.TMEDICAMENT} (SELECT TOP n *). */
  List<Map<String, Object>> findMedicaments(int limit);

  /** Lecture limitée {@code dbo.TARIF}. */
  List<Map<String, Object>> findTarifs(int limit);
}
