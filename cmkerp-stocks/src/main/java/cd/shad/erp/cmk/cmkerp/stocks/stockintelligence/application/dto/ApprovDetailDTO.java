package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record ApprovDetailDTO(
    long id,
    String reference,
    String statut,
    Long fournisseurId,
    String fournisseur,
    Long pharmacieId,
    String pharmacie,
    /** Date d'encodage (datecreate) — référence opérationnelle */
    String dateApprovisionnement,
    /** Date saisie sur le bon de livraison (peut être erronée ou future) */
    String dateBonLivraison,
    String numBonLiv,
    Integer taux,
    String encodeur,
    String encodeurUsername,
    String dateCreate,
    String dateUpdate,
    int lignesCount,
    int produitsDistinct,
    BigDecimal quantiteTotale,
    BigDecimal montantTotal,
    String produitPlusCher,
    String produitPlusApprovisionne,
    List<ApprovLineDetailDTO> lignes,
    ApprovQualityFlagsDTO qualite) {}
