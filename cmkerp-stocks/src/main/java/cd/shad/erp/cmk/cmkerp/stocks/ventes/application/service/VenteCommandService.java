package cd.shad.erp.cmk.cmkerp.stocks.ventes.application.service;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.request.SortieUsageRequest;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.request.VenteRequest;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.response.VenteResponse;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.mapper.VenteMapper;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.model.Vente;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.repository.VenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Service pour la gestion des ventes (écriture uniquement).
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class VenteCommandService {

  private static final String ENTITY_NAME = "Vente";

  private final VenteRepository venteRepository;
  private final VenteMapper venteMapper;
  private final JdbcTemplate jdbcTemplate;

  /**
   * Crée une nouvelle vente.
   */
  public VenteResponse create(VenteRequest request, Long currentUserId) {
    log.debug("Création d'une nouvelle vente pour la pharmacie: {}", request.getFkPharmacie());

    // Vérifier que la pharmacie existe
    verifyPharmacieExists(request.getFkPharmacie());

    // Créer l'entité Vente
    Vente vente = venteMapper.toEntity(request);
    vente.setUserCreatedId(currentUserId);
    vente.setDateCreate(LocalDateTime.now());

    // Sauvegarder
    int rows = venteRepository.save(vente);
    if (rows == 0) {
      throw new BusinessException("Échec de la création de la vente");
    }

    log.info("Vente créée avec succès: ID: {}", vente.getId());

    // Récupérer la vente créée avec les désignations
    Vente created = venteRepository.findById(vente.getId())
        .orElseThrow(() -> new BusinessException("Vente créée mais introuvable"));

    String pharmacieNom = getPharmacieNom(created.getFkPharmacie());

    return venteMapper.toResponse(created, pharmacieNom);
  }

  /**
   * Met à jour une vente existante.
   */
  public VenteResponse update(Long id, VenteRequest request, Long currentUserId) {
    log.debug("Mise à jour de la vente ID: {}", id);

    Vente vente =
        venteRepository.findById(id).orElseThrow(() -> NotFoundException.entity(ENTITY_NAME, id));

    // Vérifier que la vente peut être modifiée (pas validée ou annulée)
    if (vente.getStatut() == Vente.StatutVente.SORTIE_USAGE) {
      throw new BusinessException("Impossible de modifier une vente validée");
    }
    if (vente.getStatut() == Vente.StatutVente.ANNULEE
        || vente.getStatut() == Vente.StatutVente.ANNULEE_REMBOURSE) {
      throw new BusinessException("Impossible de modifier une vente annulée");
    }

    // Vérifier les références si fournies
    if (request.getFkPharmacie() != null) {
      verifyPharmacieExists(request.getFkPharmacie());
    }

    // Mettre à jour l'entité
    venteMapper.updateEntityFromRequest(request, vente);
    vente.setUserUpdatedId(currentUserId);
    vente.setDateUpdate(LocalDateTime.now());

    int rows = venteRepository.update(vente);
    if (rows == 0) {
      throw new BusinessException("Échec de la mise à jour de la vente");
    }

    log.info("Vente mise à jour avec succès: ID: {}", id);

    // Récupérer la vente mise à jour avec les désignations
    Vente updated = venteRepository.findById(id)
        .orElseThrow(() -> new BusinessException("Vente mise à jour mais introuvable"));

    String pharmacieNom = getPharmacieNom(updated.getFkPharmacie());

    return venteMapper.toResponse(updated, pharmacieNom);
  }

  /**
   * Valide une vente avec un statut dynamique.
   *
   * @param id ID de la vente à valider
   * @param currentUserId ID de l'utilisateur qui valide
   * @param statut Statut de validation (optionnel, par défaut: SORTIE-USAGE)
   * @throws BusinessException si la validation échoue
   * @throws IllegalStateException si la vente est déjà validée
   */
  public void valider(Long id, Long currentUserId, String statut) {
    log.info("🚀 [VenteCommandService] Validation de la vente ID: {} avec statut: {}", id,
        statut != null ? statut : "SORTIE-USAGE (défaut)");

    Vente vente =
        venteRepository.findById(id).orElseThrow(() -> NotFoundException.entity(ENTITY_NAME, id));

    log.info("📋 [VenteCommandService] Statut actuel de la vente: {} ({})", vente.getStatut(),
        vente.getStatut().getDbValue());

    vente.valider(currentUserId, statut);

    log.info("✅ [VenteCommandService] Statut après validation: {} ({})", vente.getStatut(),
        vente.getStatut().getDbValue());

    int rows = venteRepository.update(vente);
    if (rows == 0) {
      throw new BusinessException("Échec de la validation de la vente");
    }

    log.info("✅ [VenteCommandService] Vente validée avec succès: ID: {}, statut final: {} ({})", id,
        vente.getStatut(), vente.getStatut().getDbValue());
  }

  /**
   * Confirme une sortie pour usage (statut SORTIE-USAGE).
   * Même logique que {@link #annuler(Long)} : stored procedure si activée, sinon domaine + stock.
   * Décrémente le stock (via SP_VALIDATE_VENTE), enregistre raisonsortie / demandeur.
   */
  public void sortiePourUsage(Long id, SortieUsageRequest request, Long currentUserId) {
    log.debug("Sortie pour usage de la vente ID: {}", id);

    String raisonsortie = request != null ? request.getRaisonsortie() : null;
    String demandeur = request != null ? request.getDemandeur() : null;

    // Mettre à jour raison / demandeur avant validation stock si fournis
    if ((raisonsortie != null && !raisonsortie.isBlank())
        || (demandeur != null && !demandeur.isBlank())) {
      Vente vente = venteRepository.findById(id)
          .orElseThrow(() -> NotFoundException.entity(ENTITY_NAME, id));
      if (raisonsortie != null && !raisonsortie.isBlank()) {
        vente.setRaisonsortie(raisonsortie.trim());
      }
      if (demandeur != null && !demandeur.isBlank()) {
        vente.setDemandeur(demandeur.trim());
      }
      vente.setUserUpdatedId(currentUserId);
      vente.setDateUpdate(LocalDateTime.now());
      venteRepository.update(vente);
    }

    Vente vente =
        venteRepository.findById(id).orElseThrow(() -> NotFoundException.entity(ENTITY_NAME, id));

    vente.sortiePourUsage(currentUserId, raisonsortie, demandeur);

    int rows = venteRepository.update(vente);
    if (rows == 0) {
      throw new BusinessException("Échec de la sortie pour usage");
    }

    log.info("Sortie pour usage confirmée: ID: {}", id);
  }

  /**
   * Annule une vente (passe le statut à ANNULEE). Possible seulement dans les 24h après validation.
   */
  public void annuler(Long id, Long currentUserId) {
    log.debug("Annulation de la vente ID: {}", id);

    Vente vente =
        venteRepository.findById(id).orElseThrow(() -> NotFoundException.entity(ENTITY_NAME, id));

    vente.annuler(currentUserId);

    int rows = venteRepository.update(vente);
    if (rows == 0) {
      throw new BusinessException("Échec de l'annulation de la vente");
    }

    log.info("Vente annulée avec succès: ID: {}", id);
  }

  /**
   * Annule une vente avec remboursement (passe le statut à ANNULEE-REMBOURSE). Possible seulement
   * dans les 24h après validation.
   */
  public void annulerAvecRemboursement(Long id, Long currentUserId) {
    log.debug("Annulation avec remboursement de la vente ID: {}", id);

    Vente vente =
        venteRepository.findById(id).orElseThrow(() -> NotFoundException.entity(ENTITY_NAME, id));

    vente.annulerAvecRemboursement(currentUserId);

    int rows = venteRepository.update(vente);
    if (rows == 0) {
      throw new BusinessException("Échec de l'annulation avec remboursement de la vente");
    }

    log.info("Vente annulée avec remboursement avec succès: ID: {}", id);
  }

  private void verifyPharmacieExists(Long fkPharmacie) {
    String sql = "SELECT COUNT(*) FROM pharmacies WHERE id = ?";
    Long count = jdbcTemplate.queryForObject(sql, Long.class, fkPharmacie);
    if (count == null || count == 0) {
      throw NotFoundException.entity("Pharmacie", fkPharmacie);
    }
  }

  private String getPharmacieNom(Long fkPharmacie) {
    if (fkPharmacie == null) {
      return null;
    }
    String sql = "SELECT designation FROM pharmacies WHERE id = ?";
    try {
      return jdbcTemplate.queryForObject(sql, String.class, fkPharmacie);
    } catch (Exception e) {
      log.warn("Pharmacie non trouvée pour ID: {}", fkPharmacie);
      return null;
    }
  }
}

