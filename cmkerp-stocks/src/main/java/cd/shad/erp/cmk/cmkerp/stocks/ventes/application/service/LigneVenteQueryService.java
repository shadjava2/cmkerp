package cd.shad.erp.cmk.cmkerp.stocks.ventes.application.service;

import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.response.LigneVenteResponse;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.mapper.LigneVenteMapper;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.model.LigneVente;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.repository.LigneVenteRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Query Service pour la gestion des lignes de vente (lecture uniquement).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class LigneVenteQueryService {

    private final LigneVenteRepository ligneVenteRepository;
    private final LigneVenteMapper ligneVenteMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Récupère toutes les lignes d'une vente.
     */
    public List<LigneVenteResponse> findByFkVente(Long fkVente) {
        List<LigneVente> lignes = ligneVenteRepository.findByFkVente(fkVente);

        return lignes.stream()
                .map(this::toEnrichedResponse)
                .toList();
    }

    /**
     * Récupère une ligne par son ID.
     */
    public LigneVenteResponse findById(Long id) {
        LigneVente ligne = ligneVenteRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("LigneVente", id));

        return toEnrichedResponse(ligne);
    }

    private LigneVenteResponse toEnrichedResponse(LigneVente ligne) {
        StockProduitInfo info = getStockProduitInfo(ligne.getFkStock());
        return ligneVenteMapper.toResponse(
                ligne,
                info != null ? info.produitNom() : null,
                info != null ? info.stockActuel() : null);
    }

    private record StockProduitInfo(String produitNom, Float stockActuel) {}

    private StockProduitInfo getStockProduitInfo(Long fkStock) {
        if (fkStock == null) {
            return null;
        }
        String sql = """
                SELECT p.nomcommercial, sp.qte
                FROM stock_produits sp
                INNER JOIN produits p ON sp.fkProduits = p.id
                WHERE sp.id = ?
                """;
        try {
            return jdbcTemplate.query(sql, rs -> {
                if (!rs.next()) {
                    return null;
                }
                String nom = rs.getString("nomcommercial");
                Float qte = rs.getObject("qte") != null ? rs.getFloat("qte") : null;
                return new StockProduitInfo(nom, qte);
            }, fkStock);
        } catch (Exception e) {
            log.warn("Stock/produit non trouvé pour stock ID: {} - Erreur: {}", fkStock, e.getMessage());
            return null;
        }
    }
}
