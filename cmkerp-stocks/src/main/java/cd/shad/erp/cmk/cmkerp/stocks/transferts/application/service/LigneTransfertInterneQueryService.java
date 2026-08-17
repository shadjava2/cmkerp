package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service;

import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.LigneTransfertInterneResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.mapper.LigneTransfertInterneMapper;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.LigneTransfertInterne;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.domain.repository.LigneTransfertInterneRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Query Service pour la gestion des lignes de transfert interne (lecture uniquement).
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class LigneTransfertInterneQueryService {

    private final LigneTransfertInterneRepository ligneTransfertInterneRepository;
    private final LigneTransfertInterneMapper ligneTransfertInterneMapper;
    private final JdbcTemplate jdbcTemplate;

    public LigneTransfertInterneQueryService(
            LigneTransfertInterneRepository ligneTransfertInterneRepository,
            LigneTransfertInterneMapper ligneTransfertInterneMapper,
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.ligneTransfertInterneRepository = ligneTransfertInterneRepository;
        this.ligneTransfertInterneMapper = ligneTransfertInterneMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Récupère toutes les lignes d'un transfert interne.
     */
    public List<LigneTransfertInterneResponse> findByFkTransfertInterne(Long fkTransfertInterne) {
        List<LigneTransfertInterne> lignes = ligneTransfertInterneRepository.findByFkTransfertInterne(fkTransfertInterne);

        return lignes.stream()
                .map(ligne -> {
                    String produitNom = getProduitNom(ligne.getFkStock());
                    return ligneTransfertInterneMapper.toResponse(ligne, produitNom);
                })
                .collect(Collectors.toList());
    }

    /**
     * Récupère une ligne par son ID.
     */
    public LigneTransfertInterneResponse findById(Long id) {
        LigneTransfertInterne ligne = ligneTransfertInterneRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("LigneTransfertInterne", id));

        String produitNom = getProduitNom(ligne.getFkStock());
        return ligneTransfertInterneMapper.toResponse(ligne, produitNom);
    }

    private String getProduitNom(Long fkStock) {
        if (fkStock == null) {
            return null;
        }
        // Récupérer le nom du produit depuis le stock
        String sql = "SELECT p.nomcommercial FROM stock_produits sp " +
                "INNER JOIN produits p ON sp.fkProduits = p.id " +
                "WHERE sp.id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, fkStock);
        } catch (Exception e) {
            log.warn("Produit non trouvé pour stock ID: {} - Erreur: {}", fkStock, e.getMessage());
            return null;
        }
    }
}

