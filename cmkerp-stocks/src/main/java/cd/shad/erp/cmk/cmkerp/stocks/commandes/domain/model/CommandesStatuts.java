package cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model;

/**
 * Statuts métier du module Commandes fournisseurs.
 */
public final class CommandesStatuts {

  private CommandesStatuts() {}

  public enum DemandeCotation {
    BROUILLON, EN_VALIDATION_INTERNE, APPROUVEE, ENVOYEE, EN_ANALYSE, ATTRIBUEE, CLOTUREE, ANNULEE
  }

  public enum Invitation {
    CREEE, ENVOYEE, OUVERTE, BROUILLON_OFFRE, SOUMISE, EXPIREE, REVOQUEE, REOUVERTE
  }

  public enum Offre {
    BROUILLON, SOUMISE, DEMANDE_REOUVERTURE, REOUVERTE
  }

  public enum BonCommande {
    BROUILLON, EN_VALIDATION, VALIDE, ENVOYE, CONFIRME,
    PARTIELLEMENT_LIVRE, TOTALEMENT_LIVRE, CLOTURE, EN_RETARD, ANNULE
  }

  public enum Reception {
    BROUILLON, VALIDEE, ANNULEE
  }

  public enum ModifFournisseur {
    EN_ATTENTE, APPROUVEE, PARTIELLE, REFUSEE, PRECISIONS
  }

  public enum Reouverture {
    EN_ATTENTE, APPROUVEE, REFUSEE, APPROUVEE_AVEC_DELAI
  }

  public enum ScopeAttribution {
    GLOBAL, CATEGORIE, LIGNE
  }
}
