package cd.shad.erp.cmk.cmkerp.stocks.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.POS_BASE;
import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCKS_BASE;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.CursorPageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.request.ProduitRequest;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.request.ToggleOperationnelRequest;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.request.UpdateProduitSeuilsRequest;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.request.UpdateStockQteRequest;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.ProduitDetailResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.ProduitResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.ProduitWithStockResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.ProduitCommandService;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.ProduitQueryService;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.StockProduitCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Contrôleur REST pour la gestion des produits. Utilise les Query/Command Services de la nouvelle
 * architecture DDD.
 */
@RestController
@RequestMapping({STOCKS_BASE + "/products", POS_BASE + "/products"})
@RequiredArgsConstructor
@Tag(name = "Stocks - Produits", description = "Gestion des produits pharmaceutiques (alias POS)")
@Validated
public class ProduitRestController {

  private final ProduitQueryService produitQueryService;
  private final ProduitCommandService produitCommandService;
  private final StockProduitCommandService stockProduitCommandService;
  private final JwtTokenProvider jwtTokenProvider;

  /**
   * Récupère une page de produits avec pagination.
   *
   * <p>
   * Requiert une authentification valide (user connecté).
   */
  @GetMapping
  @Operation(summary = "Liste paginée des produits")
  public ResponseEntity<Page<ProduitResponse>> findAll(Pageable pageable,
      @RequestParam(required = false) String nomcommercial) {
    Page<ProduitResponse> produits;
    if (nomcommercial != null && !nomcommercial.trim().isEmpty()) {
      produits = produitQueryService.findByNomcommercial(nomcommercial.trim(), pageable);
    } else {
      produits = produitQueryService.findAll(pageable);
    }
    return ResponseEntity.ok(produits);
  }

