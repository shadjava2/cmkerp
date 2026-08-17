package cd.shad.erp.cmk.cmkerp.stocks.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.POS_REFERENCES_BASE;
import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCKS_BASE;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.CategorieProduitResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.ConditionnementResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.DosageResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.FormeResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.ReferenceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Contrôleur REST pour la gestion des références (formes, dosages, conditionnements, catégories).
 * Utilise le ReferenceQueryService pour les opérations de lecture.
 */
@RestController
@RequestMapping({STOCKS_BASE + "/references", POS_REFERENCES_BASE})
@RequiredArgsConstructor
@Tag(name = "Stocks - Références", description = "Gestion des tables de référence (formes, dosages, conditionnements, catégories)")
@Validated
public class ReferenceRestController {

    private final ReferenceQueryService referenceQueryService;

    /**
     * Récupère toutes les formes triées par désignation.
     */
    @GetMapping("/formes")
    @Operation(summary = "Liste toutes les formes")
    public ResponseEntity<List<FormeResponse>> findAllFormes() {
        List<FormeResponse> formes = referenceQueryService.findAllFormes();
        return ResponseEntity.ok(formes);
    }

    /**
     * Récupère tous les dosages triés par désignation.
     */
    @GetMapping("/dosages")
    @Operation(summary = "Liste tous les dosages")
    public ResponseEntity<List<DosageResponse>> findAllDosages() {
        List<DosageResponse> dosages = referenceQueryService.findAllDosages();
        return ResponseEntity.ok(dosages);
    }

    /**
     * Récupère tous les conditionnements triés par désignation.
     */
    @GetMapping("/conditionnements")
    @Operation(summary = "Liste tous les conditionnements")
    public ResponseEntity<List<ConditionnementResponse>> findAllConditionnements() {
        List<ConditionnementResponse> conditionnements = referenceQueryService.findAllConditionnements();
        return ResponseEntity.ok(conditionnements);
    }

    /**
     * Récupère toutes les catégories de produits triées par désignation.
     * Si pharmacieId est fourni, retourne uniquement les catégories auxquelles la pharmacie a droit.
     */
    @GetMapping("/categories")
    @Operation(summary = "Liste les catégories de produits (filtrées par pharmacie si pharmacieId fourni)")
    public ResponseEntity<List<CategorieProduitResponse>> findAllCategorieProduits(
            @RequestParam(required = false) Long pharmacieId) {
        List<CategorieProduitResponse> categories;
        if (pharmacieId != null) {
            categories = referenceQueryService.findCategorieProduitsByPharmacie(pharmacieId);
        } else {
            categories = referenceQueryService.findAllCategorieProduits();
        }
        return ResponseEntity.ok(categories);
    }
}

