package cd.shad.erp.cmk.cmkerp.pos.transferts.application.service;

import cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.response.LigneTransfertInterneResponse;
import cd.shad.erp.cmk.cmkerp.pos.transferts.application.mapper.LigneTransfertInterneMapper;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.LigneTransfertInterne;
import cd.shad.erp.cmk.cmkerp.pos.transferts.domain.repository.LigneTransfertInterneRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Query Service pour la gestion des lignes de transfert interne (lecture uniquement) - module POS.
 */
@Service("posLigneTransfertInterneQueryService")
@Transactional(readOnly = true)
@Slf4j
public class LigneTransfertInterneQueryService {

    private final LigneTransfertInterneRepository ligneTransfertInterneRepository;
    private final LigneTransfertInterneMapper ligneTransfertInterneMapper;
    private final TransfertInterneProduitLookup produitLookup;

    public LigneTransfertInterneQueryService(
            @Qualifier("posLigneTransfertInterneJdbcRepositoryImpl") LigneTransfertInterneRepository ligneTransfertInterneRepository,
            @Qualifier("posLigneTransfertInterneMapper") LigneTransfertInterneMapper ligneTransfertInterneMapper,
            TransfertInterneProduitLookup produitLookup) {
        this.ligneTransfertInterneRepository = ligneTransfertInterneRepository;
        this.ligneTransfertInterneMapper = ligneTransfertInterneMapper;
        this.produitLookup = produitLookup;
    }

    /**
     * Récupère toutes les lignes d'un transfert interne.
     */
    public List<LigneTransfertInterneResponse> findByFkTransfertInterne(Long fkTransfertInterne) {
        List<LigneTransfertInterne> lignes = ligneTransfertInterneRepository.findByFkTransfertInterne(fkTransfertInterne);
        Map<Long, String> nomsByStock = produitLookup.resolveNomsByStockIds(
                lignes.stream().map(LigneTransfertInterne::getFkStock).collect(Collectors.toList()));

        return lignes.stream()
                .map(ligne -> ligneTransfertInterneMapper.toResponse(
                        ligne, nomsByStock.get(ligne.getFkStock())))
                .collect(Collectors.toList());
    }

    /**
     * Récupère une ligne par son ID.
     */
    public LigneTransfertInterneResponse findById(Long id) {
        LigneTransfertInterne ligne = ligneTransfertInterneRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("LigneTransfertInterne", id));

        String produitNom = produitLookup.resolveNomsByStockIds(List.of(ligne.getFkStock()))
                .get(ligne.getFkStock());
        return ligneTransfertInterneMapper.toResponse(ligne, produitNom);
    }
}

