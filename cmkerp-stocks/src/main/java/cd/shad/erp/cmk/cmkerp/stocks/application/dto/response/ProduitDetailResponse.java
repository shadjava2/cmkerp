package cd.shad.erp.cmk.cmkerp.stocks.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de réponse détaillée pour un produit avec JOINs MySQL 8.
 *
 * <p>Ce DTO contient les désignations des tables de référence (formes, dosages,
 * conditionnements, categorie_produit) récupérées via JOINs pour éviter le problème N+1.
 *
 * <p>Exemple de requête SQL utilisée :
 * <pre>
 * SELECT p.id, p.codebarre, p.nomcommercial, p.nomscientifique,
 *        f.designation as forme, d.designation as dosage,
 *        c.designation as conditionnement, ct.designation as categorie,
 *        p.prixachat, p.prixachatcomptable, p.qtealert, p.qtcritique, p.perimable
 * FROM produits p
 * LEFT JOIN formes f ON p.fkForme = f.id
 * LEFT JOIN dosages d ON p.fkDosage = d.id
 * LEFT JOIN conditionnements c ON p.fkConditionnement = c.id
 * LEFT JOIN categorie_produit ct ON p.fkCategorie = ct.id
 * WHERE p.id = ?
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProduitDetailResponse {
    private Long id;
    private String codebarre;
    private String nomcommercial;
    private String nomscientifique;

    // IDs des relations
    private Long fkForme;
    private Long fkDosage;
    private Long fkConditionnement;
    private Long fkCategorie;

    // Désignations des relations (récupérées via JOINs)
    private String forme;           // f.designation
    private String dosage;          // d.designation
    private String conditionnement; // c.designation
    private String categorie;       // ct.designation

    private BigDecimal prixachat;
    private BigDecimal prixachatcomptable;
    private Float qtealert;
    private Float qtcritique;
    private Boolean perimable;
    private LocalDateTime dateCreate;
    private LocalDateTime dateUpdate;
}

