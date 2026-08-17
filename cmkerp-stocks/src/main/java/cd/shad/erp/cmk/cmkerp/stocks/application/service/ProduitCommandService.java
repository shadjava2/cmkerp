package cd.shad.erp.cmk.cmkerp.stocks.application.service;

import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cd.shad.erp.cmk.cmkerp.sharedkernel.events.ProductUpdatedEvent;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.request.ProduitRequest;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.ProduitResponse;
import cd.shad.erp.cmk.cmkerp.stocks.domain.model.Produit;
import cd.shad.erp.cmk.cmkerp.stocks.domain.repository.CategorieProduitRepository;
import cd.shad.erp.cmk.cmkerp.stocks.domain.repository.ConditionnementRepository;
import cd.shad.erp.cmk.cmkerp.stocks.domain.repository.DosageRepository;
import cd.shad.erp.cmk.cmkerp.stocks.domain.repository.FormeRepository;
import cd.shad.erp.cmk.cmkerp.stocks.domain.repository.ProduitRepository;
import cd.shad.erp.cmk.cmkerp.stocks.domain.service.ProduitDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Service pour la gestion des produits (écriture uniquement).
 *
 * <p>
 * Ce service contient toutes les opérations de modification (commands) liées aux produits. Toutes
 * les méthodes modifient l'état du système.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProduitCommandService {

  private final ProduitRepository produitRepository;
  private final FormeRepository formeRepository;
  private final DosageRepository dosageRepository;
  private final ConditionnementRepository conditionnementRepository;
  private final CategorieProduitRepository categorieProduitRepository;
  private final ProduitDomainService produitDomainService;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Crée un nouveau produit.
   */
  public ProduitResponse create(ProduitRequest request, Long currentUserId) {
    log.debug("Création d'un nouveau produit: {}", request.getNomcommercial());

    // Validation métier via Domain Service
    produitDomainService.validerCreationProduit(request.getCodebarre());

    // Vérifier que les références existent si fournies
    if (request.getFkForme() != null) {
      formeRepository.findById(request.getFkForme())
          .orElseThrow(() -> NotFoundException.entity("Forme", request.getFkForme()));
    }
    if (request.getFkDosage() != null) {
      dosageRepository.findById(request.getFkDosage())
          .orElseThrow(() -> NotFoundException.entity("Dosage", request.getFkDosage()));
    }
    if (request.getFkConditionnement() != null) {
      conditionnementRepository.findById(request.getFkConditionnement()).orElseThrow(
          () -> NotFoundException.entity("Conditionnement", request.getFkConditionnement()));
    }
    if (request.getFkCategorie() != null) {
      categorieProduitRepository.findById(request.getFkCategorie()).orElseThrow(
          () -> NotFoundException.entity("CategorieProduit", request.getFkCategorie()));
    }

    // Créer l'agrégat Produit
    Produit produit = Produit.builder().codebarre(request.getCodebarre())
        .nomcommercial(request.getNomcommercial()).nomscientifique(request.getNomscientifique())
        .fkForme(request.getFkForme()).fkDosage(request.getFkDosage())
        .fkConditionnement(request.getFkConditionnement()).fkCategorie(request.getFkCategorie())
        .prixachat(request.getPrixachat()).prixachatcomptable(request.getPrixachatcomptable())
        .qtealert(request.getQtealert() != null ? request.getQtealert() : 0.0f)
        .qtcritique(request.getQtcritique() != null ? request.getQtcritique() : 0.0f)
        .perimable(request.getPerimable() != null ? request.getPerimable() : false)
        .userCreatedId(currentUserId).dateCreate(LocalDateTime.now()).build();

    // Utiliser les méthodes métier de l'agrégat
    if (request.getCodebarre() != null) {
      produit.changerCodebarre(request.getCodebarre());
    }
    if (request.getNomcommercial() != null) {
      produit.changerNomCommercial(request.getNomcommercial());
    }
    if (request.getNomscientifique() != null) {
      produit.changerNomScientifique(request.getNomscientifique());
    }
    if (request.getPrixachat() != null) {
      produit.mettreAJourPrixAchat(request.getPrixachat());
    }
    if (request.getPrixachatcomptable() != null) {
      produit.mettreAJourPrixAchatComptable(request.getPrixachatcomptable());
    }
    if (request.getQtealert() != null || request.getQtcritique() != null) {
      produit.mettreAJourQuantitesAlerte(request.getQtealert(), request.getQtcritique());
    }
    if (request.getPerimable() != null) {
      produit.definirPerimable(request.getPerimable());
    }
    if (request.getFkForme() != null) {
      produit.associerForme(request.getFkForme());
    }
    if (request.getFkDosage() != null) {
      produit.associerDosage(request.getFkDosage());
    }
    if (request.getFkConditionnement() != null) {
      produit.associerConditionnement(request.getFkConditionnement());
    }
    if (request.getFkCategorie() != null) {
      produit.associerCategorie(request.getFkCategorie());
    }

    // Sauvegarder via le repository
    Produit saved = produitRepository.save(produit);

    // Convertir en DTO pour l'événement
    ProduitResponse response = produitToResponse(saved);

    // Publier l'événement pour notifier via WebSocket
    eventPublisher.publishEvent(new ProductUpdatedEvent("CREATED", saved.getId(), response, currentUserId));

    log.info("Produit créé avec succès: ID={}, nomCommercial={}", saved.getId(),
        saved.getNomcommercial());
    return response;
  }

  /**
   * Met à jour uniquement les seuils d'alerte / critique (labels UI : Stock alerte / Stock critique).
   * Ne modifie pas le schéma — colonnes qtealert et qtcritique inchangées.
   */
  public void updateSeuils(Long id, Float qtealert, Float qtcritique, Long currentUserId) {
    Produit produit =
        produitRepository.findById(id).orElseThrow(() -> NotFoundException.entity("Produit", id));
    produit.mettreAJourQuantitesAlerte(
        qtealert != null ? qtealert : 0.0f,
        qtcritique != null ? qtcritique : 0.0f);
    produit.setUserUpdatedId(currentUserId);
    Produit updated = produitRepository.save(produit);
    ProduitResponse response = produitToResponse(updated);
    eventPublisher.publishEvent(new ProductUpdatedEvent("UPDATED", updated.getId(), response, currentUserId));
    log.info("Seuils produit {} : qtealert={}, qtcritique={}", id, qtealert, qtcritique);
  }

  /**
   * Met à jour un produit existant.
   */
  public ProduitResponse update(Long id, ProduitRequest request, Long currentUserId) {
    log.debug("Mise à jour du produit ID: {}", id);

    Produit produit =
        produitRepository.findById(id).orElseThrow(() -> NotFoundException.entity("Produit", id));

    // Validation métier via Domain Service
    produitDomainService.validerModificationProduit(produit, request.getCodebarre());

    // Vérifier que les références existent si modifiées
    if (request.getFkForme() != null && !request.getFkForme().equals(produit.getFkForme())) {
      formeRepository.findById(request.getFkForme())
          .orElseThrow(() -> NotFoundException.entity("Forme", request.getFkForme()));
    }
    if (request.getFkDosage() != null && !request.getFkDosage().equals(produit.getFkDosage())) {
      dosageRepository.findById(request.getFkDosage())
          .orElseThrow(() -> NotFoundException.entity("Dosage", request.getFkDosage()));
    }
    if (request.getFkConditionnement() != null
        && !request.getFkConditionnement().equals(produit.getFkConditionnement())) {
      conditionnementRepository.findById(request.getFkConditionnement()).orElseThrow(
          () -> NotFoundException.entity("Conditionnement", request.getFkConditionnement()));
    }
    if (request.getFkCategorie() != null
        && !request.getFkCategorie().equals(produit.getFkCategorie())) {
      categorieProduitRepository.findById(request.getFkCategorie()).orElseThrow(
          () -> NotFoundException.entity("CategorieProduit", request.getFkCategorie()));
    }

    // Mettre à jour les champs via les méthodes métier de l'agrégat
    if (request.getCodebarre() != null && !request.getCodebarre().equals(produit.getCodebarre())) {
      produit.changerCodebarre(request.getCodebarre());
    }
    if (request.getNomcommercial() != null
        && !request.getNomcommercial().equals(produit.getNomcommercial())) {
      produit.changerNomCommercial(request.getNomcommercial());
    }
    if (request.getNomscientifique() != null
        && !request.getNomscientifique().equals(produit.getNomscientifique())) {
      produit.changerNomScientifique(request.getNomscientifique());
    }
    if (request.getPrixachat() != null && !request.getPrixachat().equals(produit.getPrixachat())) {
      produit.mettreAJourPrixAchat(request.getPrixachat());
    }
    if (request.getPrixachatcomptable() != null
        && !request.getPrixachatcomptable().equals(produit.getPrixachatcomptable())) {
      produit.mettreAJourPrixAchatComptable(request.getPrixachatcomptable());
    }
    if (request.getQtealert() != null || request.getQtcritique() != null) {
      produit.mettreAJourQuantitesAlerte(
          request.getQtealert() != null ? request.getQtealert() : produit.getQtealert(),
          request.getQtcritique() != null ? request.getQtcritique() : produit.getQtcritique());
    }
    if (request.getPerimable() != null && !request.getPerimable().equals(produit.getPerimable())) {
      produit.definirPerimable(request.getPerimable());
    }
    if (request.getFkForme() != null && !request.getFkForme().equals(produit.getFkForme())) {
      produit.associerForme(request.getFkForme());
    }
    if (request.getFkDosage() != null && !request.getFkDosage().equals(produit.getFkDosage())) {
      produit.associerDosage(request.getFkDosage());
    }
    if (request.getFkConditionnement() != null
        && !request.getFkConditionnement().equals(produit.getFkConditionnement())) {
      produit.associerConditionnement(request.getFkConditionnement());
    }
    if (request.getFkCategorie() != null
        && !request.getFkCategorie().equals(produit.getFkCategorie())) {
      produit.associerCategorie(request.getFkCategorie());
    }

    produit.setUserUpdatedId(currentUserId);

    // Sauvegarder via le repository
    Produit updated = produitRepository.save(produit);

    // Convertir en DTO pour l'événement
    ProduitResponse response = produitToResponse(updated);

    // Publier l'événement pour notifier via WebSocket
    eventPublisher.publishEvent(new ProductUpdatedEvent("UPDATED", updated.getId(), response, currentUserId));

    log.info("Produit mis à jour avec succès: ID={}, nomCommercial={}", updated.getId(),
        updated.getNomcommercial());
    return response;
  }

  /**
   * Supprime un produit.
   */
  public void delete(Long id) {
    log.debug("Suppression du produit ID: {}", id);

    Produit produit =
        produitRepository.findById(id).orElseThrow(() -> NotFoundException.entity("Produit", id));

    Long productId = produit.getId();
    produitRepository.delete(produit);

    // Publier l'événement pour notifier via WebSocket
    eventPublisher.publishEvent(new ProductUpdatedEvent("DELETED", productId, null, null));

    log.info("Produit supprimé avec succès: ID={}", id);
  }

  /**
   * Convertit une Produit (domain) en ProduitResponse (DTO).
   */
  private ProduitResponse produitToResponse(Produit produit) {
    if (produit == null) {
      return null;
    }

    return ProduitResponse.builder().id(produit.getId()).codebarre(produit.getCodebarre())
        .nomcommercial(produit.getNomcommercial()).nomscientifique(produit.getNomscientifique())
        .fkForme(produit.getFkForme()).fkDosage(produit.getFkDosage())
        .fkConditionnement(produit.getFkConditionnement()).fkCategorie(produit.getFkCategorie())
        .prixachat(produit.getPrixachat()).prixachatcomptable(produit.getPrixachatcomptable())
        .qtealert(produit.getQtealert()).qtcritique(produit.getQtcritique())
        .perimable(produit.getPerimable()).dateCreate(produit.getDateCreate())
        .dateUpdate(produit.getDateUpdate()).userCreatedId(produit.getUserCreatedId())
        .userUpdatedId(produit.getUserUpdatedId()).build();
  }
}

