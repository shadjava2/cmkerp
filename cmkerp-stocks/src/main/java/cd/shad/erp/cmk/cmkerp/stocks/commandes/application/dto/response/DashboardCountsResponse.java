package cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.response;

import java.util.Map;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardCountsResponse {
  private long cotationsBrouillon;
  private long cotationsOuvertes;
  private long offresEnAttente;
  private long offresSoumises;
  private long attributionsEnCours;
  private long bonsEnCours;
  private long bonsEnRetard;
  private long livraisonsPartielles;
  private long reliquatsOuverts;
  private long evaluationsEnAttente;
  private long modifsFournisseurEnAttente;

  public static DashboardCountsResponse from(Map<String, Long> m) {
    return DashboardCountsResponse.builder()
        .cotationsBrouillon(m.getOrDefault("cotationsBrouillon", 0L))
        .cotationsOuvertes(m.getOrDefault("cotationsOuvertes", 0L))
        .offresEnAttente(m.getOrDefault("offresEnAttente", 0L))
        .offresSoumises(m.getOrDefault("offresSoumises", 0L))
        .attributionsEnCours(m.getOrDefault("attributionsEnCours", 0L))
        .bonsEnCours(m.getOrDefault("bonsEnCours", 0L))
        .bonsEnRetard(m.getOrDefault("bonsEnRetard", 0L))
        .livraisonsPartielles(m.getOrDefault("livraisonsPartielles", 0L))
        .reliquatsOuverts(m.getOrDefault("reliquatsOuverts", 0L))
        .evaluationsEnAttente(m.getOrDefault("evaluationsEnAttente", 0L))
        .modifsFournisseurEnAttente(m.getOrDefault("modifsFournisseurEnAttente", 0L))
        .build();
  }
}
