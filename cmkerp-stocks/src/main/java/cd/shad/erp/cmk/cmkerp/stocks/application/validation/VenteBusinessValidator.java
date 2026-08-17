package cd.shad.erp.cmk.cmkerp.stocks.application.validation;

import org.springframework.stereotype.Component;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.request.VenteRequest;

/**
 * Validateur métier pour les ventes.
 *
 * Contient les règles de validation métier qui ne peuvent pas être exprimées via les annotations
 * Bean Validation standard.
 *
 * Architecture: Pattern Validator pour centraliser les règles métier complexes.
 */
@Component
public class VenteBusinessValidator {

  /**
   * Valide une requête de création/mise à jour de vente.
   *
   * @param request la requête à valider
   * @throws BusinessException si la validation échoue
   */
  public void validate(VenteRequest request) {
    if (request == null) {
      throw new BusinessException("La requête de vente ne peut pas être nulle");
    }

    // Validation de la pharmacie
    if (request.getFkPharmacie() == null || request.getFkPharmacie() <= 0) {
      throw new BusinessException("La pharmacie est obligatoire");
    }

    // Validation du demandeur
    if (request.getDemandeur() == null || request.getDemandeur().trim().isEmpty()) {
      throw new BusinessException("Le demandeur est obligatoire");
    }

    // Validation de la raison de sortie
    if (request.getRaisonsortie() == null || request.getRaisonsortie().trim().isEmpty()) {
      throw new BusinessException("La raison de sortie est obligatoire");
    }

    // Validation du taux (doit être entre 0 et 100)
    if (request.getTaux() != null && (request.getTaux() < 0 || request.getTaux() > 100)) {
      throw new BusinessException("Le taux doit être entre 0 et 100");
    }
  }

  /**
   * Valide qu'une vente peut être modifiée.
   *
   * @param currentStatut le statut actuel de la vente
   * @throws BusinessException si la vente ne peut pas être modifiée
   */
  public void validateCanBeModified(String currentStatut) {
    if (currentStatut == null) {
      return; // Statut non défini, on peut modifier
    }

    // Ventes validées ou annulées ne peuvent pas être modifiées
    if ("SORTIE-USAGE".equals(currentStatut) || "PAYEE".equals(currentStatut)
        || "FACTUREE".equals(currentStatut) || "ANNULEE".equals(currentStatut)
        || "ANNULEE-REMBOURSE".equals(currentStatut)) {
      throw new BusinessException(
          String.format("Impossible de modifier une vente avec le statut: %s", currentStatut));
    }
  }

  /**
   * Valide qu'une vente peut être validée.
   *
   * @param currentStatut le statut actuel de la vente
   * @throws BusinessException si la vente ne peut pas être validée
   */
  public void validateCanBeValidated(String currentStatut) {
    if (currentStatut == null) {
      return; // Statut non défini, on peut valider
    }

    // Seules les ventes en attente peuvent être validées
    if (!"EN_ATTENTE".equals(currentStatut)) {
      throw new BusinessException(
          String.format("Impossible de valider une vente avec le statut: %s", currentStatut));
    }
  }
}


























