package cd.shad.erp.cmk.cmkerp.stocks.commandes.application.service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model.CommandesStatuts.BonCommande;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model.CommandesStatuts.DemandeCotation;

/**
 * Transitions de statut autorisées (cotations / bons de commande).
 */
public final class StatutTransitionRules {

  private StatutTransitionRules() {}

  private static final Map<DemandeCotation, Set<DemandeCotation>> DEMANDE = Map.of(
      DemandeCotation.BROUILLON, EnumSet.of(
          DemandeCotation.EN_VALIDATION_INTERNE, DemandeCotation.APPROUVEE, DemandeCotation.ANNULEE),
      DemandeCotation.EN_VALIDATION_INTERNE, EnumSet.of(
          DemandeCotation.APPROUVEE, DemandeCotation.BROUILLON, DemandeCotation.ANNULEE),
      DemandeCotation.APPROUVEE, EnumSet.of(DemandeCotation.ENVOYEE, DemandeCotation.ANNULEE),
      DemandeCotation.ENVOYEE, EnumSet.of(
          DemandeCotation.EN_ANALYSE, DemandeCotation.ATTRIBUEE, DemandeCotation.ANNULEE, DemandeCotation.CLOTUREE),
      DemandeCotation.EN_ANALYSE, EnumSet.of(DemandeCotation.ATTRIBUEE, DemandeCotation.ANNULEE, DemandeCotation.CLOTUREE),
      DemandeCotation.ATTRIBUEE, EnumSet.of(DemandeCotation.CLOTUREE),
      DemandeCotation.CLOTUREE, EnumSet.noneOf(DemandeCotation.class),
      DemandeCotation.ANNULEE, EnumSet.noneOf(DemandeCotation.class));

  private static final Map<BonCommande, Set<BonCommande>> BON = Map.ofEntries(
      Map.entry(BonCommande.BROUILLON, EnumSet.of(BonCommande.EN_VALIDATION, BonCommande.ANNULE)),
      Map.entry(BonCommande.EN_VALIDATION, EnumSet.of(BonCommande.VALIDE, BonCommande.BROUILLON, BonCommande.ANNULE)),
      Map.entry(BonCommande.VALIDE, EnumSet.of(BonCommande.ENVOYE, BonCommande.ANNULE)),
      Map.entry(BonCommande.ENVOYE, EnumSet.of(BonCommande.CONFIRME, BonCommande.EN_RETARD, BonCommande.ANNULE)),
      Map.entry(BonCommande.CONFIRME, EnumSet.of(BonCommande.PARTIELLEMENT_LIVRE, BonCommande.TOTALEMENT_LIVRE, BonCommande.EN_RETARD, BonCommande.ANNULE)),
      Map.entry(BonCommande.PARTIELLEMENT_LIVRE, EnumSet.of(BonCommande.TOTALEMENT_LIVRE, BonCommande.EN_RETARD, BonCommande.CLOTURE)),
      Map.entry(BonCommande.TOTALEMENT_LIVRE, EnumSet.of(BonCommande.CLOTURE)),
      Map.entry(BonCommande.EN_RETARD, EnumSet.of(BonCommande.PARTIELLEMENT_LIVRE, BonCommande.TOTALEMENT_LIVRE, BonCommande.CONFIRME, BonCommande.ANNULE)),
      Map.entry(BonCommande.CLOTURE, EnumSet.noneOf(BonCommande.class)),
      Map.entry(BonCommande.ANNULE, EnumSet.noneOf(BonCommande.class)));

  public static void assertDemandeTransition(String from, String to) {
    DemandeCotation f = DemandeCotation.valueOf(from);
    DemandeCotation t = DemandeCotation.valueOf(to);
    Set<DemandeCotation> allowed = DEMANDE.getOrDefault(f, Set.of());
    if (!allowed.contains(t)) {
      throw new BusinessException("Transition demande interdite : " + from + " → " + to);
    }
  }

  public static void assertBonTransition(String from, String to) {
    BonCommande f = BonCommande.valueOf(from);
    BonCommande t = BonCommande.valueOf(to);
    Set<BonCommande> allowed = BON.getOrDefault(f, Set.of());
    if (!allowed.contains(t)) {
      throw new BusinessException("Transition bon de commande interdite : " + from + " → " + to);
    }
  }

  /** Mappe une action REST (valider, envoyer, confirmer, etc.) vers un statut cible. */
  public static String targetStatutForAction(String current, String action) {
    String a = action != null ? action.trim().toLowerCase() : "";
    return switch (a) {
      case "soumettre-validation", "en-validation" -> BonCommande.EN_VALIDATION.name();
      case "valider" -> BonCommande.VALIDE.name();
      case "envoyer" -> BonCommande.ENVOYE.name();
      case "confirmer" -> BonCommande.CONFIRME.name();
      case "cloturer" -> BonCommande.CLOTURE.name();
      case "annuler" -> BonCommande.ANNULE.name();
      case "marquer-retard" -> BonCommande.EN_RETARD.name();
      default -> throw new BusinessException("Action de transition inconnue : " + action);
    };
  }
}
