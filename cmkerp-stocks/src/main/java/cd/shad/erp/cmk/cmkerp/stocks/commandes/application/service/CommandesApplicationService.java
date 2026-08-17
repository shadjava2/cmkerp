package cd.shad.erp.cmk.cmkerp.stocks.commandes.application.service;

import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.request.ApprovisionnementRequest;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.request.LigneApprovRequest;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response.ApprovisionnementResponse;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.service.ApprovisionnementCommandService;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.service.LigneApprovCommandService;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.model.Approvisionnement;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.repository.ApprovisionnementRepository;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.request.*;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.response.DashboardCountsResponse;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.response.DemandeCotationResponse;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.response.EnvoiCotationResponse;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model.*;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.repository.CommandesRepository;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.util.Sha256Hasher;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CommandesApplicationService {

  private final CommandesRepository repo;
  private final MoneyConversionService moneyConversionService;
  private final ApprovisionnementCommandService approvisionnementCommandService;
  private final LigneApprovCommandService ligneApprovCommandService;
  private final ApprovisionnementRepository approvisionnementRepository;

  public DashboardCountsResponse dashboard() {
    return DashboardCountsResponse.from(repo.dashboardCounts());
  }

  public PageResponse<DemandeCotationResponse> listCotations(int page, int size, String statut, Long fkPharmacie, String search) {
    int offset = Math.max(page, 0) * Math.max(size, 1);
    List<DemandeCotation> list = repo.findDemandes(offset, size, statut, fkPharmacie, search);
    long total = repo.countDemandes(statut, fkPharmacie, search);
    List<DemandeCotationResponse> content = list.stream().map(d -> toDemandeResponse(d, false)).toList();
    return PageResponse.of(content, page, size, total);
  }

  public DemandeCotationResponse getCotation(Long id) {
    DemandeCotation d = repo.findDemandeById(id).orElseThrow(() -> NotFoundException.entity("DemandeCotation", id));
    return toDemandeResponse(d, true);
  }

  public DemandeCotationResponse createCotation(DemandeCotationRequest req, Long userId) {
    DemandeCotation d = DemandeCotation.builder()
        .numero("TMP")
        .objet(req.getObjet())
        .description(req.getDescription())
        .fkPharmacieDemandeur(req.getFkPharmacieDemandeur())
        .dateLimiteReponse(toEndOfDay(req.getDateLimiteReponse()))
        .dateLivraisonSouhaitee(req.getDateLivraisonSouhaitee())
        .lieuLivraison(req.getLieuLivraison())
        .conditions(req.getConditions())
        .statut("BROUILLON")
        .dateCreate(LocalDateTime.now())
        .userCreatedId(userId)
        .build();
    repo.saveDemande(d);
    d.setNumero(String.format("DC-%d-%d", Year.now().getValue(), d.getId()));
    repo.updateDemande(d);

    int ordre = 0;
    for (DemandeCotationRequest.LigneDemandeRequest lr : req.getLignes()) {
      repo.saveLigneDemande(LigneDemandeCotation.builder()
          .fkDemandeCotation(d.getId())
          .fkProduit(lr.getFkProduit())
          .fkCategorie(lr.getFkCategorie())
          .quantite(lr.getQuantite())
          .specifications(lr.getSpecifications())
          .ordre(lr.getOrdre() != null ? lr.getOrdre() : ordre++)
          .userCreatedId(userId)
          .build());
    }
    for (Long fid : req.getFournisseurIds()) {
      createInvitation(d, fid, userId);
    }
    return getCotation(d.getId());
  }

  public DemandeCotationResponse updateCotation(Long id, DemandeCotationRequest req, Long userId) {
    DemandeCotation d = repo.findDemandeById(id).orElseThrow(() -> NotFoundException.entity("DemandeCotation", id));
    if (!"BROUILLON".equals(d.getStatut())) {
      throw new BusinessException("Seule une cotation BROUILLON peut être modifiée");
    }
    d.setObjet(req.getObjet());
    d.setDescription(req.getDescription());
    d.setDateLimiteReponse(toEndOfDay(req.getDateLimiteReponse()));
    d.setDateLivraisonSouhaitee(req.getDateLivraisonSouhaitee());
    d.setLieuLivraison(req.getLieuLivraison());
    d.setConditions(req.getConditions());
    d.setDateUpdate(LocalDateTime.now());
    d.setUserUpdatedId(userId);
    repo.updateDemande(d);

    repo.deleteLignesDemande(id);
    int ordre = 0;
    for (DemandeCotationRequest.LigneDemandeRequest lr : req.getLignes()) {
      repo.saveLigneDemande(LigneDemandeCotation.builder()
          .fkDemandeCotation(id)
          .fkProduit(lr.getFkProduit())
          .fkCategorie(lr.getFkCategorie())
          .quantite(lr.getQuantite())
          .specifications(lr.getSpecifications())
          .ordre(lr.getOrdre() != null ? lr.getOrdre() : ordre++)
          .userCreatedId(userId)
          .build());
    }
    // sync invitations: create missing
    Set<Long> existing = repo.findInvitationsByDemande(id).stream()
        .map(InvitationFournisseur::getFkFournisseur).collect(Collectors.toSet());
    for (Long fid : req.getFournisseurIds()) {
      if (!existing.contains(fid)) {
        createInvitation(d, fid, userId);
      }
    }
    return getCotation(id);
  }

  public DemandeCotationResponse soumettreApprobationInterne(Long id, Long userId) {
    DemandeCotation d = repo.findDemandeById(id).orElseThrow(() -> NotFoundException.entity("DemandeCotation", id));
    StatutTransitionRules.assertDemandeTransition(d.getStatut(), "EN_VALIDATION_INTERNE");
    assertCotationPretPourEnvoi(id);
    d.setStatut("EN_VALIDATION_INTERNE");
    d.setDateUpdate(LocalDateTime.now());
    d.setUserUpdatedId(userId);
    repo.updateDemande(d);
    enqueueNotificationApprobationInterne(d);
    return getCotation(id);
  }

  public DemandeCotationResponse approuverCotation(Long id, Long userId) {
    DemandeCotation d = repo.findDemandeById(id).orElseThrow(() -> NotFoundException.entity("DemandeCotation", id));
    StatutTransitionRules.assertDemandeTransition(d.getStatut(), "APPROUVEE");
    assertCotationPretPourEnvoi(id);
    d.setStatut("APPROUVEE");
    d.setDateUpdate(LocalDateTime.now());
    d.setUserUpdatedId(userId);
    repo.updateDemande(d);
    return getCotation(id);
  }

  public DemandeCotationResponse retourBrouillonCotation(Long id, Long userId) {
    DemandeCotation d = repo.findDemandeById(id).orElseThrow(() -> NotFoundException.entity("DemandeCotation", id));
    StatutTransitionRules.assertDemandeTransition(d.getStatut(), "BROUILLON");
    d.setStatut("BROUILLON");
    d.setDateUpdate(LocalDateTime.now());
    d.setUserUpdatedId(userId);
    repo.updateDemande(d);
    return getCotation(id);
  }

  public EnvoiCotationResponse envoyerCotation(Long id, Long userId) {
    DemandeCotation d = repo.findDemandeById(id).orElseThrow(() -> NotFoundException.entity("DemandeCotation", id));
    if (!"APPROUVEE".equals(d.getStatut())) {
      throw new BusinessException("La cotation doit être APPROUVEE avant envoi aux fournisseurs");
    }
    StatutTransitionRules.assertDemandeTransition(d.getStatut(), "ENVOYEE");
    List<InvitationFournisseur> invitations = repo.findInvitationsByDemande(id);
    if (invitations.isEmpty()) {
      throw new BusinessException("Aucun fournisseur invité");
    }
    if (repo.findLignesDemande(id).isEmpty()) {
      throw new BusinessException("La cotation n'a aucune ligne");
    }
    String publicBase = resolvePublicBaseUrl();
    List<EnvoiCotationResponse.AccesTemporaireFournisseur> acces = new ArrayList<>();
    for (InvitationFournisseur inv : invitations) {
      if ("REVOQUEE".equals(inv.getStatut()) || "SOUMISE".equals(inv.getStatut())) {
        continue;
      }
      String email = repo.findFournisseurEmail(inv.getFkFournisseur());
      // Mot de passe temporaire UNIQUE par fournisseur / invitation
      String motDePasse = Sha256Hasher.generateAccessCode();
      inv.setAccessCodeHash(Sha256Hasher.sha256Hex(motDePasse));
      inv.setExpiresAt(d.getDateLimiteReponse());
      inv.setStatut("ENVOYEE");
      inv.setUserUpdatedId(userId);
      inv.setUnlockAttempts(0);
      inv.setUnlockLockedUntil(null);
      inv.setSessionTokenHash(null);
      inv.setSessionExpiresAt(null);
      repo.updateInvitation(inv);

      String lien = publicBase + "/portail-fournisseur/" + inv.getPublicToken();
      String sujet = "Invitation cotation " + d.getNumero() + " — " + d.getObjet();
      String corps = buildInvitationMailBody(d, inv, lien, motDePasse);
      String idem = "INVITE-" + inv.getId() + "-" + (System.currentTimeMillis() / 60000);
      if (email != null && !email.isBlank()) {
        repo.insertMailLog(idem, email, sujet, corps, inv.getId(), d.getId());
      } else {
        log.warn("Invitation {} sans e-mail fournisseur — mail non créé", inv.getId());
      }
      repo.insertOutbox("CMD_INVITATION_ENVOYEE", "cmkerp-commandes", String.valueOf(inv.getId()),
          "{\"invitationId\":" + inv.getId() + ",\"demandeId\":" + d.getId() + "}");
      if (repo.findOffreByInvitation(inv.getId()).isEmpty()) {
        repo.saveOffre(OffreFournisseur.builder()
            .fkInvitation(inv.getId())
            .fkDemandeCotation(d.getId())
            .fkFournisseur(inv.getFkFournisseur())
            .statut("BROUILLON")
            .versionNo(1)
            .build());
      }
      acces.add(EnvoiCotationResponse.AccesTemporaireFournisseur.builder()
          .invitationId(inv.getId())
          .fkFournisseur(inv.getFkFournisseur())
          .fournisseurNom(repo.findFournisseurNom(inv.getFkFournisseur()))
          .fournisseurEmail(email)
          .publicToken(inv.getPublicToken())
          .lienPortail(lien)
          .motDePasseTemporaire(motDePasse)
          .build());
    }
    d.setStatut("ENVOYEE");
    d.setDateUpdate(LocalDateTime.now());
    d.setUserUpdatedId(userId);
    repo.updateDemande(d);
    return EnvoiCotationResponse.builder()
        .cotation(getCotation(id))
        .accesTemporaires(acces)
        .build();
  }

  public DemandeCotationResponse annulerCotation(Long id, String motif, Long userId) {
    DemandeCotation d = repo.findDemandeById(id).orElseThrow(() -> NotFoundException.entity("DemandeCotation", id));
    StatutTransitionRules.assertDemandeTransition(d.getStatut(), "ANNULEE");
    d.setStatut("ANNULEE");
    d.setDateUpdate(LocalDateTime.now());
    d.setUserUpdatedId(userId);
    if (motif != null) {
      d.setConditions((d.getConditions() != null ? d.getConditions() + "\n" : "") + "Annulation: " + motif);
    }
    repo.updateDemande(d);
    return getCotation(id);
  }

  public void relancerInvitation(Long cotationId, Long invitationId, Long userId) {
    InvitationFournisseur inv = repo.findInvitationById(invitationId)
        .orElseThrow(() -> NotFoundException.entity("Invitation", invitationId));
    if (!Objects.equals(inv.getFkDemandeCotation(), cotationId)) {
      throw new BusinessException("Invitation hors cotation");
    }
    DemandeCotation d = repo.findDemandeById(cotationId)
        .orElseThrow(() -> NotFoundException.entity("DemandeCotation", cotationId));
    String accessCode = Sha256Hasher.generateAccessCode();
    inv.setAccessCodeHash(Sha256Hasher.sha256Hex(accessCode));
    inv.setRelances((inv.getRelances() != null ? inv.getRelances() : 0) + 1);
    inv.setUserUpdatedId(userId);
    repo.updateInvitation(inv);
    String email = repo.findFournisseurEmail(inv.getFkFournisseur());
    if (email != null && !email.isBlank()) {
      String lien = resolvePublicBaseUrl() + "/portail-fournisseur/" + inv.getPublicToken();
      repo.insertMailLog(
          "RELANCER-" + inv.getId() + "-" + inv.getRelances(),
          email,
          "Relance cotation " + d.getNumero(),
          buildInvitationMailBody(d, inv, lien, accessCode),
          inv.getId(),
          cotationId);
    }
  }

  /**
   * Régénère le mot de passe temporaire d'une invitation (clair renvoyé une fois).
   * Autorisé uniquement si la cotation est APPROUVEE ou ENVOYEE — pas de mail.
   */
  public EnvoiCotationResponse.AccesTemporaireFournisseur regenererAccesInvitation(
      Long cotationId, Long invitationId, Long userId) {
    InvitationFournisseur inv = repo.findInvitationById(invitationId)
        .orElseThrow(() -> NotFoundException.entity("Invitation", invitationId));
    if (!Objects.equals(inv.getFkDemandeCotation(), cotationId)) {
      throw new BusinessException("Invitation hors cotation");
    }
    DemandeCotation d = repo.findDemandeById(cotationId)
        .orElseThrow(() -> NotFoundException.entity("DemandeCotation", cotationId));
    if (!"APPROUVEE".equals(d.getStatut()) && !"ENVOYEE".equals(d.getStatut())) {
      throw new BusinessException(
          "Régénération d'accès autorisée uniquement si APPROUVEE ou ENVOYEE (statut actuel : "
              + d.getStatut() + ")");
    }
    if ("REVOQUEE".equals(inv.getStatut())) {
      throw new BusinessException("Invitation révoquée");
    }
    String motDePasse = Sha256Hasher.generateAccessCode();
    inv.setAccessCodeHash(Sha256Hasher.sha256Hex(motDePasse));
    inv.setUnlockAttempts(0);
    inv.setUnlockLockedUntil(null);
    inv.setSessionTokenHash(null);
    inv.setSessionExpiresAt(null);
    inv.setUserUpdatedId(userId);
    repo.updateInvitation(inv);
    String email = repo.findFournisseurEmail(inv.getFkFournisseur());
    String lien = resolvePublicBaseUrl() + "/portail-fournisseur/" + inv.getPublicToken();
    return EnvoiCotationResponse.AccesTemporaireFournisseur.builder()
        .invitationId(inv.getId())
        .fkFournisseur(inv.getFkFournisseur())
        .fournisseurNom(repo.findFournisseurNom(inv.getFkFournisseur()))
        .fournisseurEmail(email)
        .publicToken(inv.getPublicToken())
        .lienPortail(lien)
        .motDePasseTemporaire(motDePasse)
        .build();
  }

  private void assertCotationPretPourEnvoi(Long id) {
    if (repo.findInvitationsByDemande(id).isEmpty()) {
      throw new BusinessException("Aucun fournisseur invité");
    }
    if (repo.findLignesDemande(id).isEmpty()) {
      throw new BusinessException("La cotation n'a aucune ligne");
    }
  }

  /** Notifie les destinataires mailingsend qu'une cotation attend une approbation interne. */
  private void enqueueNotificationApprobationInterne(DemandeCotation d) {
    List<String> destinataires = repo.listActiveMailingSendEmails();
    if (destinataires.isEmpty()) {
      log.warn("Cotation {} soumise en validation — aucun destinataire mailingsend actif", d.getNumero());
      return;
    }
    String sujet = "Cotation " + d.getNumero() + " à approuver — " + d.getObjet();
    String corps = "Bonjour,\n\n"
        + "La demande de cotation " + d.getNumero() + " (« " + d.getObjet() + " ») "
        + "a été soumise pour approbation interne.\n\n"
        + "Merci de l'examiner et de l'approuver dans la console commandes avant envoi aux fournisseurs.\n\n"
        + "— CMK ERP Commandes\n";
    String to = destinataires.get(0);
    String idem = "APPROB-INT-" + d.getId() + "-" + (System.currentTimeMillis() / 60000);
    repo.insertMailLog(idem, to, sujet, corps, null, d.getId());
  }

  private InvitationFournisseur createInvitation(DemandeCotation d, Long fournisseurId, Long userId) {
    String publicToken = Sha256Hasher.generatePublicToken();
    String accessCode = Sha256Hasher.generateAccessCode();
    InvitationFournisseur inv = InvitationFournisseur.builder()
        .fkDemandeCotation(d.getId())
        .fkFournisseur(fournisseurId)
        .publicToken(publicToken)
        .accessCodeHash(Sha256Hasher.sha256Hex(accessCode))
        .statut("CREEE")
        .expiresAt(d.getDateLimiteReponse())
        .relances(0)
        .unlockAttempts(0)
        .userCreatedId(userId)
        .build();
    repo.saveInvitation(inv);
    log.info("Invitation creee id={} token={} (brouillon — mail a l'envoi)", inv.getId(), publicToken);
    return inv;
  }

  private static LocalDateTime toEndOfDay(LocalDate date) {
    return date == null ? null : date.atTime(23, 59, 59);
  }

  private String resolvePublicBaseUrl() {
    String configured = System.getenv("CMKERP_PUBLIC_APP_URL");
    if (configured != null && !configured.isBlank()) {
      return configured.replaceAll("/$", "");
    }
    return "http://localhost:3940";
  }

  private String buildInvitationMailBody(
      DemandeCotation d, InvitationFournisseur inv, String lien, String accessCode) {
    String fournisseur = repo.findFournisseurNom(inv.getFkFournisseur());
    String limite = d.getDateLimiteReponse() != null ? d.getDateLimiteReponse().toLocalDate().toString() : "-";
    return "Bonjour" + (fournisseur != null ? " " + fournisseur : "") + ",\n\n"
        + "Vous etes invite a repondre a la demande de cotation " + d.getNumero() + ".\n"
        + "Objet : " + d.getObjet() + "\n"
        + "Date limite de reponse : " + limite + "\n\n"
        + "Lien portail (personnel) :\n" + lien + "\n\n"
        + "Mot de passe temporaire (personnel) : " + accessCode + "\n\n"
        + "Ce lien et ce mot de passe sont strictement personnels a votre entreprise. "
        + "Ils permettent d'ouvrir uniquement votre formulaire de saisie.\n\n"
        + "Cordialement,\nService Approvisionnement CMK\n";
  }

  public Map<String, Object> getComparatif(Long demandeId) {
    DemandeCotation d = repo.findDemandeById(demandeId).orElseThrow(() -> NotFoundException.entity("DemandeCotation", demandeId));
    List<LigneDemandeCotation> lignes = repo.findLignesDemande(demandeId);
    List<InvitationFournisseur> invitations = repo.findInvitationsByDemande(demandeId);
    List<Map<String, Object>> lignesOut = new ArrayList<>();
    for (LigneDemandeCotation l : lignes) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", l.getId());
      m.put("fkProduit", l.getFkProduit());
      m.put("produitNom", repo.findProduitNom(l.getFkProduit()));
      m.put("quantite", l.getQuantite());
      m.put("categorieNom", null);
      lignesOut.add(m);
    }
    List<Map<String, Object>> fournisseurs = new ArrayList<>();
    List<Map<String, Object>> cellules = new ArrayList<>();
    Map<Long, BigDecimal> minPrix = new HashMap<>();
    Map<Long, Integer> minDelai = new HashMap<>();

    for (InvitationFournisseur inv : invitations) {
      Map<String, Object> f = new LinkedHashMap<>();
      f.put("id", inv.getFkFournisseur());
      f.put("nom", repo.findFournisseurNom(inv.getFkFournisseur()));
      fournisseurs.add(f);
      Optional<OffreFournisseur> offreOpt = repo.findOffreByInvitation(inv.getId());
      if (offreOpt.isEmpty() || !"SOUMISE".equals(offreOpt.get().getStatut()) && !"REOUVERTE".equals(offreOpt.get().getStatut())) {
        // still include soumises primarily
      }
      if (offreOpt.isPresent()) {
        OffreFournisseur offre = offreOpt.get();
        for (LigneOffreFournisseur lo : repo.findLignesOffre(offre.getId())) {
          Map<String, Object> cell = new LinkedHashMap<>();
          cell.put("fkLigneDemande", lo.getFkLigneDemande());
          cell.put("fkFournisseur", inv.getFkFournisseur());
          cell.put("fournisseurNom", repo.findFournisseurNom(inv.getFkFournisseur()));
          cell.put("offreId", offre.getId());
          cell.put("prixUsd", lo.getPrixUsd());
          cell.put("delaiJours", lo.getDelaiJours());
          cell.put("quantiteDisponible", lo.getQuantiteDisponible());
          cell.put("estPlusBasPrix", false);
          cell.put("estPlusCourtDelai", false);
          cellules.add(cell);
          if (lo.getPrixUsd() != null) {
            minPrix.merge(lo.getFkLigneDemande(), lo.getPrixUsd(), BigDecimal::min);
          }
          if (lo.getDelaiJours() != null) {
            minDelai.merge(lo.getFkLigneDemande(), lo.getDelaiJours(), Math::min);
          }
        }
      }
    }
    for (Map<String, Object> cell : cellules) {
      Long lid = (Long) cell.get("fkLigneDemande");
      BigDecimal prix = (BigDecimal) cell.get("prixUsd");
      Integer delai = (Integer) cell.get("delaiJours");
      if (prix != null && prix.equals(minPrix.get(lid))) {
        cell.put("estPlusBasPrix", true);
      }
      if (delai != null && delai.equals(minDelai.get(lid))) {
        cell.put("estPlusCourtDelai", true);
      }
    }
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("demandeId", d.getId());
    out.put("lignes", lignesOut);
    out.put("fournisseurs", fournisseurs);
    out.put("cellules", cellules);
    return out;
  }

  public Map<String, Object> attribuer(Long demandeId, AttributionRequest req, Long userId) {
    DemandeCotation d = repo.findDemandeById(demandeId).orElseThrow(() -> NotFoundException.entity("DemandeCotation", demandeId));
    if ("ATTRIBUEE".equals(d.getStatut()) || "CLOTUREE".equals(d.getStatut()) || "ANNULEE".equals(d.getStatut())) {
      throw new BusinessException("Cotation non attribuable dans le statut " + d.getStatut());
    }
    AttributionCotation attr = AttributionCotation.builder()
        .fkDemandeCotation(demandeId)
        .scope(req.getScope())
        .justification(req.getJustification())
        .fkCategorie(req.getFkCategorie())
        .userCreatedId(userId)
        .build();
    repo.saveAttribution(attr);
    for (AttributionRequest.LigneAttributionRequest lr : req.getLignes()) {
      repo.saveLigneAttribution(LigneAttribution.builder()
          .fkAttribution(attr.getId())
          .fkLigneDemande(lr.getFkLigneDemande())
          .fkFournisseur(lr.getFkFournisseur())
          .quantiteAttribuee(lr.getQuantiteAttribuee())
          .motif(lr.getMotif())
          .build());
    }
    d.setStatut("ATTRIBUEE");
    d.setDateUpdate(LocalDateTime.now());
    d.setUserUpdatedId(userId);
    repo.updateDemande(d);

    List<Long> bonsIds = new ArrayList<>();
    if (req.getGenererBonsCommande() == null || Boolean.TRUE.equals(req.getGenererBonsCommande())) {
      bonsIds = genererBonsFromAttribution(d, attr, req.getLignes(), userId).stream().map(BonCommande::getId).toList();
    }
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("id", attr.getId());
    out.put("fkDemandeCotation", demandeId);
    out.put("scope", attr.getScope());
    out.put("justification", attr.getJustification());
    out.put("dateCreate", LocalDateTime.now());
    out.put("bonsCommandeIds", bonsIds);
    return out;
  }

  public List<Map<String, Object>> genererBonsCommande(Long demandeId, Long userId) {
    DemandeCotation d = repo.findDemandeById(demandeId).orElseThrow(() -> NotFoundException.entity("DemandeCotation", demandeId));
    if (!"ATTRIBUEE".equals(d.getStatut())) {
      throw new BusinessException("La cotation doit être ATTRIBUEE pour générer les bons");
    }
    // Les bons sont déjà générés lors de l'attribution si genererBonsCommande=true
    return repo.findBons(0, 200, null, null, null).stream()
        .filter(b -> Objects.equals(b.getFkDemandeCotation(), demandeId))
        .map(b -> toBonMap(b, true))
        .toList();
  }

  private List<BonCommande> genererBonsFromAttribution(DemandeCotation d, AttributionCotation attr,
      List<AttributionRequest.LigneAttributionRequest> lignes, Long userId) {
    Map<Long, List<AttributionRequest.LigneAttributionRequest>> byFourn =
        lignes.stream().collect(Collectors.groupingBy(AttributionRequest.LigneAttributionRequest::getFkFournisseur));
    List<BonCommande> bons = new ArrayList<>();
    Map<Long, LigneDemandeCotation> ligneDemandeMap = repo.findLignesDemande(d.getId()).stream()
        .collect(Collectors.toMap(LigneDemandeCotation::getId, x -> x));

    for (Map.Entry<Long, List<AttributionRequest.LigneAttributionRequest>> e : byFourn.entrySet()) {
      Long fkFournisseur = e.getKey();
      BigDecimal totalUsd = BigDecimal.ZERO;
      BonCommande bon = BonCommande.builder()
          .numero("TMP")
          .fkDemandeCotation(d.getId())
          .fkAttribution(attr.getId())
          .fkFournisseur(fkFournisseur)
          .fkPharmacie(d.getFkPharmacieDemandeur())
          .statut("BROUILLON")
          .dateCommande(LocalDate.now())
          .dateLivraisonPrevue(d.getDateLivraisonSouhaitee())
          .userCreatedId(userId)
          .build();
      repo.saveBon(bon);
      bon.setNumero(String.format("BC-%d-%d", Year.now().getValue(), bon.getId()));
      repo.updateBon(bon);

      for (AttributionRequest.LigneAttributionRequest lr : e.getValue()) {
        LigneDemandeCotation ld = ligneDemandeMap.get(lr.getFkLigneDemande());
        if (ld == null) {
          throw new BusinessException("Ligne demande introuvable: " + lr.getFkLigneDemande());
        }
        BigDecimal prixUsd = resolvePrixUsdFromOffre(d.getId(), fkFournisseur, lr.getFkLigneDemande());
        BigDecimal montant = prixUsd.multiply(lr.getQuantiteAttribuee()).setScale(4, RoundingMode.HALF_UP);
        totalUsd = totalUsd.add(montant);
        repo.saveLigneBon(LigneBonCommande.builder()
            .fkBonCommande(bon.getId())
            .fkLigneDemande(lr.getFkLigneDemande())
            .fkProduit(ld.getFkProduit())
            .quantiteCommandee(lr.getQuantiteAttribuee())
            .quantiteRecue(BigDecimal.ZERO)
            .prixUnitaireUsd(prixUsd)
            .montantLigneUsd(montant)
            .build());
      }
      bon.setMontantTotalUsd(totalUsd);
      repo.updateBon(bon);
      bons.add(bon);
    }
    return bons;
  }

  private BigDecimal resolvePrixUsdFromOffre(Long demandeId, Long fkFournisseur, Long fkLigneDemande) {
    for (InvitationFournisseur inv : repo.findInvitationsByDemande(demandeId)) {
      if (!Objects.equals(inv.getFkFournisseur(), fkFournisseur)) continue;
      Optional<OffreFournisseur> offre = repo.findOffreByInvitation(inv.getId());
      if (offre.isEmpty()) continue;
      for (LigneOffreFournisseur lo : repo.findLignesOffre(offre.get().getId())) {
        if (Objects.equals(lo.getFkLigneDemande(), fkLigneDemande) && lo.getPrixUsd() != null) {
          return lo.getPrixUsd();
        }
      }
    }
    return BigDecimal.ZERO;
  }

  public PageResponse<Map<String, Object>> listBons(int page, int size, String statut, Long fkFournisseur, String search) {
    int offset = Math.max(page, 0) * Math.max(size, 1);
    List<BonCommande> list = repo.findBons(offset, size, statut, fkFournisseur, search);
    long total = repo.countBons(statut, fkFournisseur, search);
    return PageResponse.of(list.stream().map(b -> toBonMap(b, false)).toList(), page, size, total);
  }

  public Map<String, Object> getBon(Long id) {
    BonCommande b = repo.findBonById(id).orElseThrow(() -> NotFoundException.entity("BonCommande", id));
    return toBonMap(b, true);
  }

  public Map<String, Object> transitionBon(Long id, String action, Long userId) {
    BonCommande b = repo.findBonById(id).orElseThrow(() -> NotFoundException.entity("BonCommande", id));
    String target = StatutTransitionRules.targetStatutForAction(b.getStatut(), action);
    StatutTransitionRules.assertBonTransition(b.getStatut(), target);
    b.setStatut(target);
    b.setUserUpdatedId(userId);
    b.setDateUpdate(LocalDateTime.now());
    repo.updateBon(b);
    return toBonMap(b, true);
  }

  public Map<String, Object> createReception(ReceptionCommandeRequest req, Long userId) {
    BonCommande bon = repo.findBonById(req.getFkBonCommande())
        .orElseThrow(() -> NotFoundException.entity("BonCommande", req.getFkBonCommande()));
    if ("ANNULE".equals(bon.getStatut()) || "CLOTURE".equals(bon.getStatut())) {
      throw new BusinessException("Bon non réceptionnable");
    }
    ReceptionCommande reception = ReceptionCommande.builder()
        .fkBonCommande(bon.getId())
        .numero("TMP")
        .statut("BROUILLON")
        .dateReception(req.getDateReception() != null ? req.getDateReception() : LocalDate.now())
        .commentaire(req.getCommentaire())
        .userCreatedId(userId)
        .build();
    repo.saveReception(reception);
    reception.setNumero(String.format("RC-%d-%d", Year.now().getValue(), reception.getId()));
    repo.updateReception(reception);

    Map<Long, LigneBonCommande> lignesBon = repo.findLignesBon(bon.getId()).stream()
        .collect(Collectors.toMap(LigneBonCommande::getId, x -> x));

    for (ReceptionCommandeRequest.LigneReceptionRequest lr : req.getLignes()) {
      LigneBonCommande lb = lignesBon.get(lr.getFkLigneBonCommande());
      if (lb == null) {
        throw new BusinessException("Ligne BC introuvable: " + lr.getFkLigneBonCommande());
      }
      BigDecimal reste = MoneyConversionService.calculerReliquat(lb.getQuantiteCommandee(), lb.getQuantiteRecue());
      if (lr.getQuantiteRecue().compareTo(reste) > 0) {
        throw new BusinessException("Quantité reçue dépasse le reliquat pour ligne " + lb.getId());
      }
      repo.saveLigneReception(LigneReceptionCommande.builder()
          .fkReception(reception.getId())
          .fkLigneBonCommande(lr.getFkLigneBonCommande())
          .quantiteRecue(lr.getQuantiteRecue())
          .lot(lr.getLot())
          .datePeremption(lr.getDatePeremption())
          .build());
      lb.setQuantiteRecue(lb.getQuantiteRecue().add(lr.getQuantiteRecue()));
      repo.updateLigneBon(lb);
    }

    // Créer Approvisionnement Mode A + lignes + valider
    ApprovisionnementRequest approvReq = new ApprovisionnementRequest();
    approvReq.setFkFournisseur(bon.getFkFournisseur());
    approvReq.setFkPharmacie(bon.getFkPharmacie());
    approvReq.setFkEchangeDevise(bon.getFkEchangeDevise());
    approvReq.setNumbonliv("BC-" + bon.getNumero() + "-R" + reception.getId());
    approvReq.setDatebonliv(reception.getDateReception());
    ApprovisionnementResponse approvResp = approvisionnementCommandService.create(approvReq, userId);
    Long approvId = approvResp.getId();

    for (ReceptionCommandeRequest.LigneReceptionRequest lr : req.getLignes()) {
      LigneBonCommande lb = lignesBon.get(lr.getFkLigneBonCommande());
      Long fkStock = repo.findStockId(lb.getFkProduit(), bon.getFkPharmacie());
      if (fkStock == null) {
        throw new BusinessException("Stock introuvable pour produit " + lb.getFkProduit() + " / pharmacie " + bon.getFkPharmacie());
      }
      LigneApprovRequest ligneReq = new LigneApprovRequest();
      ligneReq.setFkApprov(approvId);
      ligneReq.setFkStock(fkStock);
      ligneReq.setQt(lr.getQuantiteRecue().floatValue());
      ligneReq.setPrixachat(lb.getPrixUnitaireUsd());
      ligneApprovCommandService.create(ligneReq, userId);
    }

    // set FKs on approvisionnement
    Approvisionnement approv = approvisionnementRepository.findById(approvId)
        .orElseThrow(() -> NotFoundException.entity("Approvisionnement", approvId));
    approv.setFkBonCommande(bon.getId());
    approv.setFkReceptionCommande(reception.getId());
    approvisionnementRepository.update(approv);

    boolean valider = req.getValiderImmediatement() == null || Boolean.TRUE.equals(req.getValiderImmediatement());
    if (valider) {
      approvisionnementCommandService.valider(approvId, userId);
      reception.setStatut("VALIDEE");
    }
    reception.setFkApprovisionnement(approvId);
    reception.setUserUpdatedId(userId);
    repo.updateReception(reception);

    // update BC statut
    List<LigneBonCommande> allLignes = repo.findLignesBon(bon.getId());
    boolean allDone = allLignes.stream().allMatch(l ->
        MoneyConversionService.calculerReliquat(l.getQuantiteCommandee(), l.getQuantiteRecue()).compareTo(BigDecimal.ZERO) == 0);
    boolean anyReceived = allLignes.stream().anyMatch(l ->
        l.getQuantiteRecue() != null && l.getQuantiteRecue().compareTo(BigDecimal.ZERO) > 0);
    if (allDone) {
      bon.setStatut("TOTALEMENT_LIVRE");
    } else if (anyReceived) {
      bon.setStatut("PARTIELLEMENT_LIVRE");
    }
    bon.setUserUpdatedId(userId);
    repo.updateBon(bon);

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("id", reception.getId());
    out.put("fkBonCommande", bon.getId());
    out.put("numero", reception.getNumero());
    out.put("statut", reception.getStatut());
    out.put("dateReception", reception.getDateReception());
    out.put("fkApprovisionnement", approvId);
    out.put("commentaire", reception.getCommentaire());
    return out;
  }

  public List<Map<String, Object>> listReceptions(Long bonId) {
    return repo.findReceptionsByBon(bonId).stream().map(r -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", r.getId());
      m.put("fkBonCommande", r.getFkBonCommande());
      m.put("numero", r.getNumero());
      m.put("statut", r.getStatut());
      m.put("dateReception", r.getDateReception());
      m.put("fkApprovisionnement", r.getFkApprovisionnement());
      m.put("commentaire", r.getCommentaire());
      return m;
    }).toList();
  }

  public PageResponse<Map<String, Object>> listReliquats(int page, int size) {
    // simple: scan bons partiels
    List<BonCommande> bons = repo.findBons(0, 500, null, null, null);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (BonCommande b : bons) {
      for (LigneBonCommande l : repo.findLignesBon(b.getId())) {
        BigDecimal reste = l.quantiteReste();
        if (reste.compareTo(BigDecimal.ZERO) > 0) {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("fkBonCommande", b.getId());
          m.put("bonNumero", b.getNumero());
          m.put("fkFournisseur", b.getFkFournisseur());
          m.put("fournisseurNom", repo.findFournisseurNom(b.getFkFournisseur()));
          m.put("fkLigneBonCommande", l.getId());
          m.put("produitNom", repo.findProduitNom(l.getFkProduit()));
          m.put("quantiteCommandee", l.getQuantiteCommandee());
          m.put("quantiteRecue", l.getQuantiteRecue());
          m.put("quantiteReste", reste);
          m.put("statutBon", b.getStatut());
          rows.add(m);
        }
      }
    }
    int from = Math.min(page * size, rows.size());
    int to = Math.min(from + size, rows.size());
    return PageResponse.of(rows.subList(from, to), page, size, rows.size());
  }

  public PageResponse<Map<String, Object>> listOffres(int page, int size, Long fkDemande, String statut) {
    int offset = Math.max(page, 0) * Math.max(size, 1);
    List<OffreFournisseur> list = repo.findOffres(offset, size, fkDemande, statut);
    long total = repo.countOffres(fkDemande, statut);
    return PageResponse.of(list.stream().map(o -> toOffreMap(o, true)).toList(), page, size, total);
  }

  public Map<String, Object> getOffre(Long id) {
    OffreFournisseur o = repo.findOffreById(id).orElseThrow(() -> NotFoundException.entity("Offre", id));
    return toOffreMap(o, true);
  }

  public List<Map<String, Object>> listReouvertures(String statut, int limit) {
    return repo.listReouvertures(statut, limit);
  }

  public void decideReouverture(Long offreId, ReouvertureDecisionRequest req, Long userId) {
    OffreFournisseur offre = repo.findOffreById(offreId).orElseThrow(() -> NotFoundException.entity("Offre", offreId));
    Map<String, Object> pending = repo.findDemandeReouverturePending(offreId)
        .orElseThrow(() -> new BusinessException("Aucune demande de réouverture en attente"));
    Long demId = ((Number) pending.get("id")).longValue();
    String decision = req.getDecision();
    repo.updateDemandeReouverture(demId, decision, req.getNouvelleDateLimite(), req.getCommentaire(), userId);
    if ("APPROUVEE".equals(decision) || "APPROUVEE_AVEC_DELAI".equals(decision)) {
      offre.setStatut("REOUVERTE");
      offre.setLockedAt(null);
      repo.updateOffre(offre);
      InvitationFournisseur inv = repo.findInvitationById(offre.getFkInvitation()).orElseThrow();
      inv.setStatut("REOUVERTE");
      if (req.getNouvelleDateLimite() != null) {
        inv.setExpiresAt(LocalDateTime.parse(req.getNouvelleDateLimite().contains("T")
            ? req.getNouvelleDateLimite() : req.getNouvelleDateLimite() + "T23:59:59"));
      }
      repo.updateInvitation(inv);
    }
  }

  public Map<String, Object> createEvaluation(EvaluationRequest req, Long userId) {
    ParamScoreFournisseur param = repo.findParamScore().orElse(ParamScoreFournisseur.builder()
        .poidsDelais(BigDecimal.valueOf(30)).poidsQualite(BigDecimal.valueOf(25))
        .poidsPrix(BigDecimal.valueOf(20)).poidsCompletude(BigDecimal.valueOf(15))
        .poidsReactivite(BigDecimal.valueOf(10)).build());
    BigDecimal score = req.getNoteDelais().multiply(param.getPoidsDelais())
        .add(req.getNoteQualite().multiply(param.getPoidsQualite()))
        .add(req.getNotePrix().multiply(param.getPoidsPrix()))
        .add(req.getNoteCompletude().multiply(param.getPoidsCompletude()))
        .add(req.getNoteReactivite().multiply(param.getPoidsReactivite()))
        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    EvaluationFournisseur e = EvaluationFournisseur.builder()
        .fkFournisseur(req.getFkFournisseur())
        .fkBonCommande(req.getFkBonCommande())
        .fkReception(req.getFkReception())
        .noteDelais(req.getNoteDelais())
        .noteQualite(req.getNoteQualite())
        .notePrix(req.getNotePrix())
        .noteCompletude(req.getNoteCompletude())
        .noteReactivite(req.getNoteReactivite())
        .scoreGlobal(score)
        .commentaire(req.getCommentaire())
        .userCreatedId(userId)
        .build();
    repo.saveEvaluation(e);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", e.getId());
    m.put("fkFournisseur", e.getFkFournisseur());
    m.put("fournisseurNom", repo.findFournisseurNom(e.getFkFournisseur()));
    m.put("fkBonCommande", e.getFkBonCommande());
    m.put("fkReception", e.getFkReception());
    m.put("noteDelais", e.getNoteDelais());
    m.put("noteQualite", e.getNoteQualite());
    m.put("notePrix", e.getNotePrix());
    m.put("noteCompletude", e.getNoteCompletude());
    m.put("noteReactivite", e.getNoteReactivite());
    m.put("scoreGlobal", e.getScoreGlobal());
    m.put("commentaire", e.getCommentaire());
    m.put("dateCreate", LocalDateTime.now());
    return m;
  }

  public PageResponse<Map<String, Object>> listEvaluations(int page, int size, Long fkFournisseur) {
    int offset = Math.max(page, 0) * Math.max(size, 1);
    List<EvaluationFournisseur> list = repo.findEvaluations(offset, size, fkFournisseur);
    long total = repo.countEvaluations(fkFournisseur);
    List<Map<String, Object>> content = list.stream().map(e -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", e.getId());
      m.put("fkFournisseur", e.getFkFournisseur());
      m.put("fournisseurNom", repo.findFournisseurNom(e.getFkFournisseur()));
      m.put("fkBonCommande", e.getFkBonCommande());
      m.put("fkReception", e.getFkReception());
      m.put("noteDelais", e.getNoteDelais());
      m.put("noteQualite", e.getNoteQualite());
      m.put("notePrix", e.getNotePrix());
      m.put("noteCompletude", e.getNoteCompletude());
      m.put("noteReactivite", e.getNoteReactivite());
      m.put("scoreGlobal", e.getScoreGlobal());
      m.put("commentaire", e.getCommentaire());
      m.put("dateCreate", e.getDateCreate());
      return m;
    }).toList();
    return PageResponse.of(content, page, size, total);
  }

  public ParamScoreFournisseur getParamScore() {
    return repo.findParamScore().orElseThrow(() -> new BusinessException("Paramètres score absents"));
  }

  public ParamScoreFournisseur updateParamScore(ParamScoreFournisseur p, Long userId) {
    ParamScoreFournisseur existing = getParamScore();
    existing.setPoidsDelais(p.getPoidsDelais());
    existing.setPoidsQualite(p.getPoidsQualite());
    existing.setPoidsPrix(p.getPoidsPrix());
    existing.setPoidsCompletude(p.getPoidsCompletude());
    existing.setPoidsReactivite(p.getPoidsReactivite());
    existing.setUserUpdatedId(userId);
    repo.updateParamScore(existing);
    return existing;
  }

  public PageResponse<Map<String, Object>> listModifs(int page, int size, String statut) {
    int offset = Math.max(page, 0) * Math.max(size, 1);
    List<DemandeModifFournisseur> list = repo.findModifs(offset, size, statut);
    long total = repo.countModifs(statut);
    return PageResponse.of(list.stream().map(this::toModifMap).toList(), page, size, total);
  }

  public Map<String, Object> decideModif(Long id, ModifDecisionRequest req, Long userId) {
    DemandeModifFournisseur d = repo.findModifById(id).orElseThrow(() -> NotFoundException.entity("DemandeModif", id));
    d.setStatut(req.getDecision());
    d.setCommentaireDecision(req.getCommentaire());
    d.setDecideurId(userId);
    repo.updateModif(d);
    if ("APPROUVEE".equals(req.getDecision()) || "PARTIELLE".equals(req.getDecision())) {
      applyFournisseurModifs(d, req.getChampsApprouves());
    }
    return toModifMap(d);
  }

  private void applyFournisseurModifs(DemandeModifFournisseur d, List<String> champsApprouves) {
    List<ChampModifFournisseur> champs = repo.findChampsModif(d.getId());
    for (ChampModifFournisseur c : champs) {
      if (champsApprouves != null && !champsApprouves.isEmpty() && !champsApprouves.contains(c.getChamp())) {
        continue;
      }
      String col = switch (c.getChamp()) {
        case "nom" -> "nom";
        case "email" -> "email";
        case "telephone" -> "telephone";
        case "adresse" -> "adresse";
        default -> null;
      };
      if (col != null) {
        // safe whitelist update
        repo.insertOutbox("CMD_FOURNISSEUR_MODIF", "cmkerp-commandes", String.valueOf(d.getFkFournisseur()),
            "{\"champ\":\"" + col + "\",\"valeur\":\"" + c.getValeurProposee().replace("\"", "'") + "\",\"fkFournisseur\":" + d.getFkFournisseur() + "}");
      }
    }
  }

  public Map<String, Object> getBonAsApprovDraft(Long id) {
    BonCommande b = repo.findBonById(id).orElseThrow(() -> NotFoundException.entity("BonCommande", id));
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("fkBonCommande", b.getId());
    m.put("numeroBonCommande", b.getNumero());
    m.put("fkFournisseur", b.getFkFournisseur());
    m.put("fournisseurNom", repo.findFournisseurNom(b.getFkFournisseur()));
    m.put("fkPharmacie", b.getFkPharmacie());
    m.put("pharmacieNom", repo.findPharmacieNom(b.getFkPharmacie()));
    m.put("fkEchangeDevise", b.getFkEchangeDevise());
    m.put("taux", null);
    List<Map<String, Object>> lignes = new ArrayList<>();
    for (LigneBonCommande l : repo.findLignesBon(id)) {
      Map<String, Object> lm = new LinkedHashMap<>();
      lm.put("fkProduit", l.getFkProduit());
      lm.put("produitNom", repo.findProduitNom(l.getFkProduit()));
      lm.put("quantite", l.quantiteReste());
      lm.put("prixUnitaireUsd", l.getPrixUnitaireUsd());
      lignes.add(lm);
    }
    m.put("lignes", lignes);
    return m;
  }

  // —— helpers ——

  private DemandeCotationResponse toDemandeResponse(DemandeCotation d, boolean details) {
    DemandeCotationResponse.DemandeCotationResponseBuilder b = DemandeCotationResponse.builder()
        .id(d.getId()).numero(d.getNumero()).objet(d.getObjet()).description(d.getDescription())
        .fkPharmacieDemandeur(d.getFkPharmacieDemandeur())
        .pharmacieNom(repo.findPharmacieNom(d.getFkPharmacieDemandeur()))
        .dateLimiteReponse(d.getDateLimiteReponse()).dateLivraisonSouhaitee(d.getDateLivraisonSouhaitee())
        .lieuLivraison(d.getLieuLivraison()).conditions(d.getConditions()).statut(d.getStatut())
        .dateCreate(d.getDateCreate()).dateUpdate(d.getDateUpdate());
    List<InvitationFournisseur> invs = repo.findInvitationsByDemande(d.getId());
    b.nbInvitations(invs.size());
    long soumises = repo.countOffres(d.getId(), "SOUMISE");
    b.nbOffresSoumises((int) soumises);
    if (details) {
      b.lignes(repo.findLignesDemande(d.getId()).stream().map(l -> DemandeCotationResponse.LigneDemandeResponse.builder()
          .id(l.getId()).fkProduit(l.getFkProduit()).produitNom(repo.findProduitNom(l.getFkProduit()))
          .fkCategorie(l.getFkCategorie()).quantite(l.getQuantite()).specifications(l.getSpecifications())
          .ordre(l.getOrdre()).build()).toList());
      b.invitations(invs.stream().map(i -> DemandeCotationResponse.InvitationResponse.builder()
          .id(i.getId()).fkDemandeCotation(i.getFkDemandeCotation()).fkFournisseur(i.getFkFournisseur())
          .fournisseurNom(repo.findFournisseurNom(i.getFkFournisseur()))
          .fournisseurEmail(repo.findFournisseurEmail(i.getFkFournisseur()))
          .statut(i.getStatut()).expiresAt(i.getExpiresAt()).openedAt(i.getOpenedAt())
          .submittedAt(i.getSubmittedAt()).publicToken(i.getPublicToken()).relances(i.getRelances())
          .build()).toList());
    }
    return b.build();
  }

  private Map<String, Object> toBonMap(BonCommande b, boolean details) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", b.getId());
    m.put("numero", b.getNumero());
    m.put("fkDemandeCotation", b.getFkDemandeCotation());
    m.put("fkFournisseur", b.getFkFournisseur());
    m.put("fournisseurNom", repo.findFournisseurNom(b.getFkFournisseur()));
    m.put("fkPharmacie", b.getFkPharmacie());
    m.put("pharmacieNom", repo.findPharmacieNom(b.getFkPharmacie()));
    m.put("statut", b.getStatut());
    m.put("montantTotalUsd", b.getMontantTotalUsd());
    m.put("dateCommande", b.getDateCommande());
    m.put("dateLivraisonPrevue", b.getDateLivraisonPrevue());
    m.put("enRetard", b.isEnRetard());
    m.put("dateCreate", b.getDateCreate());
    if (details) {
      List<Map<String, Object>> lignes = new ArrayList<>();
      for (LigneBonCommande l : repo.findLignesBon(b.getId())) {
        Map<String, Object> lm = new LinkedHashMap<>();
        lm.put("id", l.getId());
        lm.put("fkProduit", l.getFkProduit());
        lm.put("produitNom", repo.findProduitNom(l.getFkProduit()));
        lm.put("quantiteCommandee", l.getQuantiteCommandee());
        lm.put("quantiteRecue", l.getQuantiteRecue());
        lm.put("quantiteReste", l.quantiteReste());
        lm.put("prixUnitaireUsd", l.getPrixUnitaireUsd());
        lm.put("montantLigneUsd", l.getMontantLigneUsd());
        lignes.add(lm);
      }
      m.put("lignes", lignes);
    }
    return m;
  }

  private Map<String, Object> toOffreMap(OffreFournisseur o, boolean details) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", o.getId());
    m.put("fkInvitation", o.getFkInvitation());
    m.put("fkDemandeCotation", o.getFkDemandeCotation());
    m.put("fkFournisseur", o.getFkFournisseur());
    m.put("fournisseurNom", repo.findFournisseurNom(o.getFkFournisseur()));
    m.put("devise", o.getDevise());
    m.put("tauxDeclare", o.getTauxDeclare());
    m.put("validiteJusquau", o.getValiditeJusquau());
    m.put("fraisLivraison", o.getFraisLivraison());
    m.put("conditions", o.getConditions());
    m.put("statut", o.getStatut());
    m.put("versionNo", o.getVersionNo());
    m.put("dateSoumission", o.getDateSoumission());
    if (details) {
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
    }
    return m;
  }

  private Map<String, Object> toModifMap(DemandeModifFournisseur d) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", d.getId());
    m.put("fkFournisseur", d.getFkFournisseur());
    m.put("fournisseurNom", repo.findFournisseurNom(d.getFkFournisseur()));
    m.put("statut", d.getStatut());
    m.put("motif", d.getMotif());
    m.put("commentaireDecision", d.getCommentaireDecision());
    m.put("dateCreate", d.getDateCreate());
    List<Map<String, Object>> champs = repo.findChampsModif(d.getId()).stream().map(c -> {
      Map<String, Object> cm = new LinkedHashMap<>();
      cm.put("champ", c.getChamp());
      cm.put("valeurActuelle", c.getValeurActuelle());
      cm.put("valeurProposee", c.getValeurProposee());
      return cm;
    }).toList();
    m.put("champs", champs);
    return m;
  }

  // package visibility for portail / tests
  CommandesRepository repository() {
    return repo;
  }

  MoneyConversionService money() {
    return moneyConversionService;
  }
}
