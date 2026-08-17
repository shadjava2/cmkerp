package cd.shad.erp.cmk.cmkerp.stocks.commandes.application.service;

import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.request.PortailOffreDraftRequest;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.request.PortailProfilPropositionRequest;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model.*;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.repository.CommandesRepository;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.util.Sha256Hasher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Portail public fournisseur — pas de JWT ; session via X-Portail-Session.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PortailFournisseurService {

  private static final int MAX_UNLOCK_ATTEMPTS = 5;
  private static final int LOCK_MINUTES = 15;
  private static final int SESSION_HOURS = 8;

  private final CommandesRepository repo;
  private final MoneyConversionService moneyConversionService;
  private final ObjectMapper objectMapper;

  public Map<String, Object> getInvitationMeta(String publicToken) {
    InvitationFournisseur inv = requireInvitation(publicToken);
    DemandeCotation d = repo.findDemandeById(inv.getFkDemandeCotation())
        .orElseThrow(() -> NotFoundException.entity("DemandeCotation", inv.getFkDemandeCotation()));
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("publicToken", publicToken);
    m.put("fournisseurNom", repo.findFournisseurNom(inv.getFkFournisseur()));
    m.put("demandeNumero", d.getNumero());
    m.put("demandeObjet", d.getObjet());
    m.put("dateLimiteReponse", d.getDateLimiteReponse());
    m.put("statutInvitation", inv.getStatut());
    m.put("requiresUnlock", true);
    // jamais d'offres concurrentes
    return m;
  }

  public Map<String, Object> unlock(String publicToken, String code) {
    InvitationFournisseur inv = requireInvitation(publicToken);
    if (inv.getUnlockLockedUntil() != null && inv.getUnlockLockedUntil().isAfter(LocalDateTime.now())) {
      throw new BusinessException("Trop de tentatives — réessayez plus tard");
    }
    String hash = Sha256Hasher.sha256Hex(code != null ? code.trim() : "");
    if (!Objects.equals(hash, inv.getAccessCodeHash())) {
      int attempts = (inv.getUnlockAttempts() != null ? inv.getUnlockAttempts() : 0) + 1;
      inv.setUnlockAttempts(attempts);
      if (attempts >= MAX_UNLOCK_ATTEMPTS) {
        inv.setUnlockLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
        inv.setUnlockAttempts(0);
      }
      repo.updateInvitation(inv);
      throw new BusinessException("Code d'accès invalide");
    }
    String session = Sha256Hasher.generateSessionToken();
    inv.setSessionTokenHash(Sha256Hasher.sha256Hex(session));
    inv.setSessionExpiresAt(LocalDateTime.now().plusHours(SESSION_HOURS));
    inv.setUnlockAttempts(0);
    inv.setUnlockLockedUntil(null);
    if (inv.getOpenedAt() == null) {
      inv.setOpenedAt(LocalDateTime.now());
    }
    if ("ENVOYEE".equals(inv.getStatut()) || "CREEE".equals(inv.getStatut())) {
      inv.setStatut("OUVERTE");
    }
    repo.updateInvitation(inv);

    DemandeCotation d = repo.findDemandeById(inv.getFkDemandeCotation()).orElseThrow();
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("sessionToken", session);
    m.put("expiresAt", inv.getSessionExpiresAt());
    m.put("fournisseurNom", repo.findFournisseurNom(inv.getFkFournisseur()));
    m.put("demandeNumero", d.getNumero());
    m.put("demandeObjet", d.getObjet());
    return m;
  }

  public Map<String, Object> getDemande(String publicToken, String sessionToken) {
    InvitationFournisseur inv = requireSession(publicToken, sessionToken);
    DemandeCotation d = repo.findDemandeById(inv.getFkDemandeCotation()).orElseThrow();
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("numero", d.getNumero());
    m.put("objet", d.getObjet());
    m.put("description", d.getDescription());
    m.put("dateLimiteReponse", d.getDateLimiteReponse());
    m.put("dateLivraisonSouhaitee", d.getDateLivraisonSouhaitee());
    m.put("lieuLivraison", d.getLieuLivraison());
    m.put("conditions", d.getConditions());
    List<Map<String, Object>> lignes = new ArrayList<>();
    for (LigneDemandeCotation l : repo.findLignesDemande(d.getId())) {
      Map<String, Object> lm = new LinkedHashMap<>();
      lm.put("id", l.getId());
      lm.put("produitNom", repo.findProduitNom(l.getFkProduit()));
      lm.put("quantite", l.getQuantite());
      lm.put("specifications", l.getSpecifications());
      lm.put("categorieNom", null);
      lignes.add(lm);
    }
    m.put("lignes", lignes);
    OffreFournisseur offre = repo.findOffreByInvitation(inv.getId()).orElse(null);
    m.put("offre", offre != null ? toOwnOffre(offre) : null);
    boolean peutSoumettre = offre == null || "BROUILLON".equals(offre.getStatut()) || "REOUVERTE".equals(offre.getStatut());
    m.put("peutSoumettre", peutSoumettre);
    m.put("peutDemanderReouverture", offre != null && "SOUMISE".equals(offre.getStatut()));
    return m;
  }

  public Map<String, Object> saveDraft(String publicToken, String sessionToken, PortailOffreDraftRequest req) {
    InvitationFournisseur inv = requireSession(publicToken, sessionToken);
    OffreFournisseur offre = repo.findOffreByInvitation(inv.getId()).orElseGet(() -> {
      OffreFournisseur o = OffreFournisseur.builder()
          .fkInvitation(inv.getId())
          .fkDemandeCotation(inv.getFkDemandeCotation())
          .fkFournisseur(inv.getFkFournisseur())
          .statut("BROUILLON")
          .versionNo(1)
          .build();
      repo.saveOffre(o);
      return o;
    });
    if ("SOUMISE".equals(offre.getStatut()) && offre.getLockedAt() != null) {
      throw new BusinessException("Offre verrouillée — demandez une réouverture");
    }
    offre.setDevise(req.getDevise());
    offre.setTauxDeclare(req.getTauxDeclare());
    offre.setValiditeJusquau(req.getValiditeJusquau());
    offre.setFraisLivraison(req.getFraisLivraison());
    offre.setConditions(req.getConditions());
    if (!"REOUVERTE".equals(offre.getStatut())) {
      offre.setStatut("BROUILLON");
    }
    repo.updateOffre(offre);
    repo.deleteLignesOffre(offre.getId());
    if (req.getLignes() != null) {
      for (PortailOffreDraftRequest.LigneOffreDraft lr : req.getLignes()) {
        String devise = lr.getDevise() != null ? lr.getDevise() : req.getDevise();
        MoneyConversionService.ConvertedPrice cp =
            moneyConversionService.convert(lr.getPrixOriginal(), devise, null);
        repo.saveLigneOffre(LigneOffreFournisseur.builder()
            .fkOffre(offre.getId())
            .fkLigneDemande(lr.getFkLigneDemande())
            .prixOriginal(cp.prixOriginal())
            .devise(cp.devise())
            .taux(cp.taux())
            .fkEchangeDevise(cp.fkEchangeDevise())
            .prixUsd(cp.prixUsd())
            .prixCdf(cp.prixCdf())
            .quantiteDisponible(lr.getQuantiteDisponible())
            .delaiJours(lr.getDelaiJours())
            .substitution(lr.getSubstitution())
            .commentaire(lr.getCommentaire())
            .build());
      }
    }
    inv.setStatut("BROUILLON_OFFRE");
    repo.updateInvitation(inv);
    return toOwnOffre(offre);
  }

  public Map<String, Object> submit(String publicToken, String sessionToken, String idempotenceKey) {
    InvitationFournisseur inv = requireSession(publicToken, sessionToken);
    OffreFournisseur offre = repo.findOffreByInvitation(inv.getId())
        .orElseThrow(() -> new BusinessException("Aucun brouillon d'offre"));
    if ("SOUMISE".equals(offre.getStatut()) && Objects.equals(idempotenceKey, offre.getIdempotenceSubmitKey())) {
      return toOwnOffre(offre); // idempotent
    }
    if ("SOUMISE".equals(offre.getStatut()) && offre.getLockedAt() != null) {
      throw new BusinessException("Offre déjà soumise");
    }
    List<LigneOffreFournisseur> lignes = repo.findLignesOffre(offre.getId());
    if (lignes.isEmpty()) {
      throw new BusinessException("Impossible de soumettre une offre sans lignes");
    }
    try {
      String snapshot = objectMapper.writeValueAsString(Map.of(
          "offre", offre,
          "lignes", lignes));
      int version = offre.getVersionNo() != null ? offre.getVersionNo() : 1;
      repo.saveVersionOffre(offre.getId(), version, snapshot, null);
      offre.setVersionNo(version + 1);
    } catch (Exception e) {
      log.warn("Snapshot offre impossible: {}", e.getMessage());
    }
    offre.setStatut("SOUMISE");
    offre.setDateSoumission(LocalDateTime.now());
    offre.setLockedAt(LocalDateTime.now());
    offre.setIdempotenceSubmitKey(idempotenceKey != null ? idempotenceKey : "SUB-" + offre.getId());
    repo.updateOffre(offre);
    inv.setStatut("SOUMISE");
    inv.setSubmittedAt(LocalDateTime.now());
    repo.updateInvitation(inv);
    repo.insertOutbox("CMD_OFFRE_SOUMISE", "cmkerp-commandes", String.valueOf(offre.getId()),
        "{\"offreId\":" + offre.getId() + "}");
    return toOwnOffre(offre);
  }

  public void requestReopen(String publicToken, String sessionToken, String motif) {
    InvitationFournisseur inv = requireSession(publicToken, sessionToken);
    OffreFournisseur offre = repo.findOffreByInvitation(inv.getId())
        .orElseThrow(() -> new BusinessException("Aucune offre"));
    if (!"SOUMISE".equals(offre.getStatut())) {
      throw new BusinessException("Réouverture possible uniquement après soumission");
    }
    repo.saveDemandeReouverture(offre.getId(), motif);
    offre.setStatut("DEMANDE_REOUVERTURE");
    repo.updateOffre(offre);
  }

  public void requestProfileChange(String publicToken, String sessionToken, PortailProfilPropositionRequest req) {
    InvitationFournisseur inv = requireSession(publicToken, sessionToken);
    DemandeModifFournisseur d = DemandeModifFournisseur.builder()
        .fkFournisseur(inv.getFkFournisseur())
        .statut("EN_ATTENTE")
        .motif(req.getMotif())
        .build();
    repo.saveModif(d);
    if (req.getChamps() != null) {
      for (var c : req.getChamps()) {
        repo.saveChampModif(ChampModifFournisseur.builder()
            .fkDemandeModif(d.getId())
            .champ(c.getChamp())
            .valeurActuelle(c.getValeurActuelle())
            .valeurProposee(c.getValeurProposee())
            .build());
      }
    }
  }

  public Map<String, Object> uploadAttachment(String publicToken, String sessionToken, MultipartFile file) {
    InvitationFournisseur inv = requireSession(publicToken, sessionToken);
    OffreFournisseur offre = repo.findOffreByInvitation(inv.getId())
        .orElseThrow(() -> new BusinessException("Aucune offre"));
    String key = "cmd/" + offre.getId() + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
    // stockage logique (fichier réel hors scope — clé uniquement)
    repo.savePieceJointe(offre.getId(), file.getOriginalFilename(), file.getContentType(), file.getSize(), key);
    return Map.of("storageKey", key, "nomFichier", file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
  }

  /**
   * Sécurité : un fournisseur ne voit jamais les offres des autres.
   */
  public List<OffreFournisseur> listOffresVisibleForInvitation(Long invitationId) {
    InvitationFournisseur inv = repo.findInvitationById(invitationId)
        .orElseThrow(() -> NotFoundException.entity("Invitation", invitationId));
    return repo.findOffreByInvitation(inv.getId()).stream().toList();
  }

  public boolean canSeeCompetitorOffer(Long invitationId, Long otherOffreId) {
    OffreFournisseur other = repo.findOffreById(otherOffreId).orElse(null);
    if (other == null) {
      return false;
    }
    InvitationFournisseur inv = repo.findInvitationById(invitationId).orElse(null);
    if (inv == null) {
      return false;
    }
    return Objects.equals(other.getFkInvitation(), invitationId);
  }

  private InvitationFournisseur requireInvitation(String publicToken) {
    return repo.findInvitationByPublicToken(publicToken)
        .orElseThrow(() -> NotFoundException.entity("Invitation", publicToken));
  }

  private InvitationFournisseur requireSession(String publicToken, String sessionToken) {
    InvitationFournisseur inv = requireInvitation(publicToken);
    if (sessionToken == null || sessionToken.isBlank()) {
      throw new BusinessException("Session portail requise (header X-Portail-Session)");
    }
    String hash = Sha256Hasher.sha256Hex(sessionToken);
    if (!Objects.equals(hash, inv.getSessionTokenHash())) {
      throw new BusinessException("Session portail invalide");
    }
    if (inv.getSessionExpiresAt() != null && inv.getSessionExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BusinessException("Session portail expirée");
    }
    return inv;
  }

  private Map<String, Object> toOwnOffre(OffreFournisseur o) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", o.getId());
    m.put("fkInvitation", o.getFkInvitation());
    m.put("fkDemandeCotation", o.getFkDemandeCotation());
    m.put("fkFournisseur", o.getFkFournisseur());
    m.put("devise", o.getDevise());
    m.put("tauxDeclare", o.getTauxDeclare());
    m.put("validiteJusquau", o.getValiditeJusquau());
    m.put("fraisLivraison", o.getFraisLivraison());
    m.put("conditions", o.getConditions());
    m.put("statut", o.getStatut());
    m.put("versionNo", o.getVersionNo());
    m.put("dateSoumission", o.getDateSoumission());
    List<Map<String, Object>> lignes = new ArrayList<>();
    for (LigneOffreFournisseur l : repo.findLignesOffre(o.getId())) {
      Map<String, Object> lm = new LinkedHashMap<>();
      lm.put("id", l.getId());
      lm.put("fkLigneDemande", l.getFkLigneDemande());
      lm.put("prixOriginal", l.getPrixOriginal());
      lm.put("devise", l.getDevise());
      lm.put("taux", l.getTaux());
      lm.put("prixUsd", l.getPrixUsd());
      lm.put("prixCdf", l.getPrixCdf());
      lm.put("quantiteDisponible", l.getQuantiteDisponible());
      lm.put("delaiJours", l.getDelaiJours());
      lm.put("substitution", l.getSubstitution());
      lm.put("commentaire", l.getCommentaire());
      lignes.add(lm);
    }
    m.put("lignes", lignes);
    m.put("piecesJointes", repo.findPiecesJointes(o.getId()));
    return m;
  }
}