  /**
   * Récupère une page de produits avec stock et péremption. Utilise la
   * pagination cursor-based (Facebook-grade) pour une meilleure performance et stabilité.
   *
   * <p>
   * Requiert une authentification valide (user connecté).
   *
   * <p>
   * Cette méthode utilise une requête SQL optimisée avec JOINs pour récupérer : - Les produits
   * - Le stock en cours (stock_produits)
   * - Les dates de péremption (perimable_alerte_stock) - Les désignations des références (formes,
   * dosages, conditionnements, categories)
   *
   * <p>
   * <strong>Pagination cursor-based :</strong>
   * <ul>
   * <li>Première page : {@code cursor = null}</li>
   * <li>Page suivante : {@code cursor = nextCursor} de la page précédente</li>
   * <li>Clé de tri : {@code id DESC} (simple, performant, toujours unique)</li>
   * </ul>
   *
   * @param pharmacieId ID de la pharmacie (requis)
   * @param cursor Cursor pour la pagination (ID du dernier élément de la page précédente, null pour
   *        première page)
   * @param limit Nombre d'éléments à retourner (défaut: 20, max: 100)
   * @param nomcommercial Recherche optionnelle par nom commercial OU nom scientifique OU
   *        code-barres (recherche partielle insensible à la casse)
   * @param operationnel Filtre optionnel sur le statut opérationnel (true = actif, false = inactif,
   *        null = tous)
   * @return CursorPageResponse avec items, nextCursor et hasMore
   */
  @GetMapping("/with-stock")
  @Operation(
      summary = "Liste paginée des produits avec stock et péremption (cursor-based)")
  public ResponseEntity<CursorPageResponse<ProduitWithStockResponse>> findProductsWithStock(
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false, defaultValue = "20") Integer limit,
      @RequestParam(required = false) String nomcommercial,
      @RequestParam(required = false) Boolean operationnel,
      // Filtres avancés
      @RequestParam(required = false) Boolean perime,
      @RequestParam(required = false) Integer perimeDansXJours,
      @RequestParam(required = false) String stockOperator,
      @RequestParam(required = false) Double stockValue,
      @RequestParam(required = false) String prixOperator,
      @RequestParam(required = false) Double prixValue,
      @RequestParam(required = false) Boolean perimable) {

    // Validation: pharmacieId est requis
    if (pharmacieId == null) {
      throw new IllegalArgumentException("pharmacieId est requis");
    }

    CursorPageResponse<ProduitWithStockResponse> produits =
        produitQueryService.findProductsWithStockCursor(pharmacieId, nomcommercial, operationnel,
            cursor, limit, perime, perimeDansXJours, stockOperator, stockValue, prixOperator,
            prixValue, perimable);

    return ResponseEntity.ok(produits);
  }

  /**
   * Récupère une page de produits avec stock et péremption. Utilise la
   * pagination page/size classique pour une navigation intuitive.
   *
   * <p>
   * Requiert une authentification valide (user connecté).
   *
   * <p>
   * Cette méthode utilise une requête SQL optimisée avec JOINs pour récupérer : - Les produits
   * - Le stock en cours (stock_produits)
   * - Les dates de péremption (perimable_alerte_stock) - Les désignations des références (formes,
   * dosages, conditionnements, categories)
   *
   * <p>
   * <strong>Pagination page/size :</strong>
   * <ul>
   * <li>Première page : {@code page = 0}</li>
   * <li>Page suivante : {@code page = 1, 2, ...}</li>
   * <li>Clé de tri : {@code nomcommercial ASC} (par défaut)</li>
   * </ul>
   *
   * @param pharmacieId ID de la pharmacie (requis)
   * @param page Numéro de page (0-based, défaut: 0)
   * @param size Nombre d'éléments par page (défaut: 20, max: 100)
   * @param nomcommercial Recherche optionnelle par nom commercial OU nom scientifique OU
   *        code-barres (recherche partielle insensible à la casse)
   * @param operationnel Filtre optionnel sur le statut opérationnel (true = actif, false = inactif,
   *        null = tous)
   * @param perime Filtre sur les produits périmés (true = périmés, false = non périmés, null =
   *        tous)
   * @param perimeDansXJours Filtre sur les produits qui vont périmer dans X jours (nombre de jours)
   * @param stockOperator Opérateur de comparaison pour le stock ("gt", "lt", "eq")
   * @param stockValue Valeur de comparaison pour le stock
   * @param prixOperator Opérateur de comparaison pour le prix d'achat ("gt", "lt", "eq")
   * @param prixValue Valeur de comparaison pour le prix d'achat
   * @param perimable Filtre sur les produits périssables (true = périssable, false = non
   *        périssable, null = tous)
   * @return PageResponse avec content, page, size, totalElements, totalPages, hasNext, hasPrevious
   */
  @GetMapping("/with-stock/page")
  @Operation(
      summary = "Liste paginée des produits avec stock et péremption (page/size)")
  public ResponseEntity<PageResponse<ProduitWithStockResponse>> findProductsWithStockPage(
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false, defaultValue = "0") Integer page,
      @RequestParam(required = false, defaultValue = "20") Integer size,
      @RequestParam(required = false) String nomcommercial,
      @RequestParam(required = false) Boolean operationnel,
      // Filtres avancés
      @RequestParam(required = false) Boolean perime,
      @RequestParam(required = false) Integer perimeDansXJours,
      @RequestParam(required = false) String stockOperator,
      @RequestParam(required = false) Double stockValue,
      @RequestParam(required = false) String prixOperator,
      @RequestParam(required = false) Double prixValue,
      @RequestParam(required = false) Boolean perimable) {

    // Validation: pharmacieId est requis
    if (pharmacieId == null) {
      throw new IllegalArgumentException("pharmacieId est requis");
    }

    // Validation et limitation de size
    int effectiveSize = Math.min(Math.max(size, 1), 100); // Entre 1 et 100
    int effectivePage = Math.max(page, 0); // Au moins 0

    // Créer Pageable avec tri par défaut
    Pageable pageable =
        PageRequest.of(effectivePage, effectiveSize, Sort.by("nomcommercial").ascending());

    PageResponse<ProduitWithStockResponse> produits = produitQueryService.findProductsWithStockPage(
        pharmacieId, nomcommercial, operationnel, pageable, perime, perimeDansXJours, stockOperator,
        stockValue, prixOperator, prixValue, perimable);

    return ResponseEntity.ok(produits);
  }

  /**
   * Récupère un produit par son ID (sans JOINs, pour liste).
   *
   * <p>
   * Requiert une authentification valide (user connecté).
   */
  @GetMapping("/{id}")
  @Operation(summary = "Récupère un produit par son ID")
  public ResponseEntity<ProduitResponse> findById(@PathVariable Long id) {
    ProduitResponse produit = produitQueryService.findById(id);
    return ResponseEntity.ok(produit);
  }

  /**
   * Récupère un produit par son ID avec toutes les relations (JOINs MySQL 8).
   *
   * <p>
   * Requiert une authentification valide (user connecté).
   */
  @GetMapping("/{id}/detail")
  @Operation(summary = "Récupère un produit par son ID avec toutes les relations")
  public ResponseEntity<ProduitDetailResponse> findByIdWithRelations(@PathVariable Long id) {
    ProduitDetailResponse produit = produitQueryService.findByIdWithRelations(id);
    return ResponseEntity.ok(produit);
  }

  /**
   * Crée un nouveau produit.
   *
   * <p>
   * Requiert une authentification valide (user connecté).
   */
  @PostMapping
  @Operation(summary = "Crée un nouveau produit")
  public ResponseEntity<ProduitResponse> create(@Valid @RequestBody ProduitRequest request,
      HttpServletRequest httpRequest) {
    Long currentUserId = getCurrentUserId(httpRequest);
    ProduitResponse created = produitCommandService.create(request, currentUserId);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  /**
   * Met à jour un produit existant.
   *
   * <p>
   * Requiert une authentification valide (user connecté).
   */
  @PutMapping("/{id}")
  @Operation(summary = "Met à jour un produit")
  public ResponseEntity<ProduitResponse> update(@PathVariable Long id,
      @Valid @RequestBody ProduitRequest request, HttpServletRequest httpRequest) {
    Long currentUserId = getCurrentUserId(httpRequest);
    ProduitResponse updated = produitCommandService.update(id, request, currentUserId);
    return ResponseEntity.ok(updated);
  }

  /**
   * Supprime un produit.
   *
   * <p>
   * Requiert une authentification valide (user connecté).
   */
  @DeleteMapping("/{id}")
  @Operation(summary = "Supprime un produit")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    produitCommandService.delete(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Active ou désactive un produit pour une pharmacie (stock_produits.operationnel).
   */
  @PatchMapping("/stock/{stockId}/operationnel")
  @Operation(summary = "Active ou désactive un produit au niveau du stock")
  public ResponseEntity<Void> setOperationnel(@PathVariable Long stockId,
      @Valid @RequestBody ToggleOperationnelRequest request, HttpServletRequest httpRequest) {
    Long currentUserId = getCurrentUserId(httpRequest);
    stockProduitCommandService.setOperationnel(stockId, request.getOperationnel(), currentUserId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Ajuste le stock actuel (stock_produits.qte) pour une pharmacie.
   */
  @PatchMapping("/stock/{stockId}/qte")
  @Operation(summary = "Met à jour la quantité en stock (stock actuel)")
  public ResponseEntity<Void> setStockQte(@PathVariable Long stockId,
      @Valid @RequestBody UpdateStockQteRequest request, HttpServletRequest httpRequest) {
    Long currentUserId = getCurrentUserId(httpRequest);
    stockProduitCommandService.setQte(stockId, request.getQte(), currentUserId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Met à jour les seuils (qtealert / qtcritique) — labels UI : Stock alerte / Stock critique.
   */
  @PatchMapping("/{id}/seuils")
  @Operation(summary = "Met à jour stock alerte et stock critique")
  public ResponseEntity<Void> updateSeuils(@PathVariable Long id,
      @Valid @RequestBody UpdateProduitSeuilsRequest request, HttpServletRequest httpRequest) {
    Long currentUserId = getCurrentUserId(httpRequest);
    produitCommandService.updateSeuils(id, request.getQtealert(), request.getQtcritique(), currentUserId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Extrait l'ID de l'utilisateur connecté depuis le JWT token dans l'en-tête Authorization.
   *
   * @param request la requête HTTP
   * @return l'ID de l'utilisateur connecté
   * @throws IllegalStateException si l'utilisateur n'est pas authentifié ou si le token est
   *         invalide
   */
  private Long getCurrentUserId(HttpServletRequest request) {
        return AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    }
}





