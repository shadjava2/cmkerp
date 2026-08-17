package cd.shad.erp.cmk.cmkerp.platform.approvisionnements.application.service;

import cd.shad.erp.cmk.cmkerp.platform.approvisionnements.application.dto.response.EchangeDeviseResponse;
import cd.shad.erp.cmk.cmkerp.platform.approvisionnements.domain.model.EchangeDevise;
import cd.shad.erp.cmk.cmkerp.platform.approvisionnements.domain.repository.EchangeDeviseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Query Service pour la gestion des échanges de devise (lecture uniquement).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EchangeDeviseQueryService {

    private final EchangeDeviseRepository echangeDeviseRepository;

    /**
     * Récupère tous les échanges de devise (pour combo).
     */
    public List<EchangeDeviseResponse> findAll() {
        List<EchangeDevise> echangeDevises = echangeDeviseRepository.findAll();
        return echangeDevises.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private EchangeDeviseResponse toResponse(EchangeDevise echangeDevise) {
        return EchangeDeviseResponse.builder()
                .id(echangeDevise.getId())
                .monnaieprincipale(echangeDevise.getMonnaieprincipale())
                .tauxechange(echangeDevise.getTauxechange())
                .monnaieechange(echangeDevise.getMonnaieechange())
                .symbole(echangeDevise.getSymbole())
                .dateCreate(echangeDevise.getDateCreate())
                .dateUpdate(echangeDevise.getDateUpdate())
                .build();
    }
}



