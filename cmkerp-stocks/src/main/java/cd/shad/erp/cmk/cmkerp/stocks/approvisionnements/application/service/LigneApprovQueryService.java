package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.service;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response.LigneApprovResponse;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.mapper.LigneApprovMapper;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.model.LigneApprov;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.repository.LigneApprovRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Service pour la gestion des lignes d'approvisionnement (lecture uniquement).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class LigneApprovQueryService {

  private final LigneApprovRepository ligneApprovRepository;
  private final LigneApprovMapper ligneApprovMapper;
  private final JdbcTemplate jdbcTemplate;

  /**
   * Récupère toutes les lignes d'un approvisionnement.
   */
  public List<LigneApprovResponse> findByFkApprov(Long fkApprov) {
    List<LigneApprov> lignes = ligneApprovRepository.findByFkApprov(fkApprov);

    return lignes.stream().map(this::toEnrichedResponse).toList();
  }

  /**
   * Récupère une ligne par son ID.
   */
  public LigneApprovResponse findById(Long id) {
    LigneApprov ligne = ligneApprovRepository.findById(id)
        .orElseThrow(() -> NotFoundException.entity("LigneApprov", id));

    return toEnrichedResponse(ligne);
  }

  private LigneApprovResponse toEnrichedResponse(LigneApprov ligne) {
    StockProduitInfo info = getStockProduitInfo(ligne.getFkStock());
    return ligneApprovMapper.toResponse(
        ligne,
        info != null ? info.produitId() : null,
        info != null ? info.produitNom() : null,
        info != null ? info.stockActuel() : null);
  }

  private record StockProduitInfo(Long produitId, String produitNom, Float stockActuel) {}

  private StockProduitInfo getStockProduitInfo(Long fkStock) {
    if (fkStock == null) {
      return null;
    }
    String sql = """
        SELECT p.id AS produit_id, p.nomcommercial, sp.qte
        FROM stock_produits sp
        INNER JOIN produits p ON sp.fkProduits = p.id
        WHERE sp.id = ?
        """;
    try {
      return jdbcTemplate.query(sql, rs -> {
        if (!rs.next()) {
          return null;
        }
        Long produitId = rs.getObject("produit_id") != null ? rs.getLong("produit_id") : null;
        String nom = rs.getString("nomcommercial");
        Float qte = rs.getObject("qte") != null ? rs.getFloat("qte") : null;
        return new StockProduitInfo(produitId, nom, qte);
      }, fkStock);
    } catch (Exception e) {
      log.warn("Stock/produit non trouvé pour stock ID: {} - Erreur: {}", fkStock, e.getMessage());
      return null;
    }
  }
}
