package cd.shad.erp.cmk.cmkerp.stocks.application.service;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.request.FournisseurRequest;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.FournisseurResponse;
import cd.shad.erp.cmk.cmkerp.stocks.domain.model.Fournisseur;
import cd.shad.erp.cmk.cmkerp.stocks.domain.repository.FournisseurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Service pour la gestion des fournisseurs (écriture uniquement).
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FournisseurCommandService {

  private final FournisseurRepository fournisseurRepository;

  /**
   * Crée un nouveau fournisseur.
   */
  public FournisseurResponse create(FournisseurRequest request, Long currentUserId) {
    log.debug("Création d'un nouveau fournisseur: {}", request.getNom());

    Fournisseur fournisseur = Fournisseur.builder().nom(request.getNom())
        .adresse(request.getAdresse()).telephone(request.getTelephone()).email(request.getEmail())
        .userCreatedId(currentUserId).dateCreate(LocalDateTime.now()).build();

    Long id = fournisseurRepository.save(fournisseur);
    fournisseur.setId(id);

    return toResponse(fournisseur);
  }

  /**
   * Met à jour un fournisseur existant.
   */
  public FournisseurResponse update(Long id, FournisseurRequest request, Long currentUserId) {
    log.debug("Mise à jour du fournisseur: {}", id);

    Fournisseur fournisseur = fournisseurRepository.findById(id)
        .orElseThrow(() -> NotFoundException.entity("Fournisseur", id));

    fournisseur.setNom(request.getNom());
    fournisseur.setAdresse(request.getAdresse());
    fournisseur.setTelephone(request.getTelephone());
    fournisseur.setEmail(request.getEmail());
    fournisseur.setUserUpdatedId(currentUserId);
    fournisseur.setDateUpdate(LocalDateTime.now());

    fournisseurRepository.update(fournisseur);

    return toResponse(fournisseur);
  }

  /**
   * Supprime un fournisseur.
   */
  public void delete(Long id) {
    log.debug("Suppression du fournisseur: {}", id);

    if (!fournisseurRepository.findById(id).isPresent()) {
      throw NotFoundException.entity("Fournisseur", id);
    }

    fournisseurRepository.deleteById(id);
  }

  /**
   * Convertit un modèle de domaine en DTO de réponse.
   */
  private FournisseurResponse toResponse(Fournisseur fournisseur) {
    return FournisseurResponse.builder().id(fournisseur.getId()).nom(fournisseur.getNom())
        .adresse(fournisseur.getAdresse()).telephone(fournisseur.getTelephone())
        .email(fournisseur.getEmail()).dateCreate(fournisseur.getDateCreate())
        .dateUpdate(fournisseur.getDateUpdate()).userCreatedId(fournisseur.getUserCreatedId())
        .userUpdatedId(fournisseur.getUserUpdatedId()).build();
  }
}

