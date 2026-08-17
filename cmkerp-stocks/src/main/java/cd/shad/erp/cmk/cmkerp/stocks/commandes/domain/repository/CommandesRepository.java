package cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.repository;

import cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Accès données du module Commandes fournisseurs.
 */
public interface CommandesRepository {

  // —— Demandes cotation ——
  Optional<DemandeCotation> findDemandeById(Long id);
  List<DemandeCotation> findDemandes(int offset, int limit, String statut, Long fkPharmacie, String search);
  long countDemandes(String statut, Long fkPharmacie, String search);
  int saveDemande(DemandeCotation d);
  int updateDemande(DemandeCotation d);
  List<LigneDemandeCotation> findLignesDemande(Long demandeId);
  void deleteLignesDemande(Long demandeId);
  int saveLigneDemande(LigneDemandeCotation l);

  // —— Invitations ——
  List<InvitationFournisseur> findInvitationsByDemande(Long demandeId);
  Optional<InvitationFournisseur> findInvitationById(Long id);
  Optional<InvitationFournisseur> findInvitationByPublicToken(String token);
  int saveInvitation(InvitationFournisseur inv);
  int updateInvitation(InvitationFournisseur inv);

  // —— Offres ——
  Optional<OffreFournisseur> findOffreById(Long id);
  Optional<OffreFournisseur> findOffreByInvitation(Long invitationId);
  List<OffreFournisseur> findOffres(int offset, int limit, Long fkDemande, String statut);
  long countOffres(Long fkDemande, String statut);
  int saveOffre(OffreFournisseur o);
  int updateOffre(OffreFournisseur o);
  List<LigneOffreFournisseur> findLignesOffre(Long offreId);
  void deleteLignesOffre(Long offreId);
  int saveLigneOffre(LigneOffreFournisseur l);
  int saveVersionOffre(Long offreId, int versionNo, String snapshotJson, Long userId);
  int savePieceJointe(Long offreId, String nom, String mime, Long taille, String storageKey);
  List<Map<String, Object>> findPiecesJointes(Long offreId);

  // —— Attributions / BC ——
  int saveAttribution(AttributionCotation a);
  int saveLigneAttribution(LigneAttribution l);
  Optional<BonCommande> findBonById(Long id);
  List<BonCommande> findBons(int offset, int limit, String statut, Long fkFournisseur, String search);
  long countBons(String statut, Long fkFournisseur, String search);
  int saveBon(BonCommande b);
  int updateBon(BonCommande b);
  List<LigneBonCommande> findLignesBon(Long bonId);
  int saveLigneBon(LigneBonCommande l);
  int updateLigneBon(LigneBonCommande l);

  // —— Réceptions ——
  Optional<ReceptionCommande> findReceptionById(Long id);
  List<ReceptionCommande> findReceptionsByBon(Long bonId);
  int saveReception(ReceptionCommande r);
  int updateReception(ReceptionCommande r);
  int saveLigneReception(LigneReceptionCommande l);
  List<LigneReceptionCommande> findLignesReception(Long receptionId);

  // —— Évaluations / params / modifs ——
  Optional<ParamScoreFournisseur> findParamScore();
  int updateParamScore(ParamScoreFournisseur p);
  int saveEvaluation(EvaluationFournisseur e);
  List<EvaluationFournisseur> findEvaluations(int offset, int limit, Long fkFournisseur);
  long countEvaluations(Long fkFournisseur);

  Optional<DemandeModifFournisseur> findModifById(Long id);
  List<DemandeModifFournisseur> findModifs(int offset, int limit, String statut);
  long countModifs(String statut);
  int saveModif(DemandeModifFournisseur d);
  int updateModif(DemandeModifFournisseur d);
  int saveChampModif(ChampModifFournisseur c);
  List<ChampModifFournisseur> findChampsModif(Long demandeId);

  int saveDemandeReouverture(Long offreId, String motif);
  Optional<Map<String, Object>> findDemandeReouverturePending(Long offreId);
  List<Map<String, Object>> listReouvertures(String statut, int limit);
  int updateDemandeReouverture(Long id, String statut, String nouvelleDateLimite, String commentaire, Long decideurId);

  // —— Mail / outbox helpers ——
  int insertMailLog(String idempotenceKey, String destinataire, String sujet, String corps, Long fkInvitation, Long fkDemande);
  List<Map<String, Object>> findPendingMails(int limit);
  List<String> listActiveMailingSendEmails();
  int markMailSent(Long id);
  int markMailFailed(Long id, String error);
  int insertOutbox(String eventType, String topic, String eventKey, String payload);

  // —— Dashboard counts ——
  Map<String, Long> dashboardCounts();

  // —— Lookups ——
  String findFournisseurNom(Long id);
  String findFournisseurEmail(Long id);
  String findPharmacieNom(Long id);
  String findProduitNom(Long id);
  Long findStockId(Long fkProduit, Long fkPharmacie);
}
