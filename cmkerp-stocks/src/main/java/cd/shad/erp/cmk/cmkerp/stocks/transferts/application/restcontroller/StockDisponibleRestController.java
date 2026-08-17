package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.POS_STOCKS_DISPONIBLES_BASE;
import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCKS_DISPONIBLES_BASE;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.StockDisponibleResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service.StockDisponibleQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Contrôleur REST pour la gestion des stocks disponibles.
 */
@RestController
@RequestMapping({STOCKS_DISPONIBLES_BASE, POS_STOCKS_DISPONIBLES_BASE})
@RequiredArgsConstructor
@Tag(name = "Stocks - Stocks Disponibles", description = "Gestion des stocks disponibles pour remplacement")
@Validated
@Slf4j
public class StockDisponibleRestController {

    private final StockDisponibleQueryService stockDisponibleQueryService;

    /**
     * Récupère les stocks disponibles pour remplacer un produit.
     */
    @GetMapping
    @Operation(summary = "Récupère les stocks disponibles")
    public ResponseEntity<List<StockDisponibleResponse>> findAll(
            @RequestParam Long fkPharmacieStock,
            @RequestParam(required = false) String search) {
        log.debug("Récupération des stocks disponibles - fkPharmacieStock: {}, search: {}", fkPharmacieStock, search);

        List<StockDisponibleResponse> stocks = stockDisponibleQueryService.findAll(fkPharmacieStock, search);
        return ResponseEntity.ok(stocks);
    }
}

